package com.walletapp.transaction.repository;

import com.walletapp.transaction.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
        "WHERE le.entryType = :entryType " +
        "AND le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    java.math.BigDecimal sumByEntryTypeAndDateRange(
        @org.springframework.data.repository.query.Param("entryType") com.walletapp.common.enums.EntryType entryType,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(le) FROM LedgerEntry le " +
        "WHERE le.entryType = :entryType " +
        "AND le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    long countByEntryTypeAndDateRange(
        @org.springframework.data.repository.query.Param("entryType") com.walletapp.common.enums.EntryType entryType,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(DISTINCT le.transactionId) FROM LedgerEntry le " +
        "WHERE le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    long countDistinctTransactionsByDateRange(
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le, Transaction t " +
        "WHERE le.transactionId = t.id " +
        "AND le.entryType = :entryType " +
        "AND t.type = :type " +
        "AND le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    java.math.BigDecimal sumByEntryTypeAndTypeAndDateRange(
        @org.springframework.data.repository.query.Param("entryType") com.walletapp.common.enums.EntryType entryType,
        @org.springframework.data.repository.query.Param("type") com.walletapp.common.enums.TransactionType type,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(le) FROM LedgerEntry le, Transaction t " +
        "WHERE le.transactionId = t.id " +
        "AND le.entryType = :entryType " +
        "AND t.type = :type " +
        "AND le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    long countByEntryTypeAndTypeAndDateRange(
        @org.springframework.data.repository.query.Param("entryType") com.walletapp.common.enums.EntryType entryType,
        @org.springframework.data.repository.query.Param("type") com.walletapp.common.enums.TransactionType type,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(DISTINCT le.transactionId) FROM LedgerEntry le, Transaction t " +
        "WHERE le.transactionId = t.id " +
        "AND t.type = :type " +
        "AND le.createdAt >= :from " +
        "AND le.createdAt <= :to"
    )
    long countDistinctTransactionsByTypeAndDateRange(
        @org.springframework.data.repository.query.Param("type") com.walletapp.common.enums.TransactionType type,
        @org.springframework.data.repository.query.Param("from") java.time.OffsetDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.OffsetDateTime to
    );
}
