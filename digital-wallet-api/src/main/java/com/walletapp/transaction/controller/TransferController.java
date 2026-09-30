package com.walletapp.transaction.controller;

import com.walletapp.common.dto.ApiResponse;
import com.walletapp.transaction.dto.TopupRequest;
import com.walletapp.transaction.dto.TopupResponse;
import com.walletapp.transaction.dto.TransferRequest;
import com.walletapp.transaction.dto.TransferResponse;
import com.walletapp.transaction.service.TransferOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransferController {

    private final TransferOrchestrator transferOrchestrator;

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferResponse response = transferOrchestrator.executeTransfer(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<TopupResponse>> topup(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TopupRequest request
    ) {
        TopupResponse response = transferOrchestrator.executeTopup(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
