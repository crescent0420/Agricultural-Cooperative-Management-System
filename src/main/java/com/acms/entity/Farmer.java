
package com.acms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "farmers")
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_number", nullable = false, updatable = false, length = 20)
    private String membershipNumber;

    @Column(name = "national_id", nullable = false, updatable = false, length = 16)
    private String nationalId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cooperative_id", nullable = false, updatable = false)
    private Cooperative cooperative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FarmerStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Farmer() {
    }

    public Farmer(String membershipNumber, String nationalId, String fullName,
                  String phoneNumber, Cooperative cooperative) {
        this.membershipNumber = membershipNumber;
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.cooperative = cooperative;
        this.status = FarmerStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String fullName, String phoneNumber) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() { return id; }
    public String getMembershipNumber() { return membershipNumber; }
    public String getNationalId() { return nationalId; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Cooperative getCooperative() { return cooperative; }
    public FarmerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
