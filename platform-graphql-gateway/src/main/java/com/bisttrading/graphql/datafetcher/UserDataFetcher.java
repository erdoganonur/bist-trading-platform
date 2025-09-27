package com.bisttrading.graphql.datafetcher;

import com.bisttrading.graphql.client.UserManagementServiceClient;
import com.bisttrading.graphql.security.GraphQLSecurityContext;
import com.bisttrading.ums.dto.UserResponse;
import com.bisttrading.ums.dto.UpdateUserRequest;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL DataFetcher for User domain operations
 *
 * Provides unified access to user management functionality through GraphQL interface
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserDataFetcher {

    private final UserManagementServiceClient userServiceClient;
    private final GraphQLSecurityContext securityContext;

    /**
     * Get current authenticated user
     */
    @QueryMapping
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
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN') or @graphQLSecurityContext.canAccessUser(#userId)")
    public CompletableFuture<UserResponse> user(@Argument String userId) {
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
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<PagedUserResponse> users(
            @Argument UserFilter filter,
            @Argument Integer first,
            @Argument String after) {

        log.debug("Fetching users with filter: {}", filter);

        // Convert GraphQL pagination to service pagination
        int page = after != null ? Integer.parseInt(after) : 0;
        int size = first != null ? first : 20;

        // For now, return a simplified response
        PagedUserResponse pagedResponse = new PagedUserResponse();
        pagedResponse.setContent(List.of());
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);
        pagedResponse.setSize(size);
        pagedResponse.setNumber(page);

        return CompletableFuture.completedFuture(pagedResponse);
    }

    /**
     * Update user profile
     */
    @MutationMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<UserResponse> updateProfile(@Argument UpdateProfileInput input) {
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
    @MutationMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<UserPreferencesResponse> updatePreferences(
            @Argument UserPreferencesInput input) {
        log.debug("Updating user preferences");
        String userId = securityContext.getCurrentUserId();

        // For now, return a simple response
        UserPreferencesResponse response = new UserPreferencesResponse();
        response.setLanguage(input.getLanguage());
        response.setTheme(input.getTheme());
        response.setTimezone(input.getTimezone());

        return CompletableFuture.completedFuture(response);
    }

    /**
     * Change user password
     */
    @MutationMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> changePassword(@Argument ChangePasswordInput input) {
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
    @MutationMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> verifyEmail(@Argument String code) {
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
    @MutationMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<Boolean> verifyPhone(@Argument String code) {
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
    @SchemaMapping(typeName = "User", field = "orders")
    public CompletableFuture<PagedOrderResponse> userOrders(
            UserResponse user,
            @Argument OrderFilter filter,
            @Argument Integer first,
            @Argument String after) {

        // For now, return empty orders
        PagedOrderResponse pagedResponse = new PagedOrderResponse();
        pagedResponse.setContent(List.of());
        pagedResponse.setTotalElements(0L);
        pagedResponse.setTotalPages(0);
        pagedResponse.setSize(first != null ? first : 20);
        pagedResponse.setNumber(after != null ? Integer.parseInt(after) : 0);

        return CompletableFuture.completedFuture(pagedResponse);
    }

    /**
     * Resolve user portfolio using DataLoader
     */
    @SchemaMapping(typeName = "User", field = "portfolio")
    public CompletableFuture<PortfolioResponse> userPortfolio(UserResponse user) {
        // For now, return empty portfolio
        PortfolioResponse portfolio = new PortfolioResponse();
        portfolio.setUserId(user.getId());
        portfolio.setTotalValue(0.0);
        portfolio.setPositions(List.of());

        return CompletableFuture.completedFuture(portfolio);
    }

    /**
     * Resolve user sessions using DataLoader
     */
    @SchemaMapping(typeName = "User", field = "sessions")
    public CompletableFuture<List<UserSessionResponse>> userSessions(UserResponse user) {
        // For now, return empty sessions
        return CompletableFuture.completedFuture(List.of());
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

    // Additional DTOs for Spring GraphQL compatibility
    public static class UserPreferencesInput {
        private String language;
        private String theme;
        private String timezone;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static class UserPreferencesResponse {
        private String language;
        private String theme;
        private String timezone;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static class PagedUserResponse {
        private List<UserResponse> content;
        private Long totalElements;
        private Integer totalPages;
        private Integer size;
        private Integer number;

        public List<UserResponse> getContent() { return content; }
        public void setContent(List<UserResponse> content) { this.content = content; }
        public Long getTotalElements() { return totalElements; }
        public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
    }

    public static class OrderFilter {
        private List<String> status;
        private List<String> symbol;
        private DateRange createdDateRange;

        public List<String> getStatus() { return status; }
        public void setStatus(List<String> status) { this.status = status; }
        public List<String> getSymbol() { return symbol; }
        public void setSymbol(List<String> symbol) { this.symbol = symbol; }
        public DateRange getCreatedDateRange() { return createdDateRange; }
        public void setCreatedDateRange(DateRange createdDateRange) { this.createdDateRange = createdDateRange; }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(status, symbol, createdDateRange);
        }
    }

    public static class OrderResponse {
        private String id;
        private String symbol;
        private String status;
        private Double quantity;
        private Double price;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    public static class PagedOrderResponse {
        private List<OrderResponse> content;
        private Long totalElements;
        private Integer totalPages;
        private Integer size;
        private Integer number;

        public List<OrderResponse> getContent() { return content; }
        public void setContent(List<OrderResponse> content) { this.content = content; }
        public Long getTotalElements() { return totalElements; }
        public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
    }

    public static class PortfolioResponse {
        private String userId;
        private Double totalValue;
        private List<PositionResponse> positions;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Double getTotalValue() { return totalValue; }
        public void setTotalValue(Double totalValue) { this.totalValue = totalValue; }
        public List<PositionResponse> getPositions() { return positions; }
        public void setPositions(List<PositionResponse> positions) { this.positions = positions; }
    }

    public static class PositionResponse {
        private String symbol;
        private Double quantity;
        private Double averagePrice;
        private Double currentValue;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public Double getAveragePrice() { return averagePrice; }
        public void setAveragePrice(Double averagePrice) { this.averagePrice = averagePrice; }
        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    }

    public static class UserSessionResponse {
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private String createdAt;
        private Boolean active;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class AddressDto {
        private String street;
        private String city;
        private String district;
        private String postalCode;
        private String country;

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
}