package com.walletapp.wallet.service;

import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.dto.PageResponse;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.entity.User;
import com.walletapp.transaction.entity.Transaction;
import com.walletapp.transaction.repository.TransactionRepository;
import com.walletapp.wallet.dto.TransactionItemResponse;
import com.walletapp.wallet.dto.WalletResponse;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletQueryService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        return WalletResponse.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionItemResponse> getTransactionHistory(UUID userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        Page<Transaction> pageResult = transactionRepository
                .findBySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(wallet.getId(), wallet.getId(), pageable);

        // Collect counterpart wallet IDs to fetch usernames in batch
        Set<UUID> counterWalletIds = new HashSet<>();
        for (Transaction tx : pageResult.getContent()) {
            if (wallet.getId().equals(tx.getSourceWalletId()) && tx.getDestWalletId() != null) {
                counterWalletIds.add(tx.getDestWalletId());
            } else if (wallet.getId().equals(tx.getDestWalletId()) && tx.getSourceWalletId() != null) {
                counterWalletIds.add(tx.getSourceWalletId());
            }
        }

        Map<UUID, User> walletIdToUserMap = new HashMap<>();
        if (!counterWalletIds.isEmpty()) {
            List<Wallet> counterWallets = walletRepository.findAllById(counterWalletIds);
            Set<UUID> counterUserIds = new HashSet<>();
            Map<UUID, UUID> walletToUserMapping = new HashMap<>();
            for (Wallet w : counterWallets) {
                counterUserIds.add(w.getUserId());
                walletToUserMapping.put(w.getId(), w.getUserId());
            }

            List<User> counterUsers = userRepository.findAllById(counterUserIds);
            Map<UUID, User> userMap = new HashMap<>();
            for (User u : counterUsers) {
                userMap.put(u.getId(), u);
            }

            for (Map.Entry<UUID, UUID> entry : walletToUserMapping.entrySet()) {
                User u = userMap.get(entry.getValue());
                if (u != null) {
                    walletIdToUserMap.put(entry.getKey(), u);
                }
            }
        }

        List<TransactionItemResponse> items = pageResult.getContent().stream().map(tx -> {
            boolean isOutgoing = wallet.getId().equals(tx.getSourceWalletId());
            String direction = isOutgoing ? "OUTGOING" : "INCOMING";
            UUID counterWalletId = isOutgoing ? tx.getDestWalletId() : tx.getSourceWalletId();

            String counterName = null;
            String counterPhone = null;
            if (counterWalletId != null && walletIdToUserMap.containsKey(counterWalletId)) {
                User counterUser = walletIdToUserMap.get(counterWalletId);
                counterName = counterUser.getFullName();
                counterPhone = counterUser.getPhoneNumber();
            }

            return TransactionItemResponse.builder()
                    .id(tx.getId())
                    .type(tx.getType().name())
                    .status(tx.getStatus().name())
                    .amount(tx.getAmount())
                    .fee(tx.getFee())
                    .direction(direction)
                    .counterpartyName(counterName)
                    .counterpartyPhone(counterPhone)
                    .description(tx.getDescription())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).toList();

        return PageResponse.<TransactionItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }
}
