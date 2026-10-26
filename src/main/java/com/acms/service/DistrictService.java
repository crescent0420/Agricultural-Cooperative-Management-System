package com.acms.service;

import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.District;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.DistrictRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DistrictService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "createdAt");

    private final DistrictRepository districtRepository;

    @Transactional
    public DistrictResponse create(DistrictRequest request) {
        String name = request.name().trim();

        if (districtRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                    "District '" + name + "' already exists.");
        }

        return DistrictResponse.from(
                districtRepository.save(new District(name)));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistrictResponse> findAll(
            int page, int size, String sortBy, String direction) {

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        return PageResponse.from(
                districtRepository.findAll(pageable)
                        .map(DistrictResponse::from));
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
            throw new ConflictException(
                    "District '" + name + "' already exists.");
        }

        district.setName(name);
        return DistrictResponse.from(district);
    }

    private District getOrThrow(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District with id " + id + " was not found."));
    }
}