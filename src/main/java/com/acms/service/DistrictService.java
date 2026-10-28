package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.entity.District;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.DistrictRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Districts are seeded from the official list (see the provinces migration), so there is no "create".
 * The Registrar can only correct a district's name.
 */
@Service
@RequiredArgsConstructor
public class DistrictService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "createdAt");

    private final DistrictRepository districtRepository;

    @Transactional(readOnly = true)
    public PagedResponse<DistrictResponse> findAll(Long provinceId, int page, int size,
                                                  String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        Page<District> districts = (provinceId == null)
                ? districtRepository.findAll(pageable)
                : districtRepository.findByProvinceId(provinceId, pageable);
        return PagedResponse.from(districts.map(DistrictResponse::from));
    }

    @Transactional(readOnly = true)
    public DistrictResponse findById(Long id) {
        return DistrictResponse.from(getOrThrow(id));
    }

    @Transactional
    public DistrictResponse update(Long id, DistrictRequest request) {
        District district = getOrThrow(id);
        String name = request.name().trim();
        if (districtRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("District '" + name + "' already exists.");
        }
        district.setName(name);
        return DistrictResponse.from(district);
    }

    private District getOrThrow(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District with id " + id + " was not found."));
    }
}
