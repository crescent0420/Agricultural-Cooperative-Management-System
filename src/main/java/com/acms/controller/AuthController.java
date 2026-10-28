package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.CurrentUserResponse;
import com.acms.dto.LoginRequest;
import com.acms.dto.LoginResponse;
import com.acms.dto.RegisterRequest;
import com.acms.service.AccountOnboardingService;
import com.acms.service.AuthenticationService;
import com.acms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthenticationService authenticationService;
    private final AccountOnboardingService onboardingService;
    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ok("Logged in", authenticationService.login(request));
    }

    /** Public sign-up: the first account ever is System Administrator, later ones are FARMER. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return message(onboardingService.register(request));
    }

    /** The link in the account email. GET, because the user clicks it in their mail program. */
    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        return message(onboardingService.verifyEmail(token));
    }

    /** Who does this token belong to? Works even while the password must still be changed. */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        return ok(userService.getCurrentUser(currentUserId(authentication)));
    }
}
