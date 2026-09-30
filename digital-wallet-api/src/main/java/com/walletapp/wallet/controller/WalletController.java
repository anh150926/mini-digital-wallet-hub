package com.walletapp.wallet.controller;

import com.walletapp.common.dto.ApiResponse;
import com.walletapp.common.dto.PageResponse;
import com.walletapp.wallet.dto.TransactionItemResponse;
import com.walletapp.wallet.dto.WalletResponse;
import com.walletapp.wallet.service.WalletQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletQueryService walletQueryService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UUID userId
    ) {
        WalletResponse response = walletQueryService.getWalletByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<PageResponse<TransactionItemResponse>>> getMyTransactions(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<TransactionItemResponse> response = walletQueryService.getTransactionHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
