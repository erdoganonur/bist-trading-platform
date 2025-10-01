package com.bisttrading.repository;

import com.bisttrading.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Simplified User Repository for REAL Monolith
 * Only essential user operations
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email OR u.username = :username")
    Optional<UserEntity> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByTcKimlikNo(String tcKimlikNo);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, String userId);
}