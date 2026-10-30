package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerStatusChangeRequest;
import com.acms.dto.FarmerSummaryResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.entity.Cooperative;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import com.acms.entity.Role;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.FarmerRepository;
import com.acms.repository.FarmerSpecifications;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "fullName", "membershipNumber", "status", "createdAt");

    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    public FarmerService(
            FarmerRepository farmerRepository,
            CooperativeRepository cooperativeRepository,
            FarmerAccessService farmerAccessService,
            AuditLogService auditLogService) {
        this.farmerRepository = farmerRepository;
        this.cooperativeRepository = cooperativeRepository;
        this.farmerAccessService = farmerAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FarmerResponse register(FarmerRegistrationRequest request) {
        AppUser currentUser = farmerAccessService.getCurrentUser();

        if (currentUser.getRole() != Role.UNION_REGISTRAR) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        Cooperative cooperative = cooperativeRepository.findById(request.cooperativeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cooperative with id " + request.cooperativeId()
                                + " was not found."));

        if (farmerRepository.existsByNationalId(request.nationalId())) {
            throw new ConflictException(
                    "A farmer with this national ID is already registered.");
        }

        String membershipNumber = String.format(
                "RCU-%06d", farmerRepository.nextMembershipSequence());

        Farmer farmer = new Farmer(
                membershipNumber,
                request.nationalId(),
                request.fullName().trim(),
                request.phoneNumber(),
                cooperative);

        Farmer saved = farmerRepository.save(farmer);
        return FarmerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public FarmerResponse findById(Long id) {
        Farmer farmer = farmerAccessService.requireFarmerAccess(id);
        return FarmerResponse.from(farmer);
    }

    @Transactional(readOnly = true)
    public FarmerResponse findMyProfile() {
        Farmer farmer = farmerAccessService.requireCurrentFarmer();
        return FarmerResponse.from(farmer);
    }

    @Transactional
    public FarmerResponse update(Long id, FarmerUpdateRequest request) {
        AppUser currentUser = farmerAccessService.getCurrentUser();

        if (currentUser.getRole() != Role.UNION_REGISTRAR) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        Farmer farmer = getOrThrow(id);
        farmer.updateDetails(
                request.fullName().trim(),
                request.phoneNumber());

        return FarmerResponse.from(farmer);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FarmerSummaryResponse> search(String name, String membershipNumber,
                                                      Long cooperativeId, FarmerStatus status,
                                                      int page, int size,
                                                      String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);

        // Field Officers and Agronomists are ALWAYS limited to their own district, whatever they send.
        AppUser user = farmerAccessService.getCurrentUser();
        Long districtScope = FarmerAccessService.isDistrictRestricted(user)
                ? user.getDistrict().getId()
                : null;

        Page<Farmer> result = farmerRepository.findAll(
                FarmerSpecifications.search(name, membershipNumber, cooperativeId, status, districtScope),
                pageable);
        return PagedResponse.from(result.map(FarmerSummaryResponse::from));
    }

    @Transactional
    public FarmerResponse changeStatus(
            Long id, FarmerStatusChangeRequest request) {

        AppUser currentUser = farmerAccessService.getCurrentUser();

        if (currentUser.getRole() != Role.UNION_REGISTRAR) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        Farmer farmer = getOrThrow(id);
        FarmerStatus previousStatus = farmer.getStatus();

        try {
            farmer.changeStatus(request.status());
        } catch (IllegalStateException ex) {
            // A valid request that breaks a business rule (for example reactivating a retired farmer): 422.
            throw new BusinessRuleException(ex.getMessage());
        }

        if (previousStatus != farmer.getStatus()) {
            // Part of this transaction: the audit row is saved only if the status change itself is saved.
            auditLogService.recordInCurrentTransaction(
                    AuditEventType.FARMER_STATUS_CHANGED,
                    true,
                    currentUser.getId(),
                    currentUser.getUsername(),
                    "FARMER",
                    farmer.getId(),
                    "Status changed from " + previousStatus
                            + " to " + farmer.getStatus(),
                    null
            );
        }

        return FarmerResponse.from(farmer);
    }

    private Farmer getOrThrow(Long id) {
        return farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + id + " was not found."));
    }
}