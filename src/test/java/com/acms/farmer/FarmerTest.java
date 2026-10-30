package com.acms.farmer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Section (c) of the brief: farmer registration and status.
 * Unique membership numbers, duplicate national ids refused, search with pagination and sorting,
 * district scope, and only sensible status changes, made by the Union Registrar only.
 */
class FarmerTest extends IntegrationTestBase {

    private World world;
    private String registrarToken;
    private String officerAToken;
    private String financeToken;

    @BeforeAll
    void createWorldAndStaff() throws Exception {
        registrarToken = login(createUser(unique("farmer.registrar"), Role.UNION_REGISTRAR, null, null).getUsername(), PASSWORD);
        world = createWorld(registrarToken);
        officerAToken = login(createUser(unique("farmer.officer"), Role.FIELD_OFFICER, world.districtA(), null)
                .getUsername(), PASSWORD);
        financeToken = login(createUser(unique("farmer.finance"), Role.FINANCE_OFFICER, null, null).getUsername(), PASSWORD);
    }

    // ---------- registration ----------

    @Test
    @DisplayName("Registration gives every farmer a unique membership number (RCU-000123) and the status Active")
    void registrationGivesAUniqueMembershipNumber() throws Exception {
        String first = register("Uwimana Grace", nationalId(), world.cooperativeA()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String second = register("Mugisha Eric", nationalId(), world.cooperativeA()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String number1 = JsonPath.read(first, "$.data.membershipNumber");
        String number2 = JsonPath.read(second, "$.data.membershipNumber");
        assertThat(number1).matches("^RCU-\\d{6}$");
        assertThat(number2).matches("^RCU-\\d{6}$").isNotEqualTo(number1);
    }

    @Test
    @DisplayName("Duplicate national id (409); invalid data (400, naming the field); unknown cooperative (404); not the Registrar (403)")
    void registrationIsValidated() throws Exception {
        String nationalId = nationalId();
        register("First Person", nationalId, world.cooperativeA()).andExpect(status().isCreated());
        register("Second Person", nationalId, world.cooperativeB())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("national ID")));

        register("Short Id", "12345", world.cooperativeA())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[*].field", Matchers.hasItem("nationalId")));
        register("", nationalId(), world.cooperativeA())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[*].field", Matchers.hasItem("fullName")));
        register("No Cooperative", nationalId(), 999_999_999L).andExpect(status().isNotFound());

        mockMvc.perform(post("/api/farmers").header("Authorization", bearer(officerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("fullName", "By Officer", "nationalId", nationalId(), "cooperativeId", world.cooperativeA())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("The database itself refuses a duplicate membership number or national id")
    void databaseRefusesDuplicates() throws Exception {
        long id = postForId("/api/farmers", registrarToken,
                json("fullName", "Unique Farmer", "nationalId", nationalId(), "cooperativeId", world.cooperativeA()));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String membership = jdbc.queryForObject("SELECT membership_number FROM farmers WHERE id = ?", String.class, id);
        String national = jdbc.queryForObject("SELECT national_id FROM farmers WHERE id = ?", String.class, id);

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO farmers (membership_number, national_id, full_name, cooperative_id)
                        VALUES (?, ?, 'Copy', ?)""", membership, nationalId(), world.cooperativeA()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO farmers (membership_number, national_id, full_name, cooperative_id)
                        VALUES (?, ?, 'Copy', ?)""", "RCU-T" + (System.nanoTime() % 100000), national, world.cooperativeA()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- search ----------

    @Test
    @DisplayName("Search by name, membership number, cooperative and status; paginated and sortable")
    void searchPaginatedAndSorted() throws Exception {
        long cooperative = postForId("/api/cooperatives", registrarToken,
                json("name", unique("Search Coop"), "districtId", world.districtA()));
        String tag = "Srch" + System.nanoTime() % 1_000_000;
        long anne = postForId("/api/farmers", registrarToken, farmer(tag + " Anne", cooperative));
        postForId("/api/farmers", registrarToken, farmer(tag + " Bosco", cooperative));
        long claire = postForId("/api/farmers", registrarToken, farmer(tag + " Claire", cooperative));
        changeStatus(registrarToken, claire, "SUSPENDED").andExpect(status().isOk());

        // cooperative filter + sort by name, descending + 2 per page
        String page0 = search("?cooperativeId=" + cooperative + "&sortBy=fullName&direction=desc&size=2&page=0");
        assertThat(JsonPath.<List<String>>read(page0, "$.data.content[*].fullName"))
                .containsExactly(tag + " Claire", tag + " Bosco");
        assertThat(((Number) JsonPath.read(page0, "$.data.totalElements")).longValue()).isEqualTo(3);
        assertThat(((Number) JsonPath.read(page0, "$.data.totalPages")).intValue()).isEqualTo(2);
        String page1 = search("?cooperativeId=" + cooperative + "&sortBy=fullName&direction=desc&size=2&page=1");
        assertThat(JsonPath.<List<String>>read(page1, "$.data.content[*].fullName")).containsExactly(tag + " Anne");

        // name: part of the name, any case
        String byName = searchWith("name", tag.toLowerCase() + " ann");
        assertThat(JsonPath.<List<Integer>>read(byName, "$.data.content[*].id")).extracting(Integer::longValue)
                .containsExactly(anne);

        // membership number
        String number = new JdbcTemplate(dataSource).queryForObject(
                "SELECT membership_number FROM farmers WHERE id = ?", String.class, anne);
        String byNumber = search("?membershipNumber=" + number);
        assertThat(JsonPath.<List<Integer>>read(byNumber, "$.data.content[*].id")).extracting(Integer::longValue)
                .containsExactly(anne);

        // status
        String suspended = search("?cooperativeId=" + cooperative + "&status=SUSPENDED");
        assertThat(JsonPath.<List<Integer>>read(suspended, "$.data.content[*].id")).extracting(Integer::longValue)
                .containsExactly(claire);

        // a % typed by the user is a character, not a wildcard: it matches nobody here
        String wildcard = searchWith("cooperativeId", String.valueOf(cooperative), "name", "%");
        assertThat(JsonPath.<List<Object>>read(wildcard, "$.data.content")).isEmpty();

        // sorting only on known fields; sizes within limits
        mockMvc.perform(get("/api/farmers?sortBy=nationalId").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/farmers?size=0").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("District scope: a Field Officer only finds and opens farmers of their own district")
    void districtScope() throws Exception {
        String body = mockMvc.perform(get("/api/farmers?size=100&cooperativeId=" + world.cooperativeB())
                        .header("Authorization", bearer(officerAToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Object>>read(body, "$.data.content")).isEmpty();

        String own = mockMvc.perform(get("/api/farmers?size=100").header("Authorization", bearer(officerAToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(own, "$.data.content[*].districtId"))
                .isNotEmpty().allMatch(id -> id == world.districtA());

        mockMvc.perform(get("/api/farmers/" + world.farmerB()).header("Authorization", bearer(officerAToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/farmers/" + world.farmerA()).header("Authorization", bearer(officerAToken)))
                .andExpect(status().isOk());
    }

    // ---------- status ----------

    @Test
    @DisplayName("Status: Active -> Suspended -> Active -> Retired; a retired farmer can never be reactivated (422)")
    void statusTransitions() throws Exception {
        long id = postForId("/api/farmers", registrarToken, farmer("Status Farmer", world.cooperativeA()));

        changeStatus(registrarToken, id, "SUSPENDED").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        changeStatus(registrarToken, id, "ACTIVE").andExpect(status().isOk());
        changeStatus(registrarToken, id, "RETIRED").andExpect(status().isOk());

        changeStatus(registrarToken, id, "ACTIVE")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("RETIRED")));
        changeStatus(registrarToken, id, "WITHDRAWN").andExpect(status().is(422));
        mockMvc.perform(get("/api/farmers/" + id).header("Authorization", bearer(registrarToken)))
                .andExpect(jsonPath("$.data.status").value("RETIRED"));

        // each real change is in the audit log, with the old and the new status
        List<String> details = new JdbcTemplate(dataSource).queryForList("""
                SELECT details FROM audit_log
                 WHERE event_type = 'FARMER_STATUS_CHANGED' AND target_id = ? ORDER BY id""", String.class, id);
        assertThat(details).hasSize(3);
        assertThat(details.get(2)).contains("ACTIVE").contains("RETIRED");
    }

    @Test
    @DisplayName("Withdrawn can only return to Active; only the Registrar changes status (403); a missing status is 400")
    void statusRules() throws Exception {
        long id = postForId("/api/farmers", registrarToken, farmer("Withdrawn Farmer", world.cooperativeA()));
        changeStatus(registrarToken, id, "WITHDRAWN").andExpect(status().isOk());
        changeStatus(registrarToken, id, "SUSPENDED").andExpect(status().is(422));
        changeStatus(registrarToken, id, "ACTIVE").andExpect(status().isOk());

        changeStatus(officerAToken, id, "SUSPENDED").andExpect(status().isForbidden());
        changeStatus(financeToken, id, "SUSPENDED").andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/farmers/" + id + "/status").header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        changeStatus(registrarToken, 999_999_999L, "SUSPENDED").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The Registrar corrects a farmer's name and phone; the membership number and national id stay")
    void update() throws Exception {
        long id = postForId("/api/farmers", registrarToken, farmer("Old Name", world.cooperativeA()));
        String before = mockMvc.perform(get("/api/farmers/" + id).header("Authorization", bearer(registrarToken)))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(put("/api/farmers/" + id).header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("fullName", "  New Name  ", "phoneNumber", "+250788123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("New Name"))
                .andExpect(jsonPath("$.data.membershipNumber").value((String) JsonPath.read(before, "$.data.membershipNumber")))
                .andExpect(jsonPath("$.data.nationalId").value((String) JsonPath.read(before, "$.data.nationalId")));

        mockMvc.perform(put("/api/farmers/" + id).header("Authorization", bearer(officerAToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("fullName", "Hacked")))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private String farmer(String fullName, long cooperativeId) {
        return json("fullName", fullName, "nationalId", nationalId(), "cooperativeId", cooperativeId);
    }

    private ResultActions register(String fullName, String nationalId, long cooperativeId) throws Exception {
        return mockMvc.perform(post("/api/farmers").header("Authorization", bearer(registrarToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("fullName", fullName, "nationalId", nationalId, "cooperativeId", cooperativeId)));
    }

    private ResultActions changeStatus(String token, long farmerId, String status) throws Exception {
        return mockMvc.perform(patch("/api/farmers/" + farmerId + "/status").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json("status", status)));
    }

    /** Parameters passed one by one, so MockMvc encodes each value exactly once (spaces, %...). */
    private String searchWith(String... namesAndValues) throws Exception {
        var request = get("/api/farmers").header("Authorization", bearer(registrarToken));
        for (int i = 0; i < namesAndValues.length; i += 2) {
            request.param(namesAndValues[i], namesAndValues[i + 1]);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String search(String query) throws Exception {
        return mockMvc.perform(get("/api/farmers" + query).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
