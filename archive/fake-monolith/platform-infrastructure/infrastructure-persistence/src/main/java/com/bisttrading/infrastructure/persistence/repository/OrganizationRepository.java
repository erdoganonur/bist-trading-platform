package com.bisttrading.infrastructure.persistence.repository;

import com.bisttrading.infrastructure.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrganizationEntity operations.
 * Provides custom query methods for organization management.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

    /**
     * Finds organization by organization code.
     *
     * @param organizationCode Organization code
     * @return Optional OrganizationEntity
     */
    Optional<OrganizationEntity> findByOrganizationCode(String organizationCode);

    /**
     * Finds organization by name.
     *
     * @param name Organization name
     * @return Optional OrganizationEntity
     */
    Optional<OrganizationEntity> findByName(String name);

    /**
     * Checks if organization exists by code.
     *
     * @param organizationCode Organization code
     * @return true if exists
     */
    boolean existsByOrganizationCode(String organizationCode);

    /**
     * Checks if organization exists by name.
     *
     * @param name Organization name
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Finds all organizations by type.
     *
     * @param organizationType Organization type
     * @return List of organizations
     */
    List<OrganizationEntity> findByOrganizationType(OrganizationEntity.OrganizationType organizationType);

    /**
     * Finds all organizations by status.
     *
     * @param status Organization status
     * @return List of organizations
     */
    List<OrganizationEntity> findByStatus(OrganizationEntity.OrganizationStatus status);

    /**
     * Finds all active organizations (not deleted).
     *
     * @return List of active organizations
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.status = 'ACTIVE' AND o.deletedAt IS NULL")
    List<OrganizationEntity> findAllActive();

    /**
     * Finds organizations with trading enabled.
     *
     * @return List of trading-enabled organizations
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.tradingEnabled = true AND o.status = 'ACTIVE' AND o.deletedAt IS NULL")
    List<OrganizationEntity> findTradingEnabledOrganizations();

    /**
     * Finds organizations by country.
     *
     * @param country Country code
     * @return List of organizations in specified country
     */
    List<OrganizationEntity> findByCountry(String country);

    /**
     * Finds organizations by city.
     *
     * @param city City name
     * @return List of organizations in specified city
     */
    List<OrganizationEntity> findByCity(String city);

    /**
     * Finds organizations with valid licenses.
     *
     * @param now Current timestamp
     * @return List of organizations with valid licenses
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.licenseExpiresAt > :now AND o.licenseNumber IS NOT NULL")
    List<OrganizationEntity> findOrganizationsWithValidLicenses(@Param("now") LocalDateTime now);

    /**
     * Finds organizations with expiring licenses.
     *
     * @param startDate Start of expiry window
     * @param endDate End of expiry window
     * @return List of organizations with expiring licenses
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.licenseExpiresAt BETWEEN :startDate AND :endDate")
    List<OrganizationEntity> findOrganizationsWithExpiringLicenses(@Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Finds organizations near user capacity limit.
     *
     * @param threshold Percentage threshold (e.g., 0.9 for 90%)
     * @return List of organizations near capacity
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.maxUsers IS NOT NULL AND " +
           "(CAST(o.currentUsers AS double) / CAST(o.maxUsers AS double)) >= :threshold")
    List<OrganizationEntity> findOrganizationsNearCapacity(@Param("threshold") double threshold);

    /**
     * Finds organizations that can add more users.
     *
     * @return List of organizations with available user slots
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.status = 'ACTIVE' AND o.deletedAt IS NULL AND " +
           "(o.maxUsers IS NULL OR o.currentUsers < o.maxUsers)")
    List<OrganizationEntity> findOrganizationsWithAvailableUserSlots();

    /**
     * Finds organizations created within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of organizations created in date range
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<OrganizationEntity> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Counts organizations by type.
     *
     * @param organizationType Organization type
     * @return Number of organizations of specified type
     */
    long countByOrganizationType(OrganizationEntity.OrganizationType organizationType);

    /**
     * Counts organizations by status.
     *
     * @param status Organization status
     * @return Number of organizations with specified status
     */
    long countByStatus(OrganizationEntity.OrganizationStatus status);

    /**
     * Counts active organizations.
     *
     * @return Number of active organizations
     */
    @Query("SELECT COUNT(o) FROM OrganizationEntity o WHERE o.status = 'ACTIVE' AND o.deletedAt IS NULL")
    long countActiveOrganizations();

    /**
     * Updates organization current user count.
     *
     * @param organizationId Organization ID
     * @param currentUsers New current user count
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.currentUsers = :currentUsers WHERE o.id = :organizationId")
    int updateCurrentUserCount(@Param("organizationId") String organizationId,
                              @Param("currentUsers") int currentUsers);

    /**
     * Increments organization current user count.
     *
     * @param organizationId Organization ID
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.currentUsers = o.currentUsers + 1 WHERE o.id = :organizationId")
    int incrementCurrentUserCount(@Param("organizationId") String organizationId);

    /**
     * Decrements organization current user count.
     *
     * @param organizationId Organization ID
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.currentUsers = GREATEST(o.currentUsers - 1, 0) WHERE o.id = :organizationId")
    int decrementCurrentUserCount(@Param("organizationId") String organizationId);

    /**
     * Updates organization trading status.
     *
     * @param organizationId Organization ID
     * @param tradingEnabled Trading enabled status
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.tradingEnabled = :tradingEnabled WHERE o.id = :organizationId")
    int updateTradingStatus(@Param("organizationId") String organizationId,
                           @Param("tradingEnabled") boolean tradingEnabled);

    /**
     * Updates organization license information.
     *
     * @param organizationId Organization ID
     * @param licenseNumber License number
     * @param licenseAuthority License authority
     * @param licenseIssuedAt License issue date
     * @param licenseExpiresAt License expiry date
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.licenseNumber = :licenseNumber, " +
           "o.licenseAuthority = :licenseAuthority, o.licenseIssuedAt = :licenseIssuedAt, " +
           "o.licenseExpiresAt = :licenseExpiresAt WHERE o.id = :organizationId")
    int updateLicenseInfo(@Param("organizationId") String organizationId,
                         @Param("licenseNumber") String licenseNumber,
                         @Param("licenseAuthority") String licenseAuthority,
                         @Param("licenseIssuedAt") LocalDateTime licenseIssuedAt,
                         @Param("licenseExpiresAt") LocalDateTime licenseExpiresAt);

    /**
     * Soft deletes organization by setting deleted timestamp.
     *
     * @param organizationId Organization ID
     * @param deletedAt Deletion timestamp
     * @return Number of updated records
     */
    @Query("UPDATE OrganizationEntity o SET o.deletedAt = :deletedAt, o.status = 'CLOSED' " +
           "WHERE o.id = :organizationId")
    int softDeleteOrganization(@Param("organizationId") String organizationId,
                              @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Searches organizations by partial name or code match.
     *
     * @param searchTerm Search term
     * @return List of matching organizations
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE " +
           "(LOWER(o.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.organizationCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "o.deletedAt IS NULL")
    List<OrganizationEntity> searchOrganizations(@Param("searchTerm") String searchTerm);

    /**
     * Finds organizations by license authority.
     *
     * @param licenseAuthority License authority
     * @return List of organizations licensed by specified authority
     */
    List<OrganizationEntity> findByLicenseAuthority(String licenseAuthority);

    /**
     * Finds organizations that need license renewal.
     *
     * @param warningDays Number of days before expiry to warn
     * @return List of organizations needing license renewal
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.licenseExpiresAt IS NOT NULL AND " +
           "o.licenseExpiresAt BETWEEN CURRENT_TIMESTAMP AND (CURRENT_TIMESTAMP + :warningDays DAY)")
    List<OrganizationEntity> findOrganizationsNeedingLicenseRenewal(@Param("warningDays") int warningDays);

    /**
     * Finds organizations by contact email.
     *
     * @param contactEmail Contact email
     * @return List of organizations with specified contact email
     */
    List<OrganizationEntity> findByContactEmail(String contactEmail);

    /**
     * Finds organizations with website URL.
     *
     * @return List of organizations that have website URLs
     */
    @Query("SELECT o FROM OrganizationEntity o WHERE o.websiteUrl IS NOT NULL AND o.websiteUrl != ''")
    List<OrganizationEntity> findOrganizationsWithWebsite();

    /**
     * Gets organization statistics summary.
     *
     * @return List of objects containing organization statistics
     */
    @Query("SELECT o.organizationType, o.status, COUNT(o) as count FROM OrganizationEntity o " +
           "WHERE o.deletedAt IS NULL GROUP BY o.organizationType, o.status")
    List<Object[]> getOrganizationStatistics();
}