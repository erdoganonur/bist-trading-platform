package com.bisttrading.infrastructure.persistence.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Flyway configuration for BIST Trading Platform database migrations.
 * Handles database schema versioning and migration management.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlywayConfig {

    private final DataSource dataSource;

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;

    @Value("${spring.flyway.out-of-order:false}")
    private boolean outOfOrder;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String table;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.baseline-description:Initial baseline}")
    private String baselineDescription;

    @Value("${spring.flyway.encoding:UTF-8}")
    private String encoding;

    @Value("${spring.flyway.placeholder-replacement:true}")
    private boolean placeholderReplacement;

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    @Value("${spring.flyway.mixed:false}")
    private boolean mixed;

    @Value("${spring.flyway.group:false}")
    private boolean group;

    @Value("${spring.flyway.repair-on-migrate:false}")
    private boolean repairOnMigrate;

    /**
     * Flyway configuration and initialization.
     *
     * @return Configured Flyway instance
     */
    @Bean(initMethod = "migrate")
    @DependsOn("dataSource")
    public Flyway flyway() {
        if (!flywayEnabled) {
            log.info("Flyway devre dışı - migration atlanıyor");
            return null;
        }

        FluentConfiguration configuration = Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .table(table)
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .baselineDescription(baselineDescription)
            .validateOnMigrate(validateOnMigrate)
            .outOfOrder(outOfOrder)
            .encoding(encoding)
            .placeholderReplacement(placeholderReplacement)
            .cleanDisabled(cleanDisabled)
            .mixed(mixed)
            .group(group)
            .sqlMigrationSuffixes(".sql");

        // Add custom placeholders
        configuration.placeholders(getPlaceholders());

        // Configure callbacks for logging and monitoring
        configuration.callbacks(new FlywayMigrationCallback());

        // Custom migration resolver disabled for compatibility

        Flyway flyway = configuration.load();

        // Log configuration
        logFlywayConfiguration(flyway);

        // Validate current state before migration
        if (validateOnMigrate) {
            try {
                flyway.validate();
                log.info("Flyway validation başarılı");
            } catch (Exception e) {
                log.error("Flyway validation hatası: {}", e.getMessage(), e);
                if (repairOnMigrate) {
                    log.info("Repair mode aktif - migration repair yapılıyor");
                    flyway.repair();
                } else {
                    throw e;
                }
            }
        }

        return flyway;
    }

    /**
     * Gets custom placeholders for Flyway migrations.
     *
     * @return Placeholder map
     */
    private java.util.Map<String, String> getPlaceholders() {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();

        // Environment-specific placeholders
        placeholders.put("database.schema", "public");
        placeholders.put("application.name", "BIST Trading Platform");
        placeholders.put("migration.timestamp", String.valueOf(System.currentTimeMillis()));

        // Table and index naming conventions
        placeholders.put("table.prefix", "");
        placeholders.put("index.prefix", "idx_");

        // TimescaleDB specific placeholders
        placeholders.put("timescaledb.enabled", String.valueOf(isTimescaleDbAvailable()));

        log.debug("Flyway placeholders yapılandırıldı: {}", placeholders.size());
        return placeholders;
    }

    /**
     * Checks if TimescaleDB extension is available.
     *
     * @return true if TimescaleDB is available
     */
    private boolean isTimescaleDbAvailable() {
        try {
            var connection = dataSource.getConnection();
            var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM pg_available_extensions WHERE name = 'timescaledb'"
            );
            var resultSet = statement.executeQuery();
            resultSet.next();
            boolean available = resultSet.getInt(1) > 0;

            resultSet.close();
            statement.close();
            connection.close();

            log.info("TimescaleDB availability check: {}", available);
            return available;
        } catch (Exception e) {
            log.warn("TimescaleDB availability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Logs Flyway configuration for debugging.
     *
     * @param flyway Flyway instance
     */
    private void logFlywayConfiguration(Flyway flyway) {
        var info = flyway.info();

        log.info("Flyway yapılandırması:");
        log.info("- Locations: {}", String.join(", ", locations));
        log.info("- Table: {}", table);
        log.info("- Baseline version: {}", baselineVersion);
        log.info("- Validate on migrate: {}", validateOnMigrate);
        log.info("- Out of order: {}", outOfOrder);
        log.info("- Clean disabled: {}", cleanDisabled);

        if (info.current() != null) {
            log.info("- Current version: {}", info.current().getVersion());
            log.info("- Current description: {}", info.current().getDescription());
        } else {
            log.info("- Current version: No migrations applied");
        }

        log.info("- Pending migrations: {}", info.pending().length);
    }

    /**
     * Custom Flyway migration callback for logging and monitoring.
     */
    public static class FlywayMigrationCallback implements org.flywaydb.core.api.callback.Callback {

        @Override
        public boolean supports(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            return true;
        }

        @Override
        public boolean canHandleInTransaction(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            return true;
        }

        @Override
        public void handle(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            switch (event) {
                case BEFORE_MIGRATE:
                    log.info("Migration başlatılıyor...");
                    break;
                case AFTER_MIGRATE:
                    log.info("Migration tamamlandı");
                    break;
                case BEFORE_EACH_MIGRATE:
                    if (context.getMigrationInfo() != null) {
                        log.info("Migration uygulanıyor: {} - {}",
                            context.getMigrationInfo().getVersion(),
                            context.getMigrationInfo().getDescription());
                    }
                    break;
                case AFTER_EACH_MIGRATE:
                    if (context.getMigrationInfo() != null) {
                        log.info("Migration tamamlandı: {} ({}ms)",
                            context.getMigrationInfo().getVersion(),
                            context.getMigrationInfo().getExecutionTime());
                    }
                    break;
                case BEFORE_VALIDATE:
                    log.debug("Migration validation başlatılıyor...");
                    break;
                case AFTER_VALIDATE:
                    log.debug("Migration validation tamamlandı");
                    break;
                case AFTER_MIGRATE_ERROR:
                    log.error("Migration başarısız: {}",
                        context.getMigrationInfo() != null ?
                        context.getMigrationInfo().getDescription() : "Unknown");
                    break;
                default:
                    log.debug("Flyway event: {}", event);
                    break;
            }
        }

        @Override
        public String getCallbackName() {
            return "BistTradingFlywayCallback";
        }
    }

    /**
     * Custom migration resolver for handling special migration cases.
     * Note: Disabled for compatibility with newer Flyway versions
     */
    /*
    public static class CustomMigrationResolver implements org.flywaydb.core.api.resolver.MigrationResolver {

        @Override
        public java.util.Collection<org.flywaydb.core.api.resolver.ResolvedMigration> resolveMigrations(
                org.flywaydb.core.api.Configuration configuration) {
            // Custom migration resolution logic can be added here
            // For now, return empty collection to use default resolution
            return java.util.Collections.emptyList();
        }
    }
    */

    /**
     * Gets migration information.
     *
     * @return Migration info summary
     */
    public String getMigrationInfo() {
        if (!flywayEnabled) {
            return "Flyway disabled";
        }

        try {
            Flyway flyway = flyway();
            if (flyway == null) {
                return "Flyway not configured";
            }

            var info = flyway.info();
            var current = info.current();
            var pending = info.pending();

            return String.format(
                "Current: %s, Pending: %d, Applied: %d",
                current != null ? current.getVersion().toString() : "None",
                pending.length,
                info.applied().length
            );
        } catch (Exception e) {
            log.warn("Migration bilgisi alınamadı: {}", e.getMessage());
            return "Migration info unavailable";
        }
    }

    /**
     * Performs manual migration repair.
     *
     * @return Repair result
     */
    public String repairMigrations() {
        if (!flywayEnabled) {
            return "Flyway disabled";
        }

        try {
            Flyway flyway = flyway();
            if (flyway == null) {
                return "Flyway not configured";
            }

            flyway.repair();
            log.info("Migration repair başarıyla tamamlandı");
            return "Repair completed successfully";
        } catch (Exception e) {
            log.error("Migration repair hatası: {}", e.getMessage(), e);
            return "Repair failed: " + e.getMessage();
        }
    }

    /**
     * Validates current migration state.
     *
     * @return Validation result
     */
    public String validateMigrations() {
        if (!flywayEnabled) {
            return "Flyway disabled";
        }

        try {
            Flyway flyway = flyway();
            if (flyway == null) {
                return "Flyway not configured";
            }

            flyway.validate();
            log.info("Migration validation başarılı");
            return "Validation successful";
        } catch (Exception e) {
            log.error("Migration validation hatası: {}", e.getMessage(), e);
            return "Validation failed: " + e.getMessage();
        }
    }
}