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

    long countByCreatedAtBetweenAndStatus(
        java.time.OffsetDateTime start,
        java.time.OffsetDateTime end,
        com.walletapp.common.enums.TransactionStatus status
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
        "WHERE t.createdAt >= :start AND t.createdAt < :end AND t.status = 'SUCCESS'"
    )
    java.math.BigDecimal sumVolumeBetween(
        @org.springframework.data.repository.query.Param("start") java.time.OffsetDateTime start,
        @org.springframework.data.repository.query.Param("end") java.time.OffsetDateTime end
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT t FROM Transaction t WHERE " +
        "(:type IS NULL OR t.type = :type) AND " +
        "(:status IS NULL OR t.status = :status) AND " +
        "(:from IS NULL OR t.createdAt >= :from) AND " +
        "(:to IS NULL OR t.createdAt <= :to) " +
        "ORDER BY t.createdAt DESC"
    )
    Page<Transaction> findAllWithFilters(
        @org.springframework.data.repository.query.Param("type") com.walletapp.common.enums.TransactionType type,
        @org.springframework.data.repository.query.Param("status") com.walletapp.common.enums.TransactionStatus status,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to,
        Pageable pageable
    );

    java.util.List<Transaction> findTop10BySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(
        UUID sourceWalletId, UUID destWalletId
    );
}
