package com.walletapp.common.filter;

import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.util.CryptoUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isFinancialPost = "POST".equalsIgnoreCase(request.getMethod()) &&
                (path.startsWith("/api/v1/transfers") || path.startsWith("/api/v1/topup") || path.startsWith("/api/v1/qr-codes/pay"));

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (isFinancialPost && !StringUtils.hasText(idempotencyKey)) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED.getCode(),
                    "Header Idempotency-Key là bắt buộc đối với giao dịch tài chính");
            return;
        }

        if (!StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        if (auth != null && auth.getPrincipal() instanceof UUID principalId) {
            userId = principalId;
        }

        String redisKeyPrefix = "idemp:" + (userId != null ? userId : "guest") + ":" + idempotencyKey;

        CachedBodyHttpServletRequest requestWrapper = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        byte[] requestBodyBytes = requestWrapper.getCachedBody();
        String requestBodyStr = new String(requestBodyBytes, StandardCharsets.UTF_8);
        String currentHash = CryptoUtils.sha256Hex(requestBodyStr);

        Map<Object, Object> cachedData = redisTemplate.opsForHash().entries(redisKeyPrefix);

        if (!cachedData.isEmpty()) {
            String status = (String) cachedData.get("status");
            String storedHash = (String) cachedData.get("request_hash");

            if (PROCESSING.equals(status)) {
                log.warn("Duplicate concurrent request detected for key: {}", idempotencyKey);
                writeErrorResponse(response, HttpServletResponse.SC_CONFLICT,
                        ErrorCode.DUPLICATE_REQUEST.getCode(),
                        ErrorCode.DUPLICATE_REQUEST.getDefaultMessage());
                return;
            }

            if (COMPLETED.equals(status)) {
                if (!currentHash.equals(storedHash)) {
                    log.warn("Idempotency tampering detected for key: {}", idempotencyKey);
                    writeErrorResponse(response, 422,
                            ErrorCode.IDEMPOTENCY_PAYLOAD_MISMATCH.getCode(),
                            ErrorCode.IDEMPOTENCY_PAYLOAD_MISMATCH.getDefaultMessage());
                    return;
                }

                // Return cached response
                int cachedStatus = Integer.parseInt((String) cachedData.getOrDefault("response_status", "200"));
                String cachedBody = (String) cachedData.get("response_body");

                response.setStatus(cachedStatus);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(cachedBody != null ? cachedBody : "");
                return;
            }
        }

        // Mark as PROCESSING in Redis with 120s TTL
        redisTemplate.opsForHash().put(redisKeyPrefix, "status", PROCESSING);
        redisTemplate.opsForHash().put(redisKeyPrefix, "request_hash", currentHash);
        redisTemplate.expire(redisKeyPrefix, Duration.ofSeconds(120));

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);

            int status = responseWrapper.getStatus();
            byte[] responseBytes = responseWrapper.getContentAsByteArray();
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

            if (status >= 200 && status < 300) {
                // Save COMPLETED with 24 hours TTL
                redisTemplate.opsForHash().put(redisKeyPrefix, "status", COMPLETED);
                redisTemplate.opsForHash().put(redisKeyPrefix, "request_hash", currentHash);
                redisTemplate.opsForHash().put(redisKeyPrefix, "response_status", String.valueOf(status));
                redisTemplate.opsForHash().put(redisKeyPrefix, "response_body", responseBody);
                redisTemplate.expire(redisKeyPrefix, Duration.ofHours(24));
            } else {
                // Clean up key if error so client can retry
                redisTemplate.delete(redisKeyPrefix);
            }

            responseWrapper.copyBodyToResponse();
        } catch (Exception ex) {
            redisTemplate.delete(redisKeyPrefix);
            throw ex;
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int httpStatus, String code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String json = String.format(
                "{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"},\"timestamp\":\"%s\"}",
                code, message, Instant.now().toString()
        );
        response.getWriter().write(json);
    }

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {}

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }
    }
}
