package com.bisttrading.user.mapper;

import com.bisttrading.user.entity.UserEntity;
import com.bisttrading.user.entity.UserSessionEntity;
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
    UserProfileDto toProfileDto(UserEntity user);

    /**
     * Converts UserSessionEntity to UserSessionDto.
     *
     * @param session User session entity
     * @return User session DTO
     */
    @Mapping(target = "isCurrent", constant = "false")
    UserSessionDto toSessionDto(UserSessionEntity session);

    /**
     * Updates UserEntity from UserProfileDto (for partial updates).
     *
     * @param profileDto Source DTO
     * @param user Target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetExpiresAt", ignore = true)
    @Mapping(target = "passwordExpiresAt", ignore = true)
    @Mapping(target = "emailVerificationToken", ignore = true)
    @Mapping(target = "phoneVerificationCode", ignore = true)
    @Mapping(target = "loginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void updateUserFromProfile(UserProfileDto profileDto, @MappingTarget UserEntity user);
}