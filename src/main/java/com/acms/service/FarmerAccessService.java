package com.acms.service;

import com.acms.common.BaseService;
import com.acms.entity.AppUser;
import com.acms.entity.Farmer;
import com.acms.enums.Role;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.AppUserRepository;
import com.acms.repository.FarmerRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FarmerAccessService extends BaseService {

    private final AppUserRepository appUserRepository;
    private final FarmerRepository farmerRepository;

    public FarmerAccessService(
            AppUserRepository appUserRepository,
            FarmerRepository farmerRepository) {
        this.appUserRepository = appUserRepository;
        this.farmerRepository = farmerRepository;
    }
        /** Field Officers and District Agronomists may only work with their own district. */
    public static boolean isDistrictRestricted(AppUser user) {
        return user.getRole() == Role.FIELD_OFFICER
                || user.getRole() == Role.DISTRICT_AGRONOMIST;
    }

    public AppUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        final Long userId;

        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        return appUserRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have permission to perform this action."));
    }

    public Farmer requireCurrentFarmer() {
        AppUser user = getCurrentUser();

        if (user.getRole() != Role.FARMER || user.getFarmer() == null) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        return farmerRepository.findById(user.getFarmer().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The farmer profile was not found."));
    }

    public Farmer requireFarmerAccess(Long farmerId) {
        AppUser user = getCurrentUser();
        Role role = user.getRole();

        // Check ownership before looking up the requested farmer ID.
        if (role == Role.FARMER) {
            if (user.getFarmer() == null
                    || !user.getFarmer().getId().equals(farmerId)) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }
        }

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + farmerId + " was not found."));

        if (role == Role.FIELD_OFFICER
                || role == Role.DISTRICT_AGRONOMIST) {
            if (user.getDistrict() == null
                    || farmer.getCooperative().getDistrict() == null
                    || !user.getDistrict().getId().equals(
                            farmer.getCooperative().getDistrict().getId())) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }
        }

        return farmer;
    }
}
