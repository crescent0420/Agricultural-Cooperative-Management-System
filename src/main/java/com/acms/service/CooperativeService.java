package com.acms.service;

import com.acms.dto.CooperativeRenameRequest;
import com.acms.dto.CooperativeRequest;
import com.acms.dto.CooperativeResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.AppUser;
import com.acms.entity.Cooperative;
import com.acms.entity.District;
import com.acms.entity.Role;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.DistrictRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CooperativeService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "createdAt");

    private final CooperativeRepository cooperativeRepository;
    private final DistrictRepository districtRepository;
    private final FarmerAccessService farmerAccessService;

    public CooperativeService(
            CooperativeRepository cooperativeRepository,
            DistrictRepository districtRepository,
            FarmerAccessService farmerAccessService) {
        this.cooperativeRepository = cooperativeRepository;
        this.districtRepository = districtRepository;
        this.farmerAccessService = farmerAccessService;
    }

    @Transactional
    public CooperativeResponse create(CooperativeRequest request) {
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District with id " + request.districtId()
                                + " was not found."));

        String name = request.name().trim();

        if (cooperativeRepository.existsByDistrictIdAndNameIgnoreCase(
                district.getId(), name)) {
            throw new ConflictException(
                    "Cooperative '" + name
                            + "' already exists in district "
                            + district.getName() + ".");
        }

        Cooperative saved = cooperativeRepository.save(
                new Cooperative(name, district));

        return CooperativeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CooperativeResponse> findAll(
            Long districtId,
            int page,
            int size,
            String sortBy,
            String direction) {

        AppUser user = farmerAccessService.getCurrentUser();
        Role role = user.getRole();

        if (isDistrictRestrictedRole(role)) {
            if (user.getDistrict() == null) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }

            Long assignedDistrictId = user.getDistrict().getId();

            if (districtId != null
                    && !districtId.equals(assignedDistrictId)) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }

            districtId = assignedDistrictId;
        }

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        Page<Cooperative> result = (districtId == null)
                ? cooperativeRepository.findAll(pageable)
                : cooperativeRepository.findByDistrictId(
                        districtId, pageable);

        return PageResponse.from(
                result.map(CooperativeResponse::from));
    }

    @Transactional(readOnly = true)
    public CooperativeResponse findById(Long id) {
        AppUser user = farmerAccessService.getCurrentUser();
        Cooperative cooperative = getOrThrow(id);

        if (isDistrictRestrictedRole(user.getRole())) {
            if (user.getDistrict() == null
                    || cooperative.getDistrict() == null
                    || !user.getDistrict().getId().equals(
                            cooperative.getDistrict().getId())) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }
        }

        return CooperativeResponse.from(cooperative);
    }

    @Transactional
    public CooperativeResponse rename(
            Long id, CooperativeRenameRequest request) {
        Cooperative cooperative = getOrThrow(id);
        String name = request.name().trim();

        Long districtId = cooperative.getDistrict().getId();

        if (cooperativeRepository.existsByDistrictIdAndNameIgnoreCaseAndIdNot(
                districtId, name, id)) {
            throw new ConflictException(
                    "Cooperative '" + name
                            + "' already exists in this district.");
        }

        cooperative.setName(name);

        return CooperativeResponse.from(cooperative);
    }

    private boolean isDistrictRestrictedRole(Role role) {
        return role == Role.FIELD_OFFICER
                || role == Role.DISTRICT_AGRONOMIST;
    }

    private Cooperative getOrThrow(Long id) {
        return cooperativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cooperative with id " + id + " was not found."));
    }
}