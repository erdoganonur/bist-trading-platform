package com.bisttrading.infrastructure.persistence.config;

import com.bisttrading.infrastructure.persistence.converter.FieldEncryptionConverter;
import com.bisttrading.infrastructure.persistence.converter.JsonbConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.Properties;

/**
 * JPA configuration for BIST Trading Platform persistence layer.
 * Configures Hibernate, auditing, and transaction management.
 */
@Slf4j
@Configuration
@EnableJpaRepositories(
    basePackages = "com.bisttrading.infrastructure.persistence.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
@RequiredArgsConstructor
public class JpaConfig {

    private final DataSource dataSource;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:false}")
    private boolean formatSql;

    @Value("${spring.jpa.properties.hibernate.use_sql_comments:false}")
    private boolean useSqlComments;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy}")
    private String physicalNamingStrategy;

    @Value("${spring.jpa.hibernate.naming.implicit-strategy:org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy}")
    private String implicitNamingStrategy;

    /**
     * EntityManagerFactory configuration with Hibernate settings.
     *
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bisttrading.infrastructure.persistence.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(showSql);
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaProperties(hibernateProperties());

        log.info("EntityManagerFactory yapılandırıldı - DDL: {}, Show SQL: {}", ddlAuto, showSql);
        return em;
    }

    /**
     * Transaction manager configuration.
     *
     * @param entityManagerFactory EntityManagerFactory
     * @return PlatformTransactionManager
     */
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        // Configure transaction timeout (30 seconds default)
        transactionManager.setDefaultTimeout(30);

        log.info("JPA TransactionManager yapılandırıldı");
        return transactionManager;
    }

    /**
     * Hibernate properties configuration.
     *
     * @return Properties with Hibernate settings
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();

        // Basic Hibernate settings
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        properties.setProperty("hibernate.use_sql_comments", String.valueOf(useSqlComments));

        // Naming strategies
        properties.setProperty("hibernate.physical_naming_strategy", physicalNamingStrategy);
        properties.setProperty("hibernate.implicit_naming_strategy", implicitNamingStrategy);

        // Performance optimizations
        properties.setProperty("hibernate.jdbc.batch_size", "25");
        properties.setProperty("hibernate.jdbc.fetch_size", "100");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.batch_versioned_data", "true");

        // Connection pooling with Hikari (handled by DataSource)
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");

        // Cache settings
        properties.setProperty("hibernate.cache.use_second_level_cache", "true");
        properties.setProperty("hibernate.cache.use_query_cache", "true");
        properties.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.jcache.JCacheRegionFactory");

        // Statistics and monitoring
        properties.setProperty("hibernate.generate_statistics", "true");
        properties.setProperty("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");

        // PostgreSQL specific optimizations
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true");
        properties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");

        // JSON support for PostgreSQL
        properties.setProperty("hibernate.type_contributors", "io.hypersistence.utils.hibernate.type.json.JsonTypeContributor");

        // Timezone handling
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");

        // Validation
        properties.setProperty("hibernate.validator.apply_to_ddl", "false");

        // TimescaleDB compatibility
        properties.setProperty("hibernate.id.new_generator_mappings", "false");

        log.info("Hibernate properties yapılandırıldı - Dialect: PostgreSQL, Batch Size: 25");
        return properties;
    }

    /**
     * Hibernate properties customizer for additional configuration.
     *
     * @return HibernatePropertiesCustomizer
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // Additional runtime customizations can be added here
            hibernateProperties.put("hibernate.integration.envers.enabled", false);

            // Configure custom types if needed
            hibernateProperties.put("hibernate.type.json_format_mapper", JsonbConverter.getObjectMapper());

            log.debug("Hibernate properties özelleştirildi");
        };
    }

    /**
     * Auditor provider for JPA auditing.
     * Provides the current user for @CreatedBy and @LastModifiedBy annotations.
     *
     * @return AuditorAware<String>
     */
    @Bean("auditorProvider")
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            String username = authentication.getName();
            if ("anonymousUser".equals(username)) {
                return Optional.of("anonymous");
            }

            return Optional.of(username);
        };
    }

    /**
     * Field encryption converter bean.
     *
     * @return FieldEncryptionConverter
     */
    @Bean
    public FieldEncryptionConverter fieldEncryptionConverter() {
        return new FieldEncryptionConverter("");
    }

    /**
     * JSONB converter bean.
     *
     * @return JsonbConverter
     */
    @Bean
    public JsonbConverter jsonbConverter() {
        return new JsonbConverter();
    }

    /**
     * Custom repository factory bean for additional repository functionality.
     *
     * @return Custom repository factory
     */
    @Bean
    public CustomRepositoryFactoryBean customRepositoryFactoryBean() {
        return new CustomRepositoryFactoryBean();
    }

    /**
     * Custom repository factory for adding common repository functionality.
     */
    public static class CustomRepositoryFactoryBean {

        public CustomRepositoryFactoryBean() {
            log.debug("Custom repository factory oluşturuldu");
        }

        /**
         * Provides common repository operations.
         *
         * @return Repository operations helper
         */
        public RepositoryOperations getRepositoryOperations() {
            return new RepositoryOperations();
        }
    }

    /**
     * Common repository operations helper.
     */
    public static class RepositoryOperations {

        /**
         * Executes a batch operation with proper session management.
         *
         * @param operation Batch operation to execute
         */
        public void executeBatch(Runnable operation) {
            try {
                operation.run();
                log.debug("Batch operasyon başarıyla tamamlandı");
            } catch (Exception e) {
                log.error("Batch operasyon hatası: {}", e.getMessage(), e);
                throw e;
            }
        }

        /**
         * Validates entity before persistence operations.
         *
         * @param entity Entity to validate
         * @return true if valid
         */
        public boolean validateEntity(Object entity) {
            if (entity == null) {
                log.warn("Null entity validation attempted");
                return false;
            }

            log.debug("Entity validation: {}", entity.getClass().getSimpleName());
            return true;
        }
    }

    /**
     * Database health check.
     *
     * @return Health status
     */
    public boolean isDatabaseHealthy() {
        try {
            EntityManagerFactory emf = entityManagerFactory().getObject();
            if (emf != null) {
                var em = emf.createEntityManager();
                em.createNativeQuery("SELECT 1").getSingleResult();
                em.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Veritabanı sağlık kontrolü başarısız: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets JPA configuration summary.
     *
     * @return Configuration summary
     */
    public String getConfigurationSummary() {
        return String.format(
            "JPA Config - DDL: %s, Show SQL: %s, Batch Size: 25, Cache: Enabled",
            ddlAuto, showSql
        );
    }
}