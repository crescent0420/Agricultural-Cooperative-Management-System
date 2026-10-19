package com.acms.service;
import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerStatusChangeRequest;
import com.acms.dto.FarmerSummaryResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.dto.PageResponse;
import com.acms.entity.Cooperative;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import com.acms.exception.BadRequestException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.FarmerRepository;
import com.acms.repository.FarmerSpecifications;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FarmerService {
    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;
    public FarmerService(FarmerRepository farmerRepository,
                         CooperativeRepository cooperativeRepository) {
        this.farmerRepository = farmerRepository;
        this.cooperativeRepository = cooperativeRepository;
    }
    @Transactional
    public FarmerResponse register(FarmerRegistrationRequest request) {
        Cooperative cooperative = cooperativeRepository.findById(request.cooperativeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cooperative with id " + request.cooperativeId() + " was not found."));
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
        return FarmerResponse.from(getOrThrow(id));
    }
    @Transactional
    public FarmerResponse update(Long id, FarmerUpdateRequest request) {
        Farmer farmer = getOrThrow(id);
        farmer.updateDetails(
                request.fullName().trim(),
                request.phoneNumber());
        return FarmerResponse.from(farmer);
    }
    @Transactional(readOnly = true)
    public PageResponse<FarmerSummaryResponse> search(
            String name,
            String membershipNumber,
            Long cooperativeId,
            FarmerStatus status,
            int page,
            int size,
            String sortBy,
            String direction) {
        if (page < 0) {
            throw new BadRequestException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100.");
        }
        String sortField = sortBy == null || sortBy.isBlank()
                ? "id" : sortBy.trim();
        if (!Set.of("id", "fullName", "membershipNumber", "status", "createdAt")
                .contains(sortField)) {
            throw new BadRequestException("Unsupported sort field: " + sortField);
        }
        String sortDirection = direction == null || direction.isBlank()
                ? "asc" : direction.trim().toLowerCase(Locale.ROOT);
        Sort.Direction sortOrder;
        try {
            sortOrder = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Sort direction must be 'asc' or 'desc'.");
        }
        Sort sort = Sort.by(sortOrder, sortField);
        if (!sortField.equals("id")) {
            sort = sort.and(Sort.by(sortOrder, "id"));
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Farmer> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (name != null && !name.isBlank()) {
            specification = specification.and(
                    FarmerSpecifications.hasName(name.trim()));
        }
        if (membershipNumber != null && !membershipNumber.isBlank()) {
            specification = specification.and(
                    FarmerSpecifications.hasMembershipNumber(membershipNumber.trim()));
        }
        if (cooperativeId != null) {
            specification = specification.and(
                    FarmerSpecifications.hasCooperativeId(cooperativeId));
        }
        if (status != null) {
            specification = specification.and(
                    FarmerSpecifications.hasStatus(status));
        }
        Page<FarmerSummaryResponse> result = farmerRepository
                .findAll(specification, pageable)
                .map(FarmerSummaryResponse::from);
        return PageResponse.from(result);
    }
    @Transactional
    public FarmerResponse changeStatus(
            Long id, FarmerStatusChangeRequest request) {
        Farmer farmer = getOrThrow(id);
        try {
            farmer.changeStatus(request.status());
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        return FarmerResponse.from(farmer);
    }
    private Farmer getOrThrow(Long id) {
        return farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + id + " was not found."));
    }
}
