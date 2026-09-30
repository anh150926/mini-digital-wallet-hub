package com.walletapp.transaction.service;

import com.walletapp.common.enums.EntryType;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.TransactionStatus;
import com.walletapp.common.enums.TransactionType;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.transaction.dto.TopupResponse;
import com.walletapp.transaction.dto.TransferResponse;
import com.walletapp.transaction.entity.LedgerEntry;
import com.walletapp.transaction.entity.Transaction;
import com.walletapp.transaction.repository.LedgerEntryRepository;
import com.walletapp.transaction.repository.TransactionRepository;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferExecutor {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse executeTransferInTransaction(
            UUID sourceWalletId,
            UUID destWalletId,
            UUID firstLockId,
            UUID secondLockId,
            BigDecimal amount,
            BigDecimal fee,
            String description,
            String idempotencyKey
    ) {
        log.info("Executing transfer in DB transaction: source={}, dest={}, amount={}, fee={}",
                sourceWalletId, destWalletId, amount, fee);

        // 1. Acquire DB Pessimistic Write Locks in sorted UUID order (BR-GEN-03)
        Wallet firstWallet = walletRepository.findByIdWithPessimisticLock(firstLockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        Wallet secondWallet = walletRepository.findByIdWithPessimisticLock(secondLockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        Wallet sourceWallet = sourceWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;
        Wallet destWallet = destWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;

        // 2. Non-Negative Balance Guarantee (BR-GEN-01)
        BigDecimal totalDebit = amount.add(fee);
        if (sourceWallet.getBalance().compareTo(totalDebit) < 0) {
            log.warn("Transfer failed: Insufficient funds. Balance: {}, Required: {}",
                    sourceWallet.getBalance(), totalDebit);
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_FUNDS,
                    "Số dư ví không đủ để thực hiện giao dịch",
                    Map.of("current_balance", sourceWallet.getBalance())
            );
        }

        // 3. Update Balances
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(totalDebit));
        destWallet.setBalance(destWallet.getBalance().add(amount));

        walletRepository.save(sourceWallet);
        walletRepository.save(destWallet);

        // 4. Record Transaction
        Transaction tx = Transaction.builder()
                .sourceWalletId(sourceWallet.getId())
                .destWalletId(destWallet.getId())
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .fee(fee)
                .idempotencyKey(idempotencyKey)
                .description(description)
                .build();
        tx = transactionRepository.save(tx);

        // 5. Double-Entry Accounting Ledger (BR-GEN-05: Debit = Credit)
        List<LedgerEntry> entries = new ArrayList<>();

        // DEBIT from sender: principal
        entries.add(LedgerEntry.builder()
                .transactionId(tx.getId())
                .walletId(sourceWallet.getId())
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .balanceAfter(sourceWallet.getBalance().add(fee))
                .build());

        // DEBIT from sender: fee (if any)
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(LedgerEntry.builder()
                    .transactionId(tx.getId())
                    .walletId(sourceWallet.getId())
                    .entryType(EntryType.DEBIT)
                    .amount(fee)
                    .balanceAfter(sourceWallet.getBalance())
                    .build());
        }

        // CREDIT to receiver
        entries.add(LedgerEntry.builder()
                .transactionId(tx.getId())
                .walletId(destWallet.getId())
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(destWallet.getBalance())
                .build());

        ledgerEntryRepository.saveAll(entries);

        log.info("Transfer completed successfully. txId={}, sourceBalanceAfter={}",
                tx.getId(), sourceWallet.getBalance());

        return TransferResponse.builder()
                .transactionId(tx.getId())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .sourceBalanceAfter(sourceWallet.getBalance())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TopupResponse executeTopupInTransaction(
            UUID walletId,
            BigDecimal amount,
            String idempotencyKey,
            String paymentMethod
    ) {
        log.info("Executing topup in DB transaction: wallet={}, amount={}", walletId, amount);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .destWalletId(wallet.getId())
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .idempotencyKey(idempotencyKey)
                .description("Nạp tiền vào ví từ " + (paymentMethod != null ? paymentMethod : "Ngân hàng"))
                .build();
        tx = transactionRepository.save(tx);

        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(tx.getId())
                .walletId(wallet.getId())
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .build();
        ledgerEntryRepository.save(entry);

        log.info("Topup completed successfully. txId={}, newBalance={}", tx.getId(), wallet.getBalance());

        return TopupResponse.builder()
                .transactionId(tx.getId())
                .status(tx.getStatus().name())
                .newBalance(wallet.getBalance())
                .build();
    }
}
