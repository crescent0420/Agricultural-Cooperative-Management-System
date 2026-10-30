package com.acms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.enums.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;

/** Self-registration, the emailed one-time password, email verification and the forced password change. */
class RegistrationFlowTest extends IntegrationTestBase {

    @Test
    @DisplayName("Full journey: register, verify the email link, forced password change, then normal access")
    void fullRegistrationJourney() throws Exception {
        String username = unique("journey");
        String email = username + "@example.test";

        // 1. Register: the answer never contains a password.
        String registerAnswer = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "email", email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(registerAnswer).doesNotContainIgnoringCase("password\":");

        // 2. The (captured) email holds the one-time password and the verification link.
        SimpleMailMessage welcome = lastEmailTo(email);
        String oneTimePassword = oneTimePassword(welcome);
        String token = verificationToken(welcome);
        assertThat(oneTimePassword).hasSize(16);

        // 3. Not verified yet: even the correct password is refused.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", oneTimePassword)))
                .andExpect(status().is(422));

        // 4. Fake link fails; real link works once; reusing it fails.
        mockMvc.perform(get("/api/auth/verify-email").param("token", "fake-token-123"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/auth/verify-email").param("token", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/verify-email").param("token", token))
                .andExpect(status().isBadRequest());

        // 5. Login works, but the token is limited until the password is changed.
        String loginAnswer = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", oneTimePassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String limitedToken = JsonPath.read(loginAnswer, "$.data.accessToken");

        mockMvc.perform(get("/api/districts").header("Authorization", bearer(limitedToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(limitedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("FARMER"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true));

        // 6. Wrong current password: 400. Correct change: 204, and the limited token dies.
        mockMvc.perform(post("/api/auth/change-password").header("Authorization", bearer(limitedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", "wrong-password-123", "newPassword", "My-Own-Password-2026")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/change-password").header("Authorization", bearer(limitedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", oneTimePassword, "newPassword", "My-Own-Password-2026")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(limitedToken)))
                .andExpect(status().isUnauthorized());

        // 7. Logging in with the new password gives full access for the role.
        String fullToken = login(username, "My-Own-Password-2026");
        mockMvc.perform(get("/api/districts").header("Authorization", bearer(fullToken)))
                .andExpect(status().isOk());
        // Not linked to a farmer record yet, so there is no farmer profile.
        mockMvc.perform(get("/api/me/profile").header("Authorization", bearer(fullToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Duplicate username or email is refused (409); an invalid email is refused (400)")
    void registrationValidation() throws Exception {
        String username = unique("dupe");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "email", username + "@example.test")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "email", "other." + username + "@example.test")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", unique("dupe2"), "email", username + "@example.test")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", unique("bademail"), "email", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin-created accounts get emailed credentials; only admins can reset passwords")
    void adminCreatedAccountsAndResets() throws Exception {
        String adminName = unique("admin");
        createUser(adminName, Role.SYSTEM_ADMIN, null, null);
        String adminToken = login(adminName, PASSWORD);

        String newName = unique("created");
        String newEmail = newName + "@example.test";
        String created = mockMvc.perform(post("/api/users").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", newName, "email", newEmail, "role", "UNION_REGISTRAR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        Number newId = JsonPath.read(created, "$.data.id");
        assertThat(oneTimePassword(lastEmailTo(newEmail))).hasSize(16);

        // Reset: a second email, with a new one-time password and (still unverified) a new link.
        mockMvc.perform(post("/api/users/" + newId + "/reset-password").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        assertThat(lastEmailTo(newEmail).getText()).contains("verify-email?token=");

        // An account without an email address cannot receive a reset.
        AppUser noEmail = appUserRepository.save(withVerifiedPassword(
                new AppUser(unique("noemail"), null, "x", Role.FINANCE_OFFICER)));
        mockMvc.perform(post("/api/users/" + noEmail.getId() + "/reset-password").header("Authorization", bearer(adminToken)))
                .andExpect(status().is(422));

        // A registrar is not allowed to reset anybody's password.
        String registrarName = unique("registrar");
        createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        mockMvc.perform(post("/api/users/" + newId + "/reset-password")
                        .header("Authorization", bearer(login(registrarName, PASSWORD))))
                .andExpect(status().isForbidden());
    }

    private AppUser withVerifiedPassword(AppUser user) {
        user.markEmailVerified();
        user.changePasswordHash(passwordEncoder.encode(PASSWORD));
        return user;
    }
}
