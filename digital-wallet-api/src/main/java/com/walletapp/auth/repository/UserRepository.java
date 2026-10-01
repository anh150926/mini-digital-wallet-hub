package com.walletapp.auth.repository;

import com.walletapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
            "(:query IS NULL OR :query = '' OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<User> searchUsers(
            @org.springframework.data.repository.query.Param("query") String query,
            org.springframework.data.domain.Pageable pageable
    );

    long countByRole(com.walletapp.common.enums.Role role);
    long countByStatus(com.walletapp.common.enums.UserStatus status);
}
