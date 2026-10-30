package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.MessageResponse;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.ChangePasswordRequest;
import com.acms.dto.ChangeRoleRequest;
import com.acms.dto.CreateUserRequest;
import com.acms.dto.CurrentUserResponse;
import com.acms.dto.UserResponse;
import com.acms.entity.AppUser;
import com.acms.entity.District;
import com.acms.entity.Farmer;
import com.acms.enums.AuditEventType;
import com.acms.enums.Role;
import com.acms.exception.BadRequestException;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.AppUserRepository;
import com.acms.repository.DistrictRepository;
import com.acms.repository.FarmerRepository;
import com.acms.security.PermissionMatrix;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "role", "createdAt");

    private final AppUserRepository appUserRepository;
    private final DistrictRepository districtRepository;
    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AccountOnboardingService onboardingService;

    /** A role's district or farmer link. FARMER accounts may have no farmer yet (linked later). */
    private record Scope(District district, Farmer farmer) {
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, Long actorUserId) {
        String username = request.username();
        String email = AccountOnboardingService.normaliseEmail(request.email());
        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already in use.");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email address is already in use.");
        }

        Scope scope = resolveScope(request.role(), request.districtId(), request.farmerId(), null);
        AppUser saved = onboardingService.createAccount(
                username, email, request.role(), scope.district(), scope.farmer(), false);

        AppUser actor = findUser(actorUserId);
        auditLogService.recordInCurrentTransaction(AuditEventType.USER_CREATED, true,
                actor.getId(), actor.getUsername(), "USER", saved.getId(),
                "Account created with role " + saved.getRole() + "; one-time password emailed", null);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse changeRole(Long targetUserId, ChangeRoleRequest request, Long actorUserId) {
        if (targetUserId.equals(actorUserId)) {
            throw new ConflictException("You cannot change your own role.");
        }
        AppUser user = findUser(targetUserId);
        Scope scope = resolveScope(request.role(), request.districtId(), request.farmerId(), user.getId());

        Role previousRole = user.getRole();
        user.changeRole(request.role());
        applyScope(user, scope);
        AppUser saved = appUserRepository.saveAndFlush(user);   // flush now so a constraint error appears here

        AppUser actor = findUser(actorUserId);
        auditLogService.recordInCurrentTransaction(AuditEventType.USER_ROLE_CHANGED, true,
                actor.getId(), actor.getUsername(), "USER", saved.getId(),
                "Role changed from " + previousRole + " to " + saved.getRole(), null);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse setEnabled(Long targetUserId, boolean enabled, Long actorUserId) {
        if (!enabled && targetUserId.equals(actorUserId)) {
            throw new ConflictException("You cannot disable your own account.");
        }
        AppUser user = findUser(targetUserId);
        user.setEnabled(enabled);
        AppUser saved = appUserRepository.save(user);

        AppUser actor = findUser(actorUserId);
        auditLogService.recordInCurrentTransaction(
                enabled ? AuditEventType.USER_ENABLED : AuditEventType.USER_DISABLED, true,
                actor.getId(), actor.getUsername(), "USER", saved.getId(),
                enabled ? "User account enabled" : "User account disabled", null);
        return toResponse(saved);
    }

    /** Admin: email the user a new one-time password (for a lost email or a forgotten password). */
    @Transactional
    public MessageResponse resetPassword(Long targetUserId, Long actorUserId) {
        AppUser user = findUser(targetUserId);
        if (user.getEmail() == null) {
            throw new BusinessRuleException(
                    "This account has no email address, so a one-time password cannot be sent.");
        }
        if (!user.isEnabled()) {
            throw new BusinessRuleException("Enable the account before resetting its password.");
        }
        onboardingService.reissueCredentials(user);

        AppUser actor = findUser(actorUserId);
        auditLogService.recordInCurrentTransaction(AuditEventType.PASSWORD_RESET, true,
                actor.getId(), actor.getUsername(), "USER", user.getId(),
                "New one-time password emailed", null);
        return new MessageResponse("A new one-time password has been emailed to the user.");
    }

    @Transactional
    public void changePassword(Long actorUserId, ChangePasswordRequest request) {
        AppUser user = findUser(actorUserId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditLogService.record(AuditEventType.PASSWORD_CHANGE_FAILED, false,
                    user.getId(), user.getUsername(), "USER", user.getId(),
                    "Current password verification failed", null);
            // 400, not 401: the user IS logged in; only the value they typed is wrong.
            throw new BadRequestException("Current password is incorrect.");
        }

        validatePassword(user.getUsername(), request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current password.");
        }

        // Also clears "must change password" and increases the token version: the current token stops working.
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        auditLogService.recordInCurrentTransaction(AuditEventType.PASSWORD_CHANGED, true,
                user.getId(), user.getUsername(), "USER", user.getId(),
                "Password changed successfully", null);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        AppUser user = findUser(userId);
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEmailVerified(),
                user.isMustChangePassword(),
                user.getDistrict() == null ? null : user.getDistrict().getId(),
                user.getFarmer() == null ? null : user.getFarmer().getId(),
                user.isMustChangePassword() ? List.of() : PermissionMatrix.permissionsOf(user.getRole()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        return PagedResponse.from(appUserRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    /**
     * Field Officers and District Agronomists need a district; a FARMER may be linked to one farmer
     * record (or none yet); every other role has neither. The database enforces the same rule.
     */
    private Scope resolveScope(Role role, Long districtId, Long farmerId, Long currentUserId) {
        return switch (role) {
            case FIELD_OFFICER, DISTRICT_AGRONOMIST -> {
                if (districtId == null || farmerId != null) {
                    throw new BadRequestException("This role requires a district and no farmer assignment.");
                }
                District district = districtRepository.findById(districtId)
                        .orElseThrow(() -> new ResourceNotFoundException("District not found."));
                yield new Scope(district, null);
            }
            case FARMER -> {
                if (districtId != null) {
                    throw new BadRequestException("FARMER role must not have a district assignment.");
                }
                if (farmerId == null) {
                    yield new Scope(null, null);   // linked to a farmer record later by the administrator
                }
                Farmer farmer = farmerRepository.findById(farmerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer not found."));
                appUserRepository.findByFarmerId(farmer.getId())
                        .filter(existing -> !existing.getId().equals(currentUserId))
                        .ifPresent(existing -> {
                            throw new ConflictException("This farmer already has a login account.");
                        });
                yield new Scope(null, farmer);
            }
            default -> {
                if (districtId != null || farmerId != null) {
                    throw new BadRequestException("This role must not have a district or farmer assignment.");
                }
                yield new Scope(null, null);
            }
        };
    }

    private void applyScope(AppUser user, Scope scope) {
        if (scope.district() != null) {
            user.assignDistrict(scope.district());
        } else if (scope.farmer() != null) {
            user.assignFarmer(scope.farmer());
        } else {
            user.clearScope();
        }
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void validatePassword(String username, String password) {
        if (password == null || password.isBlank() || password.length() < 12 || password.length() > 128) {
            throw new BadRequestException("Password must contain 12 to 128 characters.");
        }
        if (password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Password must not contain the username.");
        }
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isMustChangePassword(),
                user.getDistrict() == null ? null : user.getDistrict().getId(),
                user.getFarmer() == null ? null : user.getFarmer().getId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
