package com.acms.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Every answer has the same envelope: {success, message, data | error, timestamp}. */
class ResponseEnvelopeTest extends IntegrationTestBase {

    private String registrarToken;
    private String farmerToken;

    @BeforeAll
    void users() throws Exception {
        String registrar = unique("env.registrar");
        createUser(registrar, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrar, PASSWORD);
        String farmer = unique("env.farmer");
        createUser(farmer, Role.FARMER, null, null);
        farmerToken = login(farmer, PASSWORD);
    }

    @Test
    @DisplayName("Success: success=true, a message, the DTO under data, a timestamp, and no error part")
    void successEnvelope() throws Exception {
        mockMvc.perform(get("/api/provinces").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("Errors use the same envelope: success=false, message, error {status, path, fieldErrors}, no data")
    void errorEnvelopes() throws Exception {
        // 400 validation, from the GlobalExceptionHandler
        mockMvc.perform(post("/api/seasons").header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.path").value("/api/seasons"))
                .andExpect(jsonPath("$.error.fieldErrors").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
        // 404
        mockMvc.perform(get("/api/seasons/999999999").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(404));
        // 401 and 403 come from the security filters, not from a controller: same envelope
        mockMvc.perform(get("/api/provinces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(401));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(403));
    }

    @Test
    @DisplayName("GET /api/permissions shows the matrix; /api/auth/me lists the caller's own permissions")
    void permissionMatrixEndpoint() throws Exception {
        mockMvc.perform(get("/api/permissions").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows").isNotEmpty())
                .andExpect(jsonPath("$.data.byRole.FINANCE_OFFICER", Matchers.hasItem("PAYMENT_MANAGE")))
                .andExpect(jsonPath("$.data.byRole.FARMER", Matchers.not(Matchers.hasItem("PAYMENT_MANAGE"))));
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", Matchers.hasItem("ENROLMENT_CREATE")))
                .andExpect(jsonPath("$.data.permissions", Matchers.not(Matchers.hasItem("FARMER_LIST"))));
    }
}
