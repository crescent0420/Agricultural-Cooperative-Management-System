
package com.acms.service;

import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.entity.District;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.DistrictRepository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistrictService {

    private final DistrictRepository districtRepository;

    public DistrictService(DistrictRepository districtRepository) {
        this.districtRepository = districtRepository;
    }

    @Transactional
    public DistrictResponse create(DistrictRequest request) {
        String name = request.name().trim();

        if (districtRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                "District '" + name + "' already exists."
            );
        }

        District saved = districtRepository.save(
            new District(name)
        );

        return DistrictResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> findAll() {
        return districtRepository.findAll(Sort.by("name"))
                .stream()
                .map(DistrictResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DistrictResponse findById(Long id) {
        return DistrictResponse.from(getOrThrow(id));
    }

    @Transactional
    public DistrictResponse update(
            Long id,
            DistrictRequest request) {

        District district = getOrThrow(id);
        String name = request.name().trim();

        if (districtRepository
                .existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException(
                "District '" + name + "' already exists."
            );
        }

        district.setName(name);

        return DistrictResponse.from(district);
    }

    private District getOrThrow(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "District with id " + id
                        + " was not found."
                    )
                );
    }
}
