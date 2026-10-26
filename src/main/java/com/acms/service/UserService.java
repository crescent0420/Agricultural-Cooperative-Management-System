package com.acms.service;

import com.acms.dto.ChangePasswordRequest;
import com.acms.dto.ChangeRoleRequest;
import com.acms.dto.CreateUserRequest;
import com.acms.dto.PageResponse;
import com.acms.dto.SetEnabledRequest;
import com.acms.dto.UserResponse;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.entity.District;
import com.acms.entity.Farmer;
import com.acms.entity.Role;
import com.acms.exception.BadRequestException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.AppUserRepository;
import com.acms.repository.DistrictRepository;
import com.acms.repository.FarmerRepository;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "username", "role", "createdAt");

    private final AppUserRepository appUserRepository;
    private final DistrictRepository districtRepository;
    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(
            AppUserRepository appUserRepository,
            DistrictRepository districtRepository,
            FarmerRepository farmerRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.districtRepository = districtRepository;
        this.farmerRepository = farmerRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request,
            Long actorUserId) {

        String username = request.username();

        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already in use.");
        }

        validatePassword(username, request.password());

        District district = null;
        Farmer farmer = null;

        if (request.role() == Role.FIELD_OFFICER
                || request.role() == Role.DISTRICT_AGRONOMIST) {

            if (request.districtId() == null
                    || request.farmerId() != null) {
                throw new BadRequestException(
                        "This role requires a district and no farmer assignment.");
            }

            district = districtRepository.findById(request.districtId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("District not found."));

        } else if (request.role() == Role.FARMER) {

            if (request.farmerId() == null
                    || request.districtId() != null) {
                throw new BadRequestException(
                        "FARMER role requires a farmer assignment and no district assignment.");
            }

            farmer = farmerRepository.findById(request.farmerId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Farmer not found."));

            if (appUserRepository.existsByFarmerId(farmer.getId())) {
                throw new ConflictException(
                        "This farmer already has a login account.");
            }

        } else {
            if (request.districtId() != null
                    || request.farmerId() != null) {
                throw new BadRequestException(
                        "This role must not have a district or farmer assignment.");
            }
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                request.role());

        if (district != null) {
            user.assignDistrict(district);
        } else if (farmer != null) {
            user.assignFarmer(farmer);
        } else {
            user.clearScope();
        }

        try {
            AppUser savedUser = appUserRepository.save(user);

            auditLogService.record(
                    AuditEventType.USER_CREATED,
                    true,
                    actorUserId,
                    null,
                    "USER",
                    savedUser.getId(),
                    "User account created with role " + savedUser.getRole(),
                    null);

            return toResponse(savedUser);

        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "The user could not be created because of a data conflict.");
        }
    }

    @Transactional
    public UserResponse changeRole(
            Long targetUserId,
            ChangeRoleRequest request,
            Long actorUserId) {

        if (targetUserId.equals(actorUserId)) {
            throw new ConflictException(
                    "You cannot change your own role.");
        }

        AppUser user = findUser(targetUserId);

        District district = null;
        Farmer farmer = null;

        if (request.role() == Role.FIELD_OFFICER
                || request.role() == Role.DISTRICT_AGRONOMIST) {

            if (request.districtId() == null
                    || request.farmerId() != null) {
                throw new BadRequestException(
                        "This role requires a district and no farmer assignment.");
            }

            district = districtRepository.findById(request.districtId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("District not found."));

        } else if (request.role() == Role.FARMER) {

            if (request.farmerId() == null
                    || request.districtId() != null) {
                throw new BadRequestException(
                        "FARMER role requires a farmer assignment and no district assignment.");
            }

            farmer = farmerRepository.findById(request.farmerId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Farmer not found."));

            appUserRepository.findByFarmerId(farmer.getId())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException(
                                "This farmer already has a login account.");
                    });

        } else if (request.districtId() != null
                || request.farmerId() != null) {
            throw new BadRequestException(
                    "This role must not have a district or farmer assignment.");
        }

        Role previousRole = user.getRole();
        user.changeRole(request.role());

        if (district != null) {
            user.assignDistrict(district);
        } else if (farmer != null) {
            user.assignFarmer(farmer);
        } else {
            user.clearScope();
        }

        AppUser savedUser = appUserRepository.save(user);

        auditLogService.record(
                AuditEventType.USER_ROLE_CHANGED,
                true,
                actorUserId,
                null,
                "USER",
                savedUser.getId(),
                "Role changed from " + previousRole
                        + " to " + savedUser.getRole(),
                null);

        return toResponse(savedUser);
    }

    @Transactional
    public UserResponse setEnabled(
            Long targetUserId,
            boolean enabled,
            Long actorUserId) {

        if (!enabled && targetUserId.equals(actorUserId)) {
            throw new ConflictException(
                    "You cannot disable your own account.");
        }

        AppUser user = findUser(targetUserId);
        user.setEnabled(enabled);

        AppUser savedUser = appUserRepository.save(user);

        auditLogService.record(
                enabled ? AuditEventType.USER_ENABLED
                        : AuditEventType.USER_DISABLED,
                true,
                actorUserId,
                null,
                "USER",
                savedUser.getId(),
                enabled ? "User account enabled" : "User account disabled",
                null);

        return toResponse(savedUser);
    }

    @Transactional
    public void changePassword(
            Long actorUserId,
            ChangePasswordRequest request) {

        AppUser user = findUser(actorUserId);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash())) {

            auditLogService.record(
                    AuditEventType.PASSWORD_CHANGE_FAILED,
                    false,
                    actorUserId,
                    user.getUsername(),
                    "USER",
                    actorUserId,
                    "Current password verification failed",
                    null);

            throw new BadCredentialsException(
                    "Current password is incorrect.");
        }

        validatePassword(user.getUsername(), request.newPassword());

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash())) {
            throw new BadRequestException(
                    "New password must differ from the current password.");
        }

        user.changePasswordHash(
                passwordEncoder.encode(request.newPassword()));

        appUserRepository.save(user);

        auditLogService.record(
                AuditEventType.PASSWORD_CHANGED,
                true,
                actorUserId,
                user.getUsername(),
                "USER",
                actorUserId,
                "Password changed successfully",
                null);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        return PageResponse.from(
                appUserRepository.findAll(pageable)
                        .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));
    }

    private void validatePassword(String username, String password) {
        if (password == null
                || password.length() < 12
                || password.length() > 128
                || password.isBlank()) {
            throw new BadRequestException(
                    "Password must contain 12 to 128 characters.");
        }

        if (password.toLowerCase()
                .contains(username.toLowerCase())) {
            throw new BadRequestException(
                    "Password must not contain the username.");
        }
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.getDistrict() == null
                        ? null : user.getDistrict().getId(),
                user.getFarmer() == null
                        ? null : user.getFarmer().getId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}