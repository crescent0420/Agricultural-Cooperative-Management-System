package com.acms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Authorisation, in the three layers of the design:
 *  1. function level (role)   2. district level   3. record ownership (IDOR defence).
 */
class AuthorizationRulesTest extends IntegrationTestBase {

    private World world;
    private String adminToken;
    private String registrarToken;
    private String financeToken;
    private String officerToken;   // Field Officer of district A
    private String farmerToken;    // account linked to farmer A

    @BeforeAll
    void createUsersAndData() throws Exception {
        String admin = unique("rules.admin");
        String registrar = unique("rules.registrar");
        String finance = unique("rules.finance");
        createUser(admin, Role.SYSTEM_ADMIN, null, null);
        createUser(registrar, Role.UNION_REGISTRAR, null, null);
        createUser(finance, Role.FINANCE_OFFICER, null, null);
        registrarToken = login(registrar, PASSWORD);
        adminToken = login(admin, PASSWORD);
        financeToken = login(finance, PASSWORD);

        world = createWorld(registrarToken);

        String officer = unique("rules.officer");
        String farmer = unique("rules.farmer");
        createUser(officer, Role.FIELD_OFFICER, world.districtA(), null);
        createUser(farmer, Role.FARMER, null, world.farmerA());
        officerToken = login(officer, PASSWORD);
        farmerToken = login(farmer, PASSWORD);
    }

    @Nested
    @DisplayName("1. Function level: a role may only call its own endpoints")
    class FunctionLevel {

        @Test
        @DisplayName("Farmer cannot list user accounts")
        void farmerCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users").header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Finance Officer cannot create a district")
        void financeCannotCreateDistrict() throws Exception {
            mockMvc.perform(post("/api/districts").header("Authorization", bearer(financeToken))
                            .contentType(MediaType.APPLICATION_JSON).content(json("name", unique("nope"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Field Officer cannot register a farmer")
        void officerCannotRegisterFarmer() throws Exception {
            mockMvc.perform(post("/api/farmers").header("Authorization", bearer(officerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("fullName", "X", "nationalId", nationalId(), "cooperativeId", world.cooperativeA())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Registrar cannot read the audit log")
        void registrarCannotReadAuditLog() throws Exception {
            mockMvc.perform(get("/api/audit-logs").header("Authorization", bearer(registrarToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("System Administrator cannot open a farmer's personal details")
        void adminCannotOpenFarmerDetails() throws Exception {
            mockMvc.perform(get("/api/farmers/" + world.farmerA()).header("Authorization", bearer(adminToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Every real role may read reference data")
        void everyoneMayReadDistricts() throws Exception {
            for (String token : List.of(adminToken, registrarToken, financeToken, officerToken, farmerToken)) {
                mockMvc.perform(get("/api/districts").header("Authorization", bearer(token)))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("2. District level: Field Officers see only their own district")
    class DistrictLevel {

        @Test
        @DisplayName("Officer may open a farmer of their district, not of another district")
        void officerSeesOnlyOwnDistrictFarmer() throws Exception {
            mockMvc.perform(get("/api/farmers/" + world.farmerA()).header("Authorization", bearer(officerToken)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/farmers/" + world.farmerB()).header("Authorization", bearer(officerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Officer's farmer search returns only their district, whatever filters are sent")
        void officerSearchIsLimitedToOwnDistrict() throws Exception {
            String body = mockMvc.perform(get("/api/farmers").param("size", "100")
                            .header("Authorization", bearer(officerToken)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Number> districts = JsonPath.read(body, "$.content[*].districtId");
            assertThat(districts).isNotEmpty();
            assertThat(districts).allMatch(d -> d.longValue() == world.districtA());

            // Asking for district B's cooperative explicitly still returns nothing.
            mockMvc.perform(get("/api/farmers").param("cooperativeId", String.valueOf(world.cooperativeB()))
                            .header("Authorization", bearer(officerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Officer cannot list another district's programmes")
        void officerCannotListOtherDistrictProgrammes() throws Exception {
            mockMvc.perform(get("/api/programmes").param("districtId", String.valueOf(world.districtB()))
                            .header("Authorization", bearer(officerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("3. Record ownership: a farmer cannot read another farmer's record (IDOR)")
    class Ownership {

        @Test
        @DisplayName("Farmer may read their own record")
        void farmerReadsOwnRecord() throws Exception {
            mockMvc.perform(get("/api/farmers/" + world.farmerA()).header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(Math.toIntExact(world.farmerA())));
        }

        @Test
        @DisplayName("Changing the id in the URL to another farmer's id gives 403")
        void farmerCannotReadAnotherFarmer() throws Exception {
            mockMvc.perform(get("/api/farmers/" + world.farmerB()).header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/farmers/" + world.farmerB() + "/trainings").header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("A non-existent id gives the same 403, so ids cannot be probed")
        void unknownIdLooksTheSameAsSomeoneElses() throws Exception {
            mockMvc.perform(get("/api/farmers/999999999").header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/api/me/profile uses the token, not an id from the request")
        void ownProfileComesFromTheToken() throws Exception {
            mockMvc.perform(get("/api/me/profile").header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(Math.toIntExact(world.farmerA())));
        }
    }
}
