package com.bisttrading.persistence.repository.factory;

import com.bisttrading.persistence.repository.impl.BaseRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Custom JPA repository factory for BIST Trading Platform
 * Creates BaseRepositoryImpl instances for all repositories
 */
public class BaseRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    public BaseRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JpaRepositoryImplementation<?, ?> getTargetRepository(
            RepositoryInformation information, EntityManager entityManager) {

        JpaEntityInformation<?, ?> entityInformation = getEntityInformation(information.getDomainType());

        Object repository = getTargetRepositoryViaReflection(
            information,
            BaseRepositoryImpl.class,
            entityInformation,
            entityManager
        );

        return (JpaRepositoryImplementation<?, ?>) repository;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return BaseRepositoryImpl.class;
    }
}