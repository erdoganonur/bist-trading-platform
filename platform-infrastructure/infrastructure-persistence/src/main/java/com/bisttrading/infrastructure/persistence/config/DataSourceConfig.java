package com.bisttrading.infrastructure.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DataSource configuration for BIST Trading Platform persistence layer.
 * Configures HikariCP connection pool for PostgreSQL/TimescaleDB.
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    // HikariCP specific configurations
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Value("${spring.datasource.hikari.pool-name:BistTradingPool}")
    private String poolName;

    /**
     * Primary DataSource bean with HikariCP configuration.
     *
     * @return Configured HikariDataSource
     */
    @Primary
    @Bean("dataSource")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setPoolName(poolName);

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // PostgreSQL specific optimizations
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        config.addDataSourceProperty("connectTimeout", "10");

        // Application-specific settings
        config.addDataSourceProperty("ApplicationName", "BIST-Trading-Platform");

        // Security settings
        config.addDataSourceProperty("sslmode", "prefer");

        // TimescaleDB compatibility
        config.addDataSourceProperty("prepareThreshold", "0");

        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("HikariCP DataSource yapılandırıldı - Pool: {}, Max Size: {}, Min Idle: {}",
            poolName, maximumPoolSize, minimumIdle);

        return dataSource;
    }

    /**
     * Read-only DataSource for reporting and analytics queries.
     * This can be configured to point to a read replica if available.
     *
     * @return Read-only HikariDataSource
     */
    @Bean("readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        // For now, use the same configuration as primary
        // In production, this could point to a read replica
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Smaller pool for read-only operations
        config.setMaximumPoolSize(Math.max(1, maximumPoolSize / 2));
        config.setMinimumIdle(Math.max(1, minimumIdle / 2));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setPoolName(poolName + "-ReadOnly");

        // Read-only specific settings
        config.setReadOnly(true);
        config.addDataSourceProperty("defaultRowFetchSize", "1000");
        config.addDataSourceProperty("readOnly", "true");

        // Performance optimizations for read operations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");

        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("Read-only DataSource yapılandırıldı - Pool: {}", config.getPoolName());

        return dataSource;
    }

    /**
     * Validates the DataSource configuration.
     *
     * @param dataSource DataSource to validate
     * @return true if valid
     */
    private boolean validateDataSource(DataSource dataSource) {
        try {
            dataSource.getConnection().close();
            log.info("DataSource bağlantı testi başarılı");
            return true;
        } catch (Exception e) {
            log.error("DataSource bağlantı testi başarısız: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the JDBC URL without sensitive information for logging.
     *
     * @return Masked JDBC URL
     */
    public String getMaskedJdbcUrl() {
        if (jdbcUrl == null) {
            return null;
        }

        // Remove password from URL if present
        return jdbcUrl.replaceAll("password=[^&]*", "password=***");
    }

    /**
     * Health check method for DataSource.
     *
     * @return Health status
     */
    public boolean isHealthy() {
        try {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource();
            return hikariDataSource.getHikariPoolMXBean().getActiveConnections() >= 0;
        } catch (Exception e) {
            log.warn("DataSource sağlık kontrolü başarısız: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets connection pool statistics.
     *
     * @return Pool statistics as formatted string
     */
    public String getPoolStatistics() {
        try {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource();
            var poolMXBean = hikariDataSource.getHikariPoolMXBean();

            return String.format(
                "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getTotalConnections(),
                poolMXBean.getThreadsAwaitingConnection()
            );
        } catch (Exception e) {
            log.warn("Pool istatistikleri alınamadı: {}", e.getMessage());
            return "Pool statistics unavailable";
        }
    }
}