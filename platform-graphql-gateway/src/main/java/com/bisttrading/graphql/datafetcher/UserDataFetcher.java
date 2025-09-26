package com.bisttrading.graphql.datafetcher;

import com.bisttrading.graphql.client.UserManagementServiceClient;
import com.bisttrading.graphql.security.GraphQLSecurityContext;
import com.bisttrading.ums.dto.UserResponse;
import com.bisttrading.ums.dto.UpdateUserRequest;
import com.bisttrading.ums.dto.UserPreferencesResponse;
import com.bisttrading.common.dto.PagedResponse;
import com.netflix.dgs.codegen.generated.types.*;
import com.bisttrading.oms.dto.OrderResponse;
import com.bisttrading.portfolio.dto.PortfolioResponse;
import com.bisttrading.sessions.dto.UserSessionResponse;
import com.bisttrading.ums.dto.AddressDto;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL DataFetcher for User domain operations
 *
 * Provides unified access to user management functionality through GraphQL interface
 */
@Slf4j
@DgsComponent
@RequiredArgsConstructor
public class UserDataFetcher {

    private final UserManagementServiceClient userServiceClient;
    private final GraphQLSecurityContext securityContext;

    /**
     * Get current authenticated user
     */
    @DgsQuery
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<UserResponse> me() {
        log.debug("Fetching current user profile");
        String userId = securityContext.getCurrentUserId();
        return userServiceClient.getUserById(userId)
            .exceptionally(throwable -> {
                log.error("Error fetching current user: ", throwable);
                throw new RuntimeException("Failed to fetch user profile", throwable);
            });
    }

    /**
     * Get user by ID (admin only or self)
     */
    @DgsQuery
    @PreAuthorize("hasRole('ADMIN') or @graphQLSecurityContext.canAccessUser(#userId)")
    public CompletableFuture<UserResponse> user(@InputArgument String userId) {
        log.debug("Fetching user profile for userId: {}", userId);
        return userServiceClient.getUserById(userId)
            .exceptionally(throwable -> {
                log.error("Error fetching user {}: ", userId, throwable);
                throw new RuntimeException("Failed to fetch user: " + userId, throwable);
            });
    }

    /**
     * Search and filter users (admin only)
     */
    @DgsQuery
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PagedResponse<UserResponse>> users(
            @InputArgument UserFilter filter,
            @InputArgument Integer first,
            @InputArgument String after) {

        log.debug("Fetching users with filter: {}", filter);

        // Convert GraphQL pagination to service pagination
        int page = after != null ? Integer.parseInt(after) : 0;
        int size = first != null ? first : 20;

        return userServiceClient.getUsers(filter, page, size)
            .exceptionally(throwable -> {
                log.error("Error fetching users: ", throwable);
                throw new RuntimeException("Failed to fetch users", throwable);
            });
    }

    /**
     * Update user profile
     */
    @DgsMutation
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<UserResponse> updateProfile(@InputArgument UpdateProfileInput input) {
        log.debug("Updating user profile");
        String userId = securityContext.getCurrentUserId();

        UpdateUserRequest request = mapToUpdateUserRequest(input, userId);

        return userServiceClient.updateUser(userId, request)
            .exceptionally(throwable -> {
                log.error("Error updating user profile: ", throwable);
                throw new RuntimeException("Failed to update profile", throwable);
            });
    }

    /**
     * Update user preferences
     */
    @DgsMutation
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<UserPreferencesResponse> updatePreferences(
            @InputArgument UserPreferencesInput input) {
        log.debug("Updating user preferences");
        String userId = securityContext.getCurrentUserId();

        return userServiceClient.updateUserPreferences(userId, input)
            .exceptionally(throwable -> {
                log.error("Error updating user preferences: ", throwable);
                throw new RuntimeException("Failed to update preferences", throwable);
            });
    }

    /**
     * Change user password
     */
    @DgsMutation
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> changePassword(@InputArgument ChangePasswordInput input) {
        log.debug("Changing user password");
        String userId = securityContext.getCurrentUserId();

        return userServiceClient.changePassword(userId, input.getCurrentPassword(), input.getNewPassword())
            .thenApply(response -> true)
            .exceptionally(throwable -> {
                log.error("Error changing password: ", throwable);
                throw new RuntimeException("Failed to change password", throwable);
            });
    }

    /**
     * Verify email with code
     */
    @DgsMutation
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> verifyEmail(@InputArgument String code) {
        log.debug("Verifying email with code");
        String userId = securityContext.getCurrentUserId();

        return userServiceClient.verifyEmail(userId, code)
            .thenApply(response -> true)
            .exceptionally(throwable -> {
                log.error("Error verifying email: ", throwable);
                throw new RuntimeException("Failed to verify email", throwable);
            });
    }

    /**
     * Verify phone with code
     */
    @DgsMutation
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> verifyPhone(@InputArgument String code) {
        log.debug("Verifying phone with code");
        String userId = securityContext.getCurrentUserId();

        return userServiceClient.verifyPhone(userId, code)
            .thenApply(response -> true)
            .exceptionally(throwable -> {
                log.error("Error verifying phone: ", throwable);
                throw new RuntimeException("Failed to verify phone", throwable);
            });
    }

    // ===============================
    // Field resolvers (DataLoader usage)
    // ===============================

    /**
     * Resolve user orders using DataLoader to prevent N+1 problem
     */
    @DgsData(parentType = "User", field = "orders")
    public CompletableFuture<PagedResponse<OrderResponse>> userOrders(
            DgsDataFetchingEnvironment dfe,
            @InputArgument OrderFilter filter,
            @InputArgument Integer first,
            @InputArgument String after) {

        UserResponse user = dfe.getSource();
        DataLoader<String, PagedResponse<OrderResponse>> dataLoader =
            dfe.getDataLoader("userOrdersLoader");

        // Create cache key with filters
        String cacheKey = createOrdersCacheKey(user.getId(), filter, first, after);

        return dataLoader.load(cacheKey);
    }

    /**
     * Resolve user portfolio using DataLoader
     */
    @DgsData(parentType = "User", field = "portfolio")
    public CompletableFuture<PortfolioResponse> userPortfolio(DgsDataFetchingEnvironment dfe) {
        UserResponse user = dfe.getSource();
        DataLoader<String, PortfolioResponse> dataLoader =
            dfe.getDataLoader("userPortfolioLoader");

        return dataLoader.load(user.getId());
    }

    /**
     * Resolve user sessions using DataLoader
     */
    @DgsData(parentType = "User", field = "sessions")
    public CompletableFuture<List<UserSessionResponse>> userSessions(DgsDataFetchingEnvironment dfe) {
        UserResponse user = dfe.getSource();
        DataLoader<String, List<UserSessionResponse>> dataLoader =
            dfe.getDataLoader("userSessionsLoader");

        return dataLoader.load(user.getId());
    }

    // ===============================
    // Helper methods
    // ===============================

    private UpdateUserRequest mapToUpdateUserRequest(UpdateProfileInput input, String userId) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName(input.getFirstName());
        request.setLastName(input.getLastName());
        return request;
    }

    private AddressDto mapAddressInput(AddressInput addressInput) {
        AddressDto address = new AddressDto();
        address.setStreet(addressInput.getStreet());
        address.setCity(addressInput.getCity());
        address.setPostalCode(addressInput.getPostalCode());
        address.setCountry(addressInput.getCountry());
        return address;
    }

    private String createOrdersCacheKey(String userId, OrderFilter filter, Integer first, String after) {
        StringBuilder keyBuilder = new StringBuilder(userId);

        if (filter != null) {
            keyBuilder.append(":").append(filter.hashCode());
        }
        if (first != null) {
            keyBuilder.append(":first=").append(first);
        }
        if (after != null) {
            keyBuilder.append(":after=").append(after);
        }

        return keyBuilder.toString();
    }

    // Inner classes for input mapping (these would normally be generated from GraphQL schema)
    public static class UpdateProfileInput {
        private String firstName;
        private String lastName;
        private String phone;
        private AddressInput address;
        private String riskProfile;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public AddressInput getAddress() { return address; }
        public void setAddress(AddressInput address) { this.address = address; }
        public String getRiskProfile() { return riskProfile; }
        public void setRiskProfile(String riskProfile) { this.riskProfile = riskProfile; }
    }

    public static class AddressInput {
        private String street;
        private String city;
        private String district;
        private String postalCode;
        private String country;

        // Getters and setters
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    public static class ChangePasswordInput {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class UserFilter {
        private List<String> status;
        private List<String> kycLevel;
        private DateRange registrationDateRange;
        private DateRange lastLoginDateRange;
        private List<String> riskProfile;

        // Getters and setters with proper hashCode for caching
        @Override
        public int hashCode() {
            return java.util.Objects.hash(status, kycLevel, registrationDateRange, lastLoginDateRange, riskProfile);
        }
    }

    public static class DateRange {
        private String start;
        private String end;

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(start, end);
        }
    }
}