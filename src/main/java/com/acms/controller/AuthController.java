package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.ChangePasswordRequest;
import com.acms.dto.CurrentUserResponse;
import com.acms.dto.LoginRequest;
import com.acms.dto.LoginResponse;
import com.acms.dto.RegisterRequest;
import com.acms.service.AccountOnboardingService;
import com.acms.service.AuthenticationService;
import com.acms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = ApiTags.AUTHENTICATION)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthenticationService authenticationService;
    private final AccountOnboardingService onboardingService;
    private final UserService userService;

    @Operation(
            summary = "Log in and get an access token",
            description = """
                    Log in; returns the access token.

                    - **Access:** public, no token needed
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 401 wrong credentials, 422 email not verified
                    """)
    @SecurityRequirements
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ok("Logged in", authenticationService.login(request));
    }

    /** Public sign-up: the first account ever is System Administrator, later ones are FARMER. */
    @Operation(
            summary = "Register an account",
            description = """
                    Self-registration; the very first user becomes System Admin.

                    - **Access:** public, no token needed
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 duplicate username/email
                    """)
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return message(onboardingService.register(request));
    }

    /** The link in the account email. GET, because the user clicks it in their mail program. */
    @Operation(
            summary = "Verify an email address",
            description = """
                    Email verification link (from the email).

                    - **Access:** public, no token needed
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400 invalid/expired token
                    """)
    @SecurityRequirements
    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        return message(onboardingService.verifyEmail(token));
    }

    /** Who does this token belong to? Works even while the password must still be changed. */
    @Operation(
            summary = "Current user, role and permissions",
            description = """
                    Who am I: account, role, district, permissions.

                    - **Access:** any logged-in user
                    - **Success:** 200
                    """)
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        return ok(userService.getCurrentUser(currentUserId(authentication)));
    }

    /**
     * 204 No Content: success without a body (so no envelope either).
     * The current token stops working afterwards (its token version is increased): log in again.
     */
    @Operation(
            summary = "Change your password",
            description = """
                    Replace the (one-time) password; the current token stops working, so log in again.

                    - **Access:** any logged-in user
                    - **Success:** 204
                    - **Errors** (besides 401/403): 400 wrong current password / weak or unchanged password
                    """)
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        userService.changePassword(currentUserId(authentication), request);
    }
}
