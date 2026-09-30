package com.walletapp.qrcode.controller;

import com.walletapp.common.dto.ApiResponse;
import com.walletapp.qrcode.dto.CreateQrRequest;
import com.walletapp.qrcode.dto.QrCodeResponse;
import com.walletapp.qrcode.dto.QrPaymentRequest;
import com.walletapp.qrcode.dto.QrPaymentResponse;
import com.walletapp.qrcode.service.QrCodeGeneratorService;
import com.walletapp.qrcode.service.QrCodePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/qr-codes")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final QrCodePaymentService qrCodePaymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<QrCodeResponse>> createQrCode(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateQrRequest request
    ) {
        QrCodeResponse response = qrCodeGeneratorService.generateQrCode(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<QrPaymentResponse>> payQrCode(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody QrPaymentRequest request
    ) {
        QrPaymentResponse response = qrCodePaymentService.processPayment(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
