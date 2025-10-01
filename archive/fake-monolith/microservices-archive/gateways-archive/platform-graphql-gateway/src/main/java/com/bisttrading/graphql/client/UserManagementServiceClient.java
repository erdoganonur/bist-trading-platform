package com.bisttrading.graphql.client;

import com.bisttrading.ums.dto.UserResponse;
import com.bisttrading.ums.dto.UpdateUserRequest;
import com.bisttrading.ums.dto.UserPreferencesResponse;
import com.bisttrading.common.dto.PagedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for User Management Service
 *
 * Provides async access to user management operations for GraphQL gateway
 */
@FeignClient(
    name = "user-management-service",
    url = "${service-clients.user-management.base-url}",
    configuration = ServiceClientConfiguration.class
)
public interface UserManagementServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    CompletableFuture<UserResponse> getUserById(@PathVariable String userId);

    @PutMapping("/api/v1/users/{userId}")
    CompletableFuture<UserResponse> updateUser(
        @PathVariable String userId,
        @RequestBody UpdateUserRequest request
    );

    @GetMapping("/api/v1/users")
    CompletableFuture<PagedResponse<UserResponse>> getUsers(
        @RequestParam(required = false) Object filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );

    @PutMapping("/api/v1/users/{userId}/preferences")
    CompletableFuture<UserPreferencesResponse> updateUserPreferences(
        @PathVariable String userId,
        @RequestBody Object input
    );

    @PostMapping("/api/v1/users/{userId}/change-password")
    CompletableFuture<Void> changePassword(
        @PathVariable String userId,
        @RequestParam String currentPassword,
        @RequestParam String newPassword
    );

    @PostMapping("/api/v1/users/{userId}/verify-email")
    CompletableFuture<Void> verifyEmail(
        @PathVariable String userId,
        @RequestParam String code
    );

    @PostMapping("/api/v1/users/{userId}/verify-phone")
    CompletableFuture<Void> verifyPhone(
        @PathVariable String userId,
        @RequestParam String code
    );

    @GetMapping("/api/v1/users/{userId}/sessions")
    CompletableFuture<List<Object>> getUserSessions(@PathVariable String userId);
}