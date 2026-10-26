package com.acms.controller;

import com.acms.dto.ChangePasswordRequest;
import com.acms.dto.ChangeRoleRequest;
import com.acms.dto.CreateUserRequest;
import com.acms.dto.MessageResponse;
import com.acms.dto.PageResponse;
import com.acms.dto.SetEnabledRequest;
import com.acms.dto.UserResponse;
import com.acms.service.UserService;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        return userService.createUser(request, currentUserId(authentication));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return userService.findAll(page, size, sortBy, direction);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request,
                                   Authentication authentication) {
        return userService.changeRole(id, request, currentUserId(authentication));
    }

    @PatchMapping("/users/{id}/enabled")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody SetEnabledRequest request,
                                   Authentication authentication) {
        return userService.setEnabled(id, request.enabled(), currentUserId(authentication));
    }

    /** Emails the user a new one-time password (and a new verification link if still unverified). */
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public MessageResponse resetPassword(@PathVariable Long id, Authentication authentication) {
        return userService.resetPassword(id, currentUserId(authentication));
    }

    @PostMapping("/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        userService.changePassword(currentUserId(authentication), request);
    }

    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
