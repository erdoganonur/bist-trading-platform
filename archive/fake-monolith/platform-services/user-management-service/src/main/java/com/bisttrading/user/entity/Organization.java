package com.bisttrading.user.entity;

import com.bisttrading.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Organization entity for corporate users
 */
@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {

    @Id
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "tax_number", unique = true, nullable = false, length = 20)
    private String taxNumber;

    @Column(name = "trade_register_number", length = 50)
    private String tradeRegisterNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false)
    private OrganizationType organizationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrganizationStatus status = OrganizationStatus.PENDING;

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<User> users;

    // Enums
    public enum OrganizationType {
        CORPORATION, PARTNERSHIP, SOLE_PROPRIETORSHIP, GOVERNMENT, NGO
    }

    public enum OrganizationStatus {
        PENDING, ACTIVE, SUSPENDED, DEACTIVATED
    }

    // Business methods
    public boolean isActive() {
        return active && status == OrganizationStatus.ACTIVE;
    }

    public int getUserCount() {
        return users != null ? users.size() : 0;
    }
}