package com.bisttrading.repository.trading;

import com.bisttrading.entity.trading.BrokerAccount;
import com.bisttrading.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BrokerAccount entity operations
 * Simplified version with only basic JpaRepository operations
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface BrokerAccountRepository extends BaseRepository<BrokerAccount, String> {

    // Basic broker account lookups using Spring Data JPA method naming conventions

    /**
     * Find all broker accounts for user
     */
    List<BrokerAccount> findByUserIdAndDeletedAtIsNull(String userId);

    /**
     * Find broker account by user, broker name, and account number
     */
    Optional<BrokerAccount> findByUserIdAndBrokerNameAndAccountNumberAndDeletedAtIsNull(String userId, String brokerName, String accountNumber);

    /**
     * Find accounts by broker name
     */
    List<BrokerAccount> findByBrokerNameAndDeletedAtIsNull(String brokerName);

    /**
     * Find active broker accounts for user
     */
    List<BrokerAccount> findByUserIdAndIsActiveAndDeletedAtIsNull(String userId, boolean isActive);

    /**
     * Find verified accounts for user
     */
    List<BrokerAccount> findByUserIdAndIsVerifiedAndDeletedAtIsNull(String userId, boolean isVerified);

    /**
     * Find trading-enabled accounts for user
     */
    List<BrokerAccount> findByUserIdAndTradingEnabledAndDeletedAtIsNull(String userId, boolean tradingEnabled);

    /**
     * Find accounts by connection status
     */
    List<BrokerAccount> findByConnectionStatusAndDeletedAtIsNull(String connectionStatus);

    /**
     * Find all active accounts (not deleted)
     */
    List<BrokerAccount> findByDeletedAtIsNull();
}