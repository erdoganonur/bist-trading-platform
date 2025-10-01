package com.bisttrading.persistence.config;

import com.bisttrading.persistence.repository.factory.BaseRepositoryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Repository configuration for BIST Trading Platform
 * Configures custom repository factory and settings
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.bisttrading.*.repository",
        "com.bisttrading.persistence.repository"
    },
    repositoryFactoryBeanClass = RepositoryConfiguration.BaseRepositoryFactoryBean.class
)
public class RepositoryConfiguration {

    /**
     * Custom repository factory bean for injecting BaseRepositoryImpl
     */
    public static class BaseRepositoryFactoryBean extends org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean {

        public BaseRepositoryFactoryBean(Class repositoryInterface) {
            super(repositoryInterface);
        }

        @Override
        protected org.springframework.data.repository.core.support.RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
            return new BaseRepositoryFactory(entityManager);
        }
    }
}