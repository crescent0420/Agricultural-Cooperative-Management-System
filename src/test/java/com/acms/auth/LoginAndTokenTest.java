package com.acms.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.config.JwtProperties;
import com.acms.entity.AppUser;
import com.acms.enums.Role;
import com.acms.support.IntegrationTestBase;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Authentication: how credentials are checked and how every later request is tied to a user. */
class LoginAndTokenTest extends IntegrationTestBase {

    private static final String GENERIC_LOGIN_ERROR = "Invalid username or password.";

    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private JwtProperties jwtProperties;

    // ---------- login ----------

    @Test
    @DisplayName("Correct username and password give a Bearer token")
    void correctCredentialsGiveAToken() throws Exception {
        String username = unique("login.ok");
        createUser(username, Role.UNION_REGISTRAR, null, null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    @DisplayName("Wrong password and unknown username get the SAME 401 answer (no username guessing)")
    void wrongPasswordAndUnknownUserLookTheSame() throws Exception {
        String username = unique("login.wrong");
        createUser(username, Role.UNION_REGISTRAR, null, null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", "not-the-password-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_LOGIN_ERROR));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", unique("nobody"), "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_LOGIN_ERROR));
    }

    @Test
    @DisplayName("A correct password is still refused until the email address is verified (422)")
    void unverifiedEmailCannotLogIn() throws Exception {
        String username = unique("login.unverified");
        AppUser user = new AppUser(username, username + "@example.test", passwordEncoder.encode(PASSWORD), Role.FARMER);
        user.changePasswordHash(passwordEncoder.encode(PASSWORD));   // known password, but NOT verified
        appUserRepository.save(user);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", PASSWORD)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    // ---------- every later request ----------

    @Test
    @DisplayName("A protected endpoint without a token answers 401")
    void noTokenGives401() throws Exception {
        mockMvc.perform(get("/api/districts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/auth/me shows exactly whose token it is")
    void meIdentifiesTheTokenOwner() throws Exception {
        String username = unique("me");
        AppUser user = createUser(username, Role.FINANCE_OFFICER, null, null);
        String token = login(username, PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(Math.toIntExact(user.getId())))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("FINANCE_OFFICER"));
    }

    @Test
    @DisplayName("A token whose signature was tampered with is rejected (401)")
    void forgedTokenIsRejected() throws Exception {
        String username = unique("forged");
        createUser(username, Role.UNION_REGISTRAR, null, null);
        String token = login(username, PASSWORD);

        // Change one character in the middle of the signature (the third part of the token).
        int position = token.lastIndexOf('.') + 10;
        char replacement = token.charAt(position) == 'A' ? 'B' : 'A';
        String forged = token.substring(0, position) + replacement + token.substring(position + 1);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired token is rejected (401), even though its signature is valid")
    void expiredTokenIsRejected() throws Exception {
        String username = unique("expired");
        AppUser user = createUser(username, Role.UNION_REGISTRAR, null, null);

        // Signed with the real key, but it expired an hour ago (beyond the 60-second clock tolerance).
        Instant twoHoursAgo = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .issuedAt(twoHoursAgo)
                .expiresAt(twoHoursAgo.plusSeconds(3600))
                .claim("token_version", user.getTokenVersion())
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("After a password change, every older token stops working (token_version)")
    void oldTokenDiesAfterPasswordChange() throws Exception {
        String username = unique("pwchange");
        createUser(username, Role.UNION_REGISTRAR, null, null);
        String oldToken = login(username, PASSWORD);

        mockMvc.perform(post("/api/auth/change-password").header("Authorization", bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentPassword", PASSWORD, "newPassword", "Another-Password-2026!")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        login(username, "Another-Password-2026!");   // the new password works
    }

    @Test
    @DisplayName("Disabling an account stops its existing token at once, and blocks new logins")
    void disabledAccountIsLockedOutImmediately() throws Exception {
        String username = unique("disabled");
        AppUser user = createUser(username, Role.UNION_REGISTRAR, null, null);
        String token = login(username, PASSWORD);

        user.setEnabled(false);
        appUserRepository.save(user);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}
