package com.acms.inspection;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Step 7: inspections, the Draft -> Submitted -> Published workflow, grading, corrections and access rules. */
class InspectionResultTest extends IntegrationTestBase {

    private static final String DEFAULT_SCALE = """
            {"bands":[{"grade":"A","minScore":80,"description":"Excellent"},
                      {"grade":"B","minScore":70,"description":"Very good"},
                      {"grade":"C","minScore":60,"description":"Good"},
                      {"grade":"D","minScore":50,"description":"Fair"},
                      {"grade":"E","minScore":0,"description":"Below standard"}]}""";

    private World world;
    private AppUser registrar;
    private String registrarToken;
    private AppUser officerA;           // assigned to every offering created here
    private String officerAToken;
    private String otherOfficerAToken;  // same district, NOT assigned
    private String agronomistAToken;
    private String agronomistBToken;    // another district
    private String farmerAToken;        // account of world.farmerA (district A)
    private String farmerBToken;        // account of world.farmerB (district B)

    /** The four inspections of a standard offering, and an enrolment of farmer A in it. */
    private record Setup(long offeringId, long enrolmentId, long soil, long pest, long husbandry, long postHarvest) {
    }

    @BeforeAll
    void createWorldAndUsers() throws Exception {
        String registrarName = unique("insp.registrar");
        registrar = createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        world = createWorld(registrarToken);

        officerA = createUser(unique("insp.officer.a"), Role.FIELD_OFFICER, world.districtA(), null);
        officerAToken = login(officerA.getUsername(), PASSWORD);
        otherOfficerAToken = loginNew("insp.officer.a2", Role.FIELD_OFFICER, world.districtA());
        agronomistAToken = loginNew("insp.agro.a", Role.DISTRICT_AGRONOMIST, world.districtA());
        agronomistBToken = loginNew("insp.agro.b", Role.DISTRICT_AGRONOMIST, world.districtB());
        farmerAToken = login(createUser(unique("insp.farmer.a"), Role.FARMER, null, world.farmerA()).getUsername(), PASSWORD);
        farmerBToken = login(createUser(unique("insp.farmer.b"), Role.FARMER, null, world.farmerB()).getUsername(), PASSWORD);
    }

    private String loginNew(String prefix, Role role, long districtId) throws Exception {
        return login(createUser(unique(prefix), role, districtId, null).getUsername(), PASSWORD);
    }

    // ---------- helpers ----------

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    /** A new offering in district A run by officer A (season and programme are new as well). */
    private long newOffering() throws Exception {
        LocalDate today = LocalDate.now();
        long seasonId = postForId("/api/seasons", registrarToken, json(
                "name", unique("Insp Season"), "startsOn", today.toString(), "endsOn", today.plusDays(120).toString(),
                "maxLandHectares", 5, "distributionDate", today.plusDays(10).toString()));
        long programmeId = postForId("/api/programmes", registrarToken,
                json("name", unique("Irish potatoes"), "districtId", world.districtA()));
        return postForId("/api/offerings", registrarToken, json(
                "programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officerA.getId(),
                "hectaresPerParticipant", 1.0, "maxParticipants", 50));
    }

    /** Test set-up shortcut: enrolment rules are tested in EnrolmentTest, so the row is inserted directly. */
    private long enrolDirectly(long farmerId, long offeringId) {
        Long id = jdbc().queryForObject("""
                INSERT INTO enrolments (offering_id, farmer_id, hectares, enrolled_by)
                VALUES (?, ?, 1.0, ?) RETURNING id""", Long.class, offeringId, farmerId, registrar.getId());
        jdbc().update("UPDATE programme_offerings SET enrolled_count = enrolled_count + 1 WHERE id = ?", offeringId);
        return id;
    }

    private ResultActions addInspection(String token, long offeringId, String name, double max, double weight)
            throws Exception {
        return mockMvc.perform(post("/api/offerings/" + offeringId + "/inspections")
                .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(json("name", name, "maxScore", max, "weight", weight)));
    }

    private long inspection(long offeringId, String name, double max, double weight) throws Exception {
        String body = addInspection(officerAToken, offeringId, name, max, weight)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    /** Soil 20 pts / 25%, Pest 10 pts / 25%, Husbandry 50 pts / 30%, Post-harvest 100 pts / 20%. */
    private Setup standardSetup() throws Exception {
        long offeringId = newOffering();
        long soil = inspection(offeringId, "Soil and land preparation", 20, 25);
        long pest = inspection(offeringId, "Pest and disease control", 10, 25);
        long husbandry = inspection(offeringId, "Crop husbandry", 50, 30);
        long postHarvest = inspection(offeringId, "Post-harvest handling", 100, 20);
        return new Setup(offeringId, enrolDirectly(world.farmerA(), offeringId), soil, pest, husbandry, postHarvest);
    }

    private String scoresJson(Map<Long, Double> scores) {
        return "{\"scores\":[" + scores.entrySet().stream()
                .map(e -> json("inspectionId", e.getKey(), "score", e.getValue()))
                .collect(Collectors.joining(",")) + "]}";
    }

    private ResultActions enterScores(String token, long enrolmentId, Map<Long, Double> scores) throws Exception {
        return mockMvc.perform(put("/api/enrolments/" + enrolmentId + "/scores")
                .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(scoresJson(scores)));
    }

    /** 16/20, 7/10, 40/50, 75/100 -> 0.8x25 + 0.7x25 + 0.8x30 + 0.75x20 = 20 + 17.5 + 24 + 15 = 76.50 (grade B). */
    private long enterStandardScores(Setup s) throws Exception {
        Map<Long, Double> scores = new LinkedHashMap<>();
        scores.put(s.soil(), 16.0);
        scores.put(s.pest(), 7.0);
        scores.put(s.husbandry(), 40.0);
        scores.put(s.postHarvest(), 75.0);
        MvcResult result = enterScores(officerAToken, s.enrolmentId(), scores)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT")).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private ResultActions action(String token, long resultId, String action) throws Exception {
        return mockMvc.perform(post("/api/results/" + resultId + "/" + action).header("Authorization", bearer(token)));
    }

    private ResultActions action(String token, long resultId, String action, String body) throws Exception {
        return mockMvc.perform(post("/api/results/" + resultId + "/" + action).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long publishedStandardResult() throws Exception {
        Setup s = standardSetup();
        long resultId = enterStandardScores(s);
        action(officerAToken, resultId, "submit").andExpect(status().isOk());
        action(agronomistAToken, resultId, "approve").andExpect(status().isOk());
        return resultId;
    }

    private List<Long> ids(String url, String token) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.data.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    // ---------- inspections ----------

    @Test
    @DisplayName("Weights: never above 100% (422); submitting needs exactly 100% and every score (422)")
    void weightsMustTotalHundred() throws Exception {
        long offeringId = newOffering();
        long soil = inspection(offeringId, "Soil", 20, 30);
        long pest = inspection(offeringId, "Pest", 10, 30);
        long husbandry = inspection(offeringId, "Husbandry", 50, 30);
        mockMvc.perform(get("/api/offerings/" + offeringId + "/inspections").header("Authorization", bearer(agronomistAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWeight").value(90.0))
                .andExpect(jsonPath("$.data.weightsComplete").value(false));

        long enrolmentId = enrolDirectly(world.farmerA(), offeringId);
        long resultId = ((Number) JsonPath.read(enterScores(officerAToken, enrolmentId,
                Map.of(soil, 10.0, pest, 5.0, husbandry, 25.0)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id")).longValue();
        action(officerAToken, resultId, "submit").andExpect(status().is(422));        // weights total 90%

        addInspection(officerAToken, offeringId, "Post-harvest", 100, 20).andExpect(status().is(422));   // 110%
        long postHarvest = inspection(offeringId, "Post-harvest", 100, 10);           // exactly 100%
        action(officerAToken, resultId, "submit").andExpect(status().is(422));        // its score is missing

        enterScores(officerAToken, enrolmentId, Map.of(postHarvest, 50.0)).andExpect(status().isOk());
        action(officerAToken, resultId, "submit").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        addInspection(otherOfficerAToken, newOffering(), "Not mine", 10, 10).andExpect(status().isForbidden());
        addInspection(agronomistAToken, offeringId, "Agronomist", 10, 10).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scores lie between 0 and the inspection's maximum; only the offering's own Field Officer enters them")
    void scoreValidation() throws Exception {
        Setup s = standardSetup();
        enterScores(officerAToken, s.enrolmentId(), Map.of(s.soil(), 25.0)).andExpect(status().is(422));   // max 20
        enterScores(officerAToken, s.enrolmentId(), Map.of(s.soil(), -1.0)).andExpect(status().isBadRequest());
        Setup other = standardSetup();
        enterScores(officerAToken, s.enrolmentId(), Map.of(other.soil(), 5.0)).andExpect(status().is(422)); // other offering
        mockMvc.perform(put("/api/enrolments/" + s.enrolmentId() + "/scores").header("Authorization", bearer(officerAToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scores\":[]}"))
                .andExpect(status().isBadRequest());
        enterScores(otherOfficerAToken, s.enrolmentId(), Map.of(s.soil(), 5.0)).andExpect(status().isForbidden());
        enterScores(agronomistAToken, s.enrolmentId(), Map.of(s.soil(), 5.0)).andExpect(status().isForbidden());
        enterScores(officerAToken, s.enrolmentId(), Map.of(s.soil(), 20.0)).andExpect(status().isOk());    // the maximum is fine
    }

    // ---------- workflow ----------

    @Test
    @DisplayName("Draft -> Submitted -> Published: 76.50 = grade B; only the district's Agronomist approves")
    void workflowAndCalculation() throws Exception {
        Setup s = standardSetup();
        long resultId = enterStandardScores(s);

        action(officerAToken, resultId, "submit").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.finalScore").value(76.5))
                .andExpect(jsonPath("$.data.grade").value("B"));

        // Not published yet: invisible to the farmer.
        assertThat(ids("/api/me/results?size=100", farmerAToken)).doesNotContain(resultId);
        mockMvc.perform(get("/api/results/" + resultId).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isForbidden());

        // Who may NOT approve: the Field Officer, and an Agronomist of another district.
        action(officerAToken, resultId, "approve").andExpect(status().isForbidden());
        action(agronomistBToken, resultId, "approve").andExpect(status().isForbidden());
        // A submitted result is no longer editable.
        enterScores(officerAToken, s.enrolmentId(), Map.of(s.soil(), 18.0)).andExpect(status().isConflict());

        action(agronomistAToken, resultId, "approve").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        assertThat(ids("/api/me/results?size=100", farmerAToken)).contains(resultId);
        mockMvc.perform(get("/api/results/" + resultId).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalScore").value(76.5))
                .andExpect(jsonPath("$.data.grade").value("B"));

        // A Field Officer tries to edit a PUBLISHED result: refused, and the inspections are frozen too.
        enterScores(officerAToken, s.enrolmentId(), Map.of(s.soil(), 20.0)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("authorised correction")));
        addInspection(officerAToken, s.offeringId(), "Late extra", 10, 1).andExpect(status().isConflict());
        action(agronomistAToken, resultId, "approve").andExpect(status().isConflict());   // already published
    }

    @Test
    @DisplayName("The Agronomist returns a submitted result for rework (reason required); it becomes editable again")
    void returnForRework() throws Exception {
        Setup s = standardSetup();
        long resultId = enterStandardScores(s);
        action(officerAToken, resultId, "submit").andExpect(status().isOk());

        action(agronomistAToken, resultId, "return", "{}").andExpect(status().isBadRequest());
        action(agronomistBToken, resultId, "return", json("reason", "x")).andExpect(status().isForbidden());
        action(agronomistAToken, resultId, "return", json("reason", "Husbandry score looks too low"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.finalScore").value(org.hamcrest.Matchers.nullValue()));

        enterScores(officerAToken, s.enrolmentId(), Map.of(s.husbandry(), 45.0)).andExpect(status().isOk());
        action(officerAToken, resultId, "submit").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalScore").value(79.5));    // 76.5 + (5/50) x 30
    }

    // ---------- corrections ----------

    @Test
    @DisplayName("Correction of a published result keeps original and corrected values, reason, who and when")
    void authorisedCorrection() throws Exception {
        long resultId = publishedStandardResult();
        long pest = jdbc().queryForObject("""
                SELECT i.id FROM inspections i JOIN inspection_scores s ON s.inspection_id = i.id
                WHERE s.result_id = ? AND i.name = 'Pest and disease control'""", Long.class, resultId);

        action(agronomistAToken, resultId, "corrections", json("inspectionId", pest, "correctedScore", 9))
                .andExpect(status().isBadRequest());                                   // no reason
        action(agronomistBToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 9, "reason", "x")).andExpect(status().isForbidden());
        action(officerAToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 9, "reason", "x")).andExpect(status().isForbidden());
        action(agronomistAToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 11, "reason", "above max")).andExpect(status().is(422));
        action(agronomistAToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 7, "reason", "same")).andExpect(status().isBadRequest());

        // 76.5 + (2/10) x 25 = 81.5 -> grade A
        action(agronomistAToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 9, "reason", "Second visit: pests under control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.finalScore").value(81.5))
                .andExpect(jsonPath("$.data.grade").value("A"))
                .andExpect(jsonPath("$.data.correctionCount").value(1));

        mockMvc.perform(get("/api/results/" + resultId + "/corrections").header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].originalScore").value(7.0))
                .andExpect(jsonPath("$.data[0].correctedScore").value(9.0))
                .andExpect(jsonPath("$.data[0].originalFinalScore").value(76.5))
                .andExpect(jsonPath("$.data[0].correctedFinalScore").value(81.5))
                .andExpect(jsonPath("$.data[0].originalGrade").value("B"))
                .andExpect(jsonPath("$.data[0].correctedGrade").value("A"))
                .andExpect(jsonPath("$.data[0].reason").value("Second visit: pests under control"))
                .andExpect(jsonPath("$.data[0].correctedAt").isNotEmpty());
        // The original score is still there; the current one shows the correction.
        String detail = mockMvc.perform(get("/api/results/" + resultId).header("Authorization", bearer(registrarToken)))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> pestLine = JsonPath.read(detail, "$.data.scores[?(@.inspectionId == " + pest + ")]");
        assertThat(((Number) pestLine.get(0).get("originalScore")).doubleValue()).isEqualTo(7.0);
        assertThat(((Number) pestLine.get(0).get("currentScore")).doubleValue()).isEqualTo(9.0);
        assertThat(pestLine.get(0).get("corrected")).isEqualTo(true);

        // A result that is only SUBMITTED is returned, not corrected.
        Setup s = standardSetup();
        long submitted = enterStandardScores(s);
        action(officerAToken, submitted, "submit").andExpect(status().isOk());
        action(agronomistAToken, submitted, "corrections",
                json("inspectionId", s.pest(), "correctedScore", 9, "reason", "x")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("The database itself refuses to change published scores or history, even with direct SQL")
    void databaseGuards() throws Exception {
        long resultId = publishedStandardResult();
        long pest = jdbc().queryForObject("""
                SELECT i.id FROM inspections i JOIN inspection_scores s ON s.inspection_id = i.id
                WHERE s.result_id = ? AND i.name = 'Pest and disease control'""", Long.class, resultId);
        action(agronomistAToken, resultId, "corrections",
                json("inspectionId", pest, "correctedScore", 8, "reason", "recount")).andExpect(status().isOk());

        assertThatThrownBy(() -> jdbc().update("UPDATE inspection_scores SET score = 0 WHERE result_id = ?", resultId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("only while the result is a DRAFT");
        assertThatThrownBy(() -> jdbc().update("UPDATE inspection_results SET final_score = 99 WHERE id = ?", resultId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("recorded correction");
        assertThatThrownBy(() -> jdbc().update("UPDATE inspection_results SET status = 'DRAFT', final_score = NULL, "
                + "grade = NULL, submitted_at = NULL, submitted_by = NULL, published_at = NULL, published_by = NULL "
                + "WHERE id = ?", resultId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("cannot move");
        assertThatThrownBy(() -> jdbc().update("DELETE FROM result_corrections WHERE result_id = ?", resultId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("permanent");
        assertThatThrownBy(() -> jdbc().update("DELETE FROM inspection_results WHERE id = ?", resultId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("never deleted");
    }

    // ---------- who may read what ----------

    @Test
    @DisplayName("A farmer who changes the farmer id in the URL cannot read another farmer's results (IDOR, 403)")
    void farmersSeeOnlyTheirOwnPublishedResults() throws Exception {
        long published = publishedStandardResult();
        Setup s = standardSetup();
        long submitted = enterStandardScores(s);
        action(officerAToken, submitted, "submit").andExpect(status().isOk());

        mockMvc.perform(get("/api/farmers/" + world.farmerA() + "/results").header("Authorization", bearer(farmerBToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/results/" + published).header("Authorization", bearer(farmerBToken)))
                .andExpect(status().isForbidden());

        List<Long> own = ids("/api/farmers/" + world.farmerA() + "/results?size=100", farmerAToken);
        assertThat(own).contains(published).doesNotContain(submitted);

        String url = "/api/offerings/" + s.offeringId() + "/results";
        mockMvc.perform(get(url).header("Authorization", bearer(agronomistAToken))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(officerAToken))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(agronomistBToken))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(otherOfficerAToken))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(farmerAToken))).andExpect(status().isForbidden());
    }

    // ---------- grading scale ----------

    @Test
    @DisplayName("The grading scale is configurable; results already published keep the grade they received")
    void configurableGradingScale() throws Exception {
        mockMvc.perform(get("/api/grading-scale").header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(5));
        long publishedUnderOldScale = publishedStandardResult();                     // 76.50 -> B

        String stricter = """
                {"bands":[{"grade":"A","minScore":90},{"grade":"B","minScore":80},{"grade":"C","minScore":0}]}""";
        String noZero = """
                {"bands":[{"grade":"A","minScore":90},{"grade":"B","minScore":50}]}""";
        String duplicate = """
                {"bands":[{"grade":"A","minScore":90},{"grade":"a","minScore":0}]}""";
        for (String[] attempt : new String[][] {{officerAToken, stricter, "403"}, {registrarToken, noZero, "400"},
                {registrarToken, duplicate, "400"}}) {
            mockMvc.perform(put("/api/grading-scale").header("Authorization", bearer(attempt[0]))
                            .contentType(MediaType.APPLICATION_JSON).content(attempt[1]))
                    .andExpect(status().is(Integer.parseInt(attempt[2])));
        }

        try {
            mockMvc.perform(put("/api/grading-scale").header("Authorization", bearer(registrarToken))
                            .contentType(MediaType.APPLICATION_JSON).content(stricter))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));

            long publishedUnderNewScale = publishedStandardResult();                 // 76.50 -> now C
            mockMvc.perform(get("/api/results/" + publishedUnderNewScale).header("Authorization", bearer(registrarToken)))
                    .andExpect(jsonPath("$.data.grade").value("C"));
            mockMvc.perform(get("/api/results/" + publishedUnderOldScale).header("Authorization", bearer(registrarToken)))
                    .andExpect(jsonPath("$.data.grade").value("B"));                       // unchanged
        } finally {
            mockMvc.perform(put("/api/grading-scale").header("Authorization", bearer(registrarToken))
                            .contentType(MediaType.APPLICATION_JSON).content(DEFAULT_SCALE))
                    .andExpect(status().isOk());
        }
    }
}
