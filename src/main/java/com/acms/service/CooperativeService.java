
package com.acms.service;

import com.acms.dto.CooperativeRenameRequest;
import com.acms.dto.CooperativeRequest;
import com.acms.dto.CooperativeResponse;
import com.acms.entity.Cooperative;
import com.acms.entity.District;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.DistrictRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CooperativeService {

    private final CooperativeRepository cooperativeRepository;
    private final DistrictRepository districtRepository;

    public CooperativeService(
            CooperativeRepository cooperativeRepository,
            DistrictRepository districtRepository) {
        this.cooperativeRepository = cooperativeRepository;
        this.districtRepository = districtRepository;
    }

    @Transactional
    public CooperativeResponse create(CooperativeRequest request) {
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District with id " + request.districtId() + " was not found."));

        String name = request.name().trim();

        if (cooperativeRepository.existsByDistrictIdAndNameIgnoreCase(
                district.getId(), name)) {
            throw new ConflictException(
                    "Cooperative '" + name + "' already exists in district "
                            + district.getName() + ".");
        }

        Cooperative saved = cooperativeRepository.save(
                new Cooperative(name, district));

        return CooperativeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CooperativeResponse> findAll(Long districtId) {
        Sort sort = Sort.by("name");

        List<Cooperative> cooperatives = (districtId == null)
                ? cooperativeRepository.findAll(sort)
                : cooperativeRepository.findByDistrictId(districtId, sort);

        return cooperatives.stream()
                .map(CooperativeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CooperativeResponse findById(Long id) {
        return CooperativeResponse.from(getOrThrow(id));
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

    private Cooperative getOrThrow(Long id) {
        return cooperativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cooperative with id " + id + " was not found."));
    }
}
