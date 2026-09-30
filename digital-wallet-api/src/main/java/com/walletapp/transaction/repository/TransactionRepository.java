package com.walletapp.transaction.repository;

import com.walletapp.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findBySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(
        UUID sourceWalletId, UUID destWalletId, Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
        "WHERE t.sourceWalletId = :sourceWalletId " +
        "AND t.status = 'SUCCESS' " +
        "AND t.createdAt >= :startOfDay"
    )
    java.math.BigDecimal sumDailyOutgoingAmount(
        @org.springframework.data.repository.query.Param("sourceWalletId") UUID sourceWalletId,
        @org.springframework.data.repository.query.Param("startOfDay") java.time.OffsetDateTime startOfDay
    );
}
