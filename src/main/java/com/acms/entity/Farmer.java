package com.acms.entity;

import com.acms.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "farmers")
public class Farmer extends AuditableEntity {

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

    @Version
    private Long version;

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

    public void updateDetails(String fullName, String phoneNumber) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public void changeStatus(FarmerStatus target) {
        if (!status.canChangeTo(target)) {
            throw new IllegalStateException(
                    "Cannot change status from " + status + " to " + target + "."
            );
        }

        this.status = target;
    }

    public String getMembershipNumber() { return membershipNumber; }
    public String getNationalId() { return nationalId; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public Cooperative getCooperative() { return cooperative; }
    public FarmerStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}