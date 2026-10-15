
package com.acms.service;

import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.entity.Cooperative;
import com.acms.entity.Farmer;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.FarmerRepository;
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

    private Farmer getOrThrow(Long id) {
        return farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + id + " was not found."));
    }
}
