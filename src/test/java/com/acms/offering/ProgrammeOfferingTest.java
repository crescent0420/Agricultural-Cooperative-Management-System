package com.acms.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Step 5b: programme offerings (programme + season + Field Officer + land per participant + places). */
class ProgrammeOfferingTest extends IntegrationTestBase {

    private World world;
    private String registrarToken;
    private String officerAToken;     // Field Officer of district A (Huye)
    private String officerBToken;     // Field Officer of district B (Musanze)
    private String farmerAToken;      // farmer of a cooperative in district A
    private String farmerBToken;      // farmer of a cooperative in district B
    private AppUser officerA;
    private AppUser secondOfficerA;
    private AppUser officerB;
    private AppUser agronomistA;

    @BeforeAll
    void createWorldAndUsers() throws Exception {
        String registrarName = unique("offering.registrar");
        createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        world = createWorld(registrarToken);

        officerA = createUser(unique("offering.officer.a"), Role.FIELD_OFFICER, world.districtA(), null);
        secondOfficerA = createUser(unique("offering.officer.a2"), Role.FIELD_OFFICER, world.districtA(), null);
        officerB = createUser(unique("offering.officer.b"), Role.FIELD_OFFICER, world.districtB(), null);
        agronomistA = createUser(unique("offering.agronomist.a"), Role.DISTRICT_AGRONOMIST, world.districtA(), null);
        AppUser farmerA = createUser(unique("offering.farmer.a"), Role.FARMER, null, world.farmerA());
        AppUser farmerB = createUser(unique("offering.farmer.b"), Role.FARMER, null, world.farmerB());

        officerAToken = login(officerA.getUsername(), PASSWORD);
        officerBToken = login(officerB.getUsername(), PASSWORD);
        farmerAToken = login(farmerA.getUsername(), PASSWORD);
        farmerBToken = login(farmerB.getUsername(), PASSWORD);
    }

    // ---------- helpers ----------

    /** A programme run by district A. */
    private long programmeInDistrictA() throws Exception {
        return postForId("/api/programmes", registrarToken,
                json("name", unique("Maize"), "districtId", world.districtA()));
    }

    /** A season that runs from `startOffset` to `endOffset` days from today; max 2.5 ha per farmer. */
    private long season(int startOffset, int endOffset) throws Exception {
        LocalDate today = LocalDate.now();
        return postForId("/api/seasons", registrarToken, json(
                "name", unique("Season"),
                "startsOn", today.plusDays(startOffset).toString(),
                "endsOn", today.plusDays(endOffset).toString(),
                "maxLandHectares", 2.5,
                "distributionDate", today.plusDays(startOffset).toString()));
    }

    private String offeringJson(long programmeId, long seasonId, long officerId, double hectares, int max) {
        return json("programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officerId,
                "hectaresPerParticipant", hectares, "maxParticipants", max);
    }

    private String updateJson(long officerId, double hectares, int max) {
        return json("fieldOfficerId", officerId, "hectaresPerParticipant", hectares, "maxParticipants", max);
    }

    private long createOffering() throws Exception {
        return postForId("/api/offerings", registrarToken,
                offeringJson(programmeInDistrictA(), season(0, 90), officerA.getId(), 1.5, 40));
    }

    private void postOffering(String token, String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/offerings").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    private void putOffering(long id, String body, int expectedStatus) throws Exception {
        mockMvc.perform(put("/api/offerings/" + id).header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    private List<Long> listIds(String url, String token) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    // ---------- tests ----------

    @Test
    @DisplayName("Registrar creates an offering: all places free; the same programme and season again is 409")
    void createAndDuplicate() throws Exception {
        long programmeId = programmeInDistrictA();
        long seasonId = season(0, 90);
        String body = offeringJson(programmeId, seasonId, officerA.getId(), 1.5, 40);

        long id = postForId("/api/offerings", registrarToken, body);

        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districtId").value(world.districtA()))
                .andExpect(jsonPath("$.fieldOfficerUsername").value(officerA.getUsername()))
                .andExpect(jsonPath("$.hectaresPerParticipant").value(1.5))
                .andExpect(jsonPath("$.maxParticipants").value(40))
                .andExpect(jsonPath("$.enrolledCount").value(0))
                .andExpect(jsonPath("$.remainingPlaces").value(40));

        postOffering(registrarToken, body, 409);
    }

    @Test
    @DisplayName("Business rules: wrong district officer, not an officer, too much land, ended season (all 422)")
    void businessRules() throws Exception {
        long programmeId = programmeInDistrictA();
        long seasonId = season(0, 90);

        // Field Officer of ANOTHER district
        postOffering(registrarToken, offeringJson(programmeId, seasonId, officerB.getId(), 1.5, 40), 422);
        // A District Agronomist is not a Field Officer, even in the right district
        postOffering(registrarToken, offeringJson(programmeId, seasonId, agronomistA.getId(), 1.5, 40), 422);
        // 3 ha per participant, but the season allows only 2.5 ha per farmer
        postOffering(registrarToken, offeringJson(programmeId, seasonId, officerA.getId(), 3.0, 40), 422);
        // A season that ended yesterday
        postOffering(registrarToken, offeringJson(programmeId, season(-100, -1), officerA.getId(), 1.5, 40), 422);
    }

    @Test
    @DisplayName("Validation (400), unknown ids (404) and non-registrars (403) are refused")
    void validationNotFoundAndForbidden() throws Exception {
        long programmeId = programmeInDistrictA();
        long seasonId = season(0, 90);

        postOffering(registrarToken, offeringJson(programmeId, seasonId, officerA.getId(), 1.5, 0), 400);
        postOffering(registrarToken, offeringJson(programmeId, seasonId, officerA.getId(), 0, 40), 400);
        postOffering(registrarToken, json("programmeId", programmeId), 400);

        postOffering(registrarToken, offeringJson(999_999_999L, seasonId, officerA.getId(), 1.5, 40), 404);
        postOffering(registrarToken, offeringJson(programmeId, 999_999_999L, officerA.getId(), 1.5, 40), 404);

        postOffering(officerAToken, offeringJson(programmeId, seasonId, officerA.getId(), 1.5, 40), 403);
        postOffering(farmerAToken, offeringJson(programmeId, seasonId, officerA.getId(), 1.5, 40), 403);
    }

    @Test
    @DisplayName("District scope: officers and farmers see only their own district's offerings")
    void districtScope() throws Exception {
        long id = createOffering();   // district A

        assertThat(listIds("/api/offerings?size=100", registrarToken)).contains(id);
        assertThat(listIds("/api/offerings?size=100", officerAToken)).contains(id);
        assertThat(listIds("/api/offerings?size=100", officerBToken)).doesNotContain(id);
        // Asking explicitly for district A does not help an officer of district B
        assertThat(listIds("/api/offerings?size=100&districtId=" + world.districtA(), officerBToken)).isEmpty();

        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(farmerBToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(officerBToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("assigned-to-me lists only the offerings of the calling Field Officer")
    void assignedToMe() throws Exception {
        long mine = createOffering();   // officer A
        long notMine = postForId("/api/offerings", registrarToken,
                offeringJson(programmeInDistrictA(), season(0, 90), secondOfficerA.getId(), 1.5, 40));

        List<Long> ids = listIds("/api/offerings/assigned-to-me?size=100", officerAToken);
        assertThat(ids).contains(mine).doesNotContain(notMine);

        mockMvc.perform(get("/api/offerings/assigned-to-me").header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update: change places and officer; refuse an officer of another district")
    void update() throws Exception {
        long id = createOffering();

        putOffering(id, updateJson(secondOfficerA.getId(), 2.0, 60), 200);
        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(registrarToken)))
                .andExpect(jsonPath("$.fieldOfficerId").value(secondOfficerA.getId()))
                .andExpect(jsonPath("$.hectaresPerParticipant").value(2.0))
                .andExpect(jsonPath("$.maxParticipants").value(60));

        putOffering(id, updateJson(officerB.getId(), 2.0, 60), 422);

        mockMvc.perform(put("/api/offerings/" + id).header("Authorization", bearer(officerAToken))
                        .contentType(MediaType.APPLICATION_JSON).content(updateJson(officerA.getId(), 2.0, 60)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Once farmers are enrolled: places cannot drop below them, land per participant is frozen")
    void updateAfterEnrolments() throws Exception {
        long id = createOffering();   // 40 places of 1.5 ha
        // Enrolment arrives in Step 5c; until then, pretend 3 farmers have enrolled.
        jdbc().update("UPDATE programme_offerings SET enrolled_count = 3 WHERE id = ?", id);

        putOffering(id, updateJson(officerA.getId(), 1.5, 2), 409);    // below the 3 already enrolled
        putOffering(id, updateJson(officerA.getId(), 2.0, 40), 409);   // hectares changed
        putOffering(id, updateJson(officerA.getId(), 1.5, 3), 200);    // exactly full is fine

        mockMvc.perform(get("/api/offerings/" + id).header("Authorization", bearer(registrarToken)))
                .andExpect(jsonPath("$.remainingPlaces").value(0));
    }

    @Test
    @DisplayName("The database itself refuses more participants than places, even if Java had a bug")
    void databaseCapacityGuard() throws Exception {
        long id = createOffering();   // 40 places

        assertThatThrownBy(() ->
                jdbc().update("UPDATE programme_offerings SET enrolled_count = 41 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_offerings_capacity");
    }
}
