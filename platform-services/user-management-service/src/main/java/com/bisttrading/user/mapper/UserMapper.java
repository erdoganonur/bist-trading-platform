package com.bisttrading.user.mapper;

import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.entity.UserSessionEntity;
import com.bisttrading.user.dto.UserProfileDto;
import com.bisttrading.user.dto.UserSessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between User entities and DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Converts UserEntity to UserProfileDto.
     *
     * @param user User entity
     * @return User profile DTO
     */
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "fullyVerified", ignore = true)
    @Mapping(target = "canTrade", ignore = true)
    UserProfileDto toProfileDto(UserEntity user);

    /**
     * Converts UserSessionEntity to UserSessionDto.
     *
     * @param session User session entity
     * @return User session DTO
     */
    @Mapping(target = "deviceDescription", ignore = true)
    @Mapping(target = "sessionDurationMinutes", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "isCurrent", constant = "false")
    UserSessionDto toSessionDto(UserSessionEntity session);

    /**
     * Updates UserEntity from UserProfileDto (for partial updates).
     *
     * @param profileDto Source DTO
     * @param user Target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "phoneVerifiedAt", ignore = true)
    @Mapping(target = "kycCompleted", ignore = true)
    @Mapping(target = "kycCompletedAt", ignore = true)
    @Mapping(target = "kycLevel", ignore = true)
    @Mapping(target = "professionalInvestor", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "accountLockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "passwordExpiresAt", ignore = true)
    @Mapping(target = "emailVerificationCode", ignore = true)
    @Mapping(target = "emailVerificationExpiry", ignore = true)
    @Mapping(target = "phoneVerificationCode", ignore = true)
    @Mapping(target = "phoneVerificationExpiry", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    @Mapping(target = "twoFactorSecret", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "fullyVerified", ignore = true)
    @Mapping(target = "canTrade", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    void updateUserFromProfile(UserProfileDto profileDto, @MappingTarget UserEntity user);
}