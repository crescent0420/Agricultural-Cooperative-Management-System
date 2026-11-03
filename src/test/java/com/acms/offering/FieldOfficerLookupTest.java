package com.acms.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.enums.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Frontend phase, gap G8: the Union Registrar's Field Officer dropdown, GET /api/field-officers.
 * Proves: district filter, enabled officers only, sorted by username, no email, Registrar only.
 */
class FieldOfficerLookupTest extends IntegrationTestBase {

    private World world;
    private String registrarToken;
    private AppUser bravoA;          // enabled Field Officer of district A, created FIRST
    private AppUser alphaA;          // enabled Field Officer of district A, created second, sorts first
    private AppUser disabledA;       // disabled Field Officer of district A
    private AppUser officerB;        // enabled Field Officer of district B
    private AppUser agronomistA;     // district A, but not a Field Officer
    private List<String> otherRoleTokens;

    @BeforeAll
    void createWorldAndUsers() throws Exception {
        String registrarName = unique("lookup.registrar");
        createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        world = createWorld(registrarToken);

        // "fo.bravo" is created before "fo.alpha", so a correct answer is sorted by name, not by id.
        bravoA = createUser(unique("fo.bravo"), Role.FIELD_OFFICER, world.districtA(), null);
        alphaA = createUser(unique("fo.alpha"), Role.FIELD_OFFICER, world.districtA(), null);
        disabledA = createUser(unique("fo.disabled"), Role.FIELD_OFFICER, world.districtA(), null);
        disabledA.setEnabled(false);
        appUserRepository.save(disabledA);
        officerB = createUser(unique("fo.musanze"), Role.FIELD_OFFICER, world.districtB(), null);
        agronomistA = createUser(unique("lookup.agronomist"), Role.DISTRICT_AGRONOMIST, world.districtA(), null);

        AppUser admin = createUser(unique("lookup.admin"), Role.SYSTEM_ADMIN, null, null);
        AppUser finance = createUser(unique("lookup.finance"), Role.FINANCE_OFFICER, null, null);
        AppUser farmer = createUser(unique("lookup.farmer"), Role.FARMER, null, world.farmerA());
        otherRoleTokens = List.of(
                login(bravoA.getUsername(), PASSWORD),
                login(agronomistA.getUsername(), PASSWORD),
                login(admin.getUsername(), PASSWORD),
                login(finance.getUsername(), PASSWORD),
                login(farmer.getUsername(), PASSWORD));
    }

    private String getAsRegistrar(String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Registrar lists one district's enabled Field Officers, sorted by username, without email")
    void listsOneDistrictsEnabledOfficers() throws Exception {
        String body = getAsRegistrar("/api/field-officers?districtId=" + world.districtA());

        List<String> usernames = JsonPath.read(body, "$.data[*].username");
        assertThat(usernames)
                .contains(alphaA.getUsername(), bravoA.getUsername())
                .doesNotContain(disabledA.getUsername(), officerB.getUsername(), agronomistA.getUsername());
        assertThat(usernames.indexOf(alphaA.getUsername()))
                .as("sorted by username, not by creation order")
                .isLessThan(usernames.indexOf(bravoA.getUsername()));

        List<Number> districtIds = JsonPath.read(body, "$.data[*].districtId");
        assertThat(districtIds).allSatisfy(id -> assertThat(id.longValue()).isEqualTo(world.districtA()));

        List<String> districtNames = JsonPath.read(body, "$.data[*].districtName");
        assertThat(districtNames).containsOnly("Huye");

        assertThat(body).as("no email addresses are exposed").doesNotContain("@example.test");
        List<Object> emails = JsonPath.read(body, "$.data[*].email");
        assertThat(emails).isEmpty();
    }

    @Test
    @DisplayName("Without a district, enabled Field Officers of every district are listed")
    void listsEveryDistrictWithoutFilter() throws Exception {
        String body = getAsRegistrar("/api/field-officers");

        List<String> usernames = JsonPath.read(body, "$.data[*].username");
        assertThat(usernames)
                .contains(alphaA.getUsername(), bravoA.getUsername(), officerB.getUsername())
                .doesNotContain(disabledA.getUsername(), agronomistA.getUsername());
    }

    @Test
    @DisplayName("Only the Union Registrar may use it: other roles get 403, no token gets 401")
    void onlyTheRegistrar() throws Exception {
        for (String token : otherRoleTokens) {
            mockMvc.perform(get("/api/field-officers").header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/field-officers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An unknown district is 404; a district id that is not a number is 400")
    void badDistrictFilter() throws Exception {
        mockMvc.perform(get("/api/field-officers?districtId=999999999")
                        .header("Authorization", bearer(registrarToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/field-officers?districtId=abc")
                        .header("Authorization", bearer(registrarToken)))
                .andExpect(status().isBadRequest());
    }
}