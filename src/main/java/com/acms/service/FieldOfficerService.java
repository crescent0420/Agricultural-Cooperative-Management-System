package com.acms.service;

import com.acms.common.BaseService;
import com.acms.dto.FieldOfficerOptionResponse;
import com.acms.entity.AppUser;
import com.acms.enums.Role;
import com.acms.repository.AppUserRepository;
import com.acms.repository.DistrictRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Field Officers a Union Registrar may assign to a programme offering.
 * Lists ENABLED officers only: ProgrammeOfferingService refuses a disabled one (422),
 * so the dropdown never offers a choice that the save would reject.
 */
@Service
@RequiredArgsConstructor
public class FieldOfficerService extends BaseService {

    private final AppUserRepository appUserRepository;
    private final DistrictRepository districtRepository;

    /** All enabled Field Officers, or only those of one district; sorted by username. */
    @Transactional(readOnly = true)
    public List<FieldOfficerOptionResponse> findAssignable(Long districtId) {
        List<AppUser> officers;
        if (districtId == null) {
            officers = appUserRepository.findByRoleAndEnabledTrueOrderByUsernameAsc(Role.FIELD_OFFICER);
        } else {
            if (!districtRepository.existsById(districtId)) {
                throw notFound("District", districtId);    // same 404 wording as DistrictService
            }
            officers = appUserRepository.findByRoleAndEnabledTrueAndDistrictIdOrderByUsernameAsc(
                    Role.FIELD_OFFICER, districtId);
        }
        return officers.stream().map(FieldOfficerOptionResponse::from).toList();
    }
}