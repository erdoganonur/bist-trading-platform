package com.bisttrading.user.repository.trading;

import com.bisttrading.user.entity.trading.BrokerAccount;
import com.bisttrading.user.repository.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BrokerAccount entity operations
 * Provides broker integration and account management queries
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Repository
public interface BrokerAccountRepository extends BaseRepository<BrokerAccount, UUID> {

    // Basic account lookups

    /**
     * Find all broker accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.deletedAt IS NULL ORDER BY ba.createdAt DESC")
    List<BrokerAccount> findByUserId(@Param("userId") UUID userId);

    /**
     * Find broker account by user, broker name, and account number
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.brokerName = :brokerName " +
           "AND ba.accountNumber = :accountNumber AND ba.deletedAt IS NULL")
    Optional<BrokerAccount> findByUserIdAndBrokerNameAndAccountNumber(@Param("userId") UUID userId,
                                                                     @Param("brokerName") String brokerName,
                                                                     @Param("accountNumber") String accountNumber);

    /**
     * Find accounts by broker name
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.brokerName = :brokerName AND ba.deletedAt IS NULL")
    List<BrokerAccount> findByBrokerName(@Param("brokerName") String brokerName);

    // Active accounts

    /**
     * Find active broker accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.isActive = true AND ba.deletedAt IS NULL")
    List<BrokerAccount> findActiveAccountsByUserId(@Param("userId") UUID userId);

    /**
     * Find verified and active accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.isActive = true " +
           "AND ba.isVerified = true AND ba.deletedAt IS NULL")
    List<BrokerAccount> findVerifiedActiveAccountsByUserId(@Param("userId") UUID userId);

    /**
     * Find trading-enabled accounts for user
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.isActive = true " +
           "AND ba.isVerified = true AND ba.tradingEnabled = true AND ba.deletedAt IS NULL")
    List<BrokerAccount> findTradingEnabledAccountsByUserId(@Param("userId") UUID userId);

    /**
     * Find connected accounts
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.connectionStatus = 'CONNECTED' AND ba.deletedAt IS NULL")
    List<BrokerAccount> findConnectedAccounts();

    /**
     * Find disconnected or error accounts
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.connectionStatus IN ('DISCONNECTED', 'ERROR') AND ba.deletedAt IS NULL")
    List<BrokerAccount> findProblematicAccounts();

    // Balance and limits queries

    /**
     * Find accounts with sufficient buying power
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.user.id = :userId AND ba.buyingPower >= :requiredAmount " +
           "AND ba.isActive = true AND ba.tradingEnabled = true AND ba.deletedAt IS NULL")
    List<BrokerAccount> findAccountsWithSufficientBuyingPower(@Param("userId") UUID userId,
                                                             @Param("requiredAmount") BigDecimal requiredAmount);

    /**
     * Find accounts with high margin utilization
     */
    @Query(value = """
        SELECT ba.* FROM broker_accounts ba
        WHERE ba.deleted_at IS NULL
        AND ba.margin_used IS NOT NULL
        AND ba.total_equity IS NOT NULL
        AND ba.total_equity > 0
        AND (ba.margin_used / ba.total_equity) > :utilizationThreshold / 100.0
        ORDER BY (ba.margin_used / ba.total_equity) DESC
        """, nativeQuery = true)
    List<BrokerAccount> findHighMarginUtilizationAccounts(@Param("utilizationThreshold") BigDecimal utilizationThreshold);

    /**
     * Get account balance summary for user
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_accounts,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_accounts,
            COUNT(CASE WHEN trading_enabled = true THEN 1 END) as trading_accounts,
            COALESCE(SUM(cash_balance), 0) as total_cash,
            COALESCE(SUM(buying_power), 0) as total_buying_power,
            COALESCE(SUM(total_equity), 0) as total_equity,
            COALESCE(SUM(margin_used), 0) as total_margin_used
        FROM broker_accounts
        WHERE user_id = :userId AND deleted_at IS NULL
        """, nativeQuery = true)
    Object[] getAccountBalanceSummary(@Param("userId") UUID userId);

    // Sync and maintenance queries

    /**
     * Find accounts that need balance refresh
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.lastSyncAt IS NULL " +
           "OR ba.lastSyncAt < :staleThreshold AND ba.isActive = true AND ba.deletedAt IS NULL")
    List<BrokerAccount> findAccountsNeedingSync(@Param("staleThreshold") ZonedDateTime staleThreshold);

    /**
     * Find accounts with old error messages
     */
    @Query("SELECT ba FROM BrokerAccount ba WHERE ba.errorMessage IS NOT NULL " +
           "AND ba.updatedAt < :errorAgeThreshold AND ba.deletedAt IS NULL")
    List<BrokerAccount> findAccountsWithStaleErrors(@Param("errorAgeThreshold") ZonedDateTime errorAgeThreshold);

    /**
     * Count accounts by connection status
     */
    @Query("SELECT ba.connectionStatus, COUNT(ba) FROM BrokerAccount ba WHERE ba.deletedAt IS NULL GROUP BY ba.connectionStatus")
    List<Object[]> countAccountsByConnectionStatus();

    /**
     * Count accounts by broker
     */
    @Query("SELECT ba.brokerName, COUNT(ba) FROM BrokerAccount ba WHERE ba.deletedAt IS NULL GROUP BY ba.brokerName")
    List<Object[]> countAccountsByBroker();

    // Risk monitoring

    /**
     * Find accounts approaching daily trade limit
     */
    @Query(value = """
        SELECT ba.*, daily_volume.volume_used
        FROM broker_accounts ba
        LEFT JOIN (
            SELECT o.account_id,
                   SUM(o.quantity * COALESCE(o.average_fill_price, o.price)) as volume_used
            FROM orders o
            WHERE DATE(o.created_at) = CURRENT_DATE
            AND o.order_status IN ('FILLED', 'PARTIALLY_FILLED')
            GROUP BY o.account_id
        ) daily_volume ON ba.account_number = daily_volume.account_id
        WHERE ba.daily_trade_limit IS NOT NULL
        AND ba.deleted_at IS NULL
        AND (COALESCE(daily_volume.volume_used, 0) / ba.daily_trade_limit) > :utilizationThreshold / 100.0
        ORDER BY (COALESCE(daily_volume.volume_used, 0) / ba.daily_trade_limit) DESC
        """, nativeQuery = true)
    List<Object[]> findAccountsApproachingDailyLimit(@Param("utilizationThreshold") BigDecimal utilizationThreshold);

    /**
     * Get risk scores for all user accounts
     */
    @Query(value = """
        SELECT
            ba.broker_account_id,
            ba.broker_name,
            ba.account_number,
            CASE
                WHEN ba.margin_used IS NULL OR ba.total_equity IS NULL OR ba.total_equity = 0 THEN 0
                ELSE LEAST(100, (ba.margin_used / ba.total_equity * 100) * 0.4)
            END +
            CASE
                WHEN ba.connection_status != 'CONNECTED' THEN 30
                WHEN ba.last_sync_at < NOW() - INTERVAL '1 day' THEN 20
                WHEN ba.last_sync_at < NOW() - INTERVAL '6 hours' THEN 10
                ELSE 0
            END +
            CASE
                WHEN ba.is_active = false THEN 30
                WHEN ba.is_verified = false THEN 20
                WHEN ba.trading_enabled = false THEN 15
                WHEN ba.error_message IS NOT NULL THEN 10
                ELSE 0
            END as risk_score
        FROM broker_accounts ba
        WHERE ba.user_id = :userId AND ba.deleted_at IS NULL
        ORDER BY risk_score DESC
        """, nativeQuery = true)
    List<Object[]> getRiskScoresForUser(@Param("userId") UUID userId);

    // Bulk operations

    /**
     * Update connection status for multiple accounts
     */
    @Modifying
    @Query("UPDATE BrokerAccount ba SET ba.connectionStatus = :status, ba.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ba.id IN :accountIds")
    int updateConnectionStatus(@Param("accountIds") List<UUID> accountIds, @Param("status") String status);

    /**
     * Update balances for an account
     */
    @Modifying
    @Query("UPDATE BrokerAccount ba SET ba.cashBalance = :cashBalance, ba.buyingPower = :buyingPower, " +
           "ba.totalEquity = :totalEquity, ba.marginUsed = :marginUsed, ba.marginAvailable = :marginAvailable, " +
           "ba.lastSyncAt = CURRENT_TIMESTAMP, ba.connectionStatus = 'CONNECTED', ba.errorMessage = NULL, " +
           "ba.updatedAt = CURRENT_TIMESTAMP WHERE ba.id = :accountId")
    int updateAccountBalances(@Param("accountId") UUID accountId,
                             @Param("cashBalance") BigDecimal cashBalance,
                             @Param("buyingPower") BigDecimal buyingPower,
                             @Param("totalEquity") BigDecimal totalEquity,
                             @Param("marginUsed") BigDecimal marginUsed,
                             @Param("marginAvailable") BigDecimal marginAvailable);

    /**
     * Clear error messages for accounts
     */
    @Modifying
    @Query("UPDATE BrokerAccount ba SET ba.errorMessage = NULL, ba.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ba.errorMessage IS NOT NULL AND ba.updatedAt < :clearBefore")
    int clearOldErrorMessages(@Param("clearBefore") ZonedDateTime clearBefore);

    /**
     * Enable trading for verified accounts
     */
    @Modifying
    @Query("UPDATE BrokerAccount ba SET ba.tradingEnabled = true, ba.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ba.isVerified = true AND ba.isActive = true AND ba.tradingEnabled = false " +
           "AND ba.connectionStatus = 'CONNECTED' AND ba.deletedAt IS NULL")
    int enableTradingForVerifiedAccounts();

    // Reporting queries

    /**
     * Get account activity summary
     */
    @Query(value = """
        SELECT
            ba.broker_name,
            COUNT(ba.broker_account_id) as account_count,
            COUNT(CASE WHEN ba.is_active = true THEN 1 END) as active_accounts,
            COUNT(CASE WHEN ba.connection_status = 'CONNECTED' THEN 1 END) as connected_accounts,
            COALESCE(SUM(ba.total_equity), 0) as total_equity_sum,
            AVG(ba.total_equity) as avg_equity_per_account
        FROM broker_accounts ba
        WHERE ba.deleted_at IS NULL
        GROUP BY ba.broker_name
        ORDER BY total_equity_sum DESC
        """, nativeQuery = true)
    List<Object[]> getBrokerAccountSummary();

    /**
     * Get user trading capacity
     */
    @Query(value = """
        SELECT
            u.email,
            COUNT(ba.broker_account_id) as account_count,
            SUM(ba.buying_power) as total_buying_power,
            SUM(ba.daily_trade_limit) as total_daily_limit,
            AVG(CASE WHEN ba.margin_used IS NOT NULL AND ba.total_equity IS NOT NULL AND ba.total_equity > 0
                     THEN ba.margin_used / ba.total_equity * 100
                     ELSE 0 END) as avg_margin_utilization
        FROM users u
        JOIN broker_accounts ba ON u.user_id = ba.user_id
        WHERE ba.is_active = true AND ba.trading_enabled = true AND ba.deleted_at IS NULL
        GROUP BY u.user_id, u.email
        HAVING SUM(ba.buying_power) > :minBuyingPower
        ORDER BY total_buying_power DESC
        """, nativeQuery = true)
    List<Object[]> getUserTradingCapacity(@Param("minBuyingPower") BigDecimal minBuyingPower);
}