package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.ChangeRoleRequest;
import com.acms.dto.CreateUserRequest;
import com.acms.dto.SetEnabledRequest;
import com.acms.dto.UserResponse;
import com.acms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** @PreAuthorize is a second check, after the URL rule in SecurityConfig (defence in depth). */
@Tag(name = ApiTags.USER_ADMINISTRATION)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @Operation(
            summary = "Create a staff or farmer account",
            description = """
                    Create a staff or farmer account; emails a one-time password.

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400 district/farmer does not fit the role, 409 duplicate username/email or farmer already has an account
                    """)
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                Authentication authentication) {
        return created(userService.createUser(request, currentUserId(authentication)));
    }

    @Operation(
            summary = "List user accounts",
            description = """
                    List user accounts.

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, username, role, createdAt; default username), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    """)
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<PagedResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ok(userService.findAll(page, size, sortBy, direction));
    }

    @Operation(
            summary = "One user account",
            description = """
                    One user account.

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ok(userService.getUser(id));
    }

    @Operation(
            summary = "Change a role",
            description = """
                    Change a role (and its district or farmer).

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 district/farmer does not fit the role, 409 own role / farmer already has an account
                    """)
    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<UserResponse> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                                Authentication authentication) {
        return ok(userService.changeRole(id, request, currentUserId(authentication)));
    }

    @Operation(
            summary = "Enable or disable an account",
            description = """
                    Enable or disable an account.

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 own account
                    """)
    @PatchMapping("/users/{id}/enabled")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<UserResponse> setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request,
                                                Authentication authentication) {
        return ok(userService.setEnabled(id, request.enabled(), currentUserId(authentication)));
    }

    /** Emails the user a new one-time password (and a new verification link if still unverified). */
    @Operation(
            summary = "Email a new one-time password",
            description = """
                    Email a new one-time password.

                    - **Permission:** `USER_MANAGE` (Admin)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 422 no email address / account disabled
                    """)
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, Authentication authentication) {
        return message(userService.resetPassword(id, currentUserId(authentication)));
    }
}
