package com.acms.enrolment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Step 5c: enrolment rules, capacity under concurrency, cancellation, and who may see which enrolments. */
class EnrolmentTest extends IntegrationTestBase {

    private World world;
    private AppUser registrar;
    private String registrarToken;
    private AppUser officerA;           // assigned to the offerings created below
    private String officerAToken;
    private String otherOfficerAToken;  // same district, NOT assigned
    private String officerBToken;       // another district
    private String agronomistAToken;
    private String agronomistBToken;

    /** A farmer of cooperative A (district A) together with the token of their own account. */
    private record TestFarmer(long id, String token) {
    }

    @BeforeAll
    void createWorldAndStaff() throws Exception {
        String registrarName = unique("enrol.registrar");
        registrar = createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        world = createWorld(registrarToken);

        officerA = createUser(unique("enrol.officer.a"), Role.FIELD_OFFICER, world.districtA(), null);
        officerAToken = login(officerA.getUsername(), PASSWORD);
        otherOfficerAToken = login(createUser(unique("enrol.officer.a2"), Role.FIELD_OFFICER,
                world.districtA(), null).getUsername(), PASSWORD);
        officerBToken = login(createUser(unique("enrol.officer.b"), Role.FIELD_OFFICER,
                world.districtB(), null).getUsername(), PASSWORD);
        agronomistAToken = login(createUser(unique("enrol.agro.a"), Role.DISTRICT_AGRONOMIST,
                world.districtA(), null).getUsername(), PASSWORD);
        agronomistBToken = login(createUser(unique("enrol.agro.b"), Role.DISTRICT_AGRONOMIST,
                world.districtB(), null).getUsername(), PASSWORD);
    }

    // ---------- helpers ----------

    private TestFarmer newFarmer(long cooperativeId) throws Exception {
        long farmerId = postForId("/api/farmers", registrarToken,
                json("fullName", "Enrolment Farmer", "nationalId", nationalId(), "cooperativeId", cooperativeId));
        AppUser account = createUser(unique("enrol.farmer"), Role.FARMER, null, farmerId);
        return new TestFarmer(farmerId, login(account.getUsername(), PASSWORD));
    }

    private TestFarmer newFarmerA() throws Exception {
        return newFarmer(world.cooperativeA());
    }

    /** A season of 90 days from today, max 2.5 ha per farmer, with its enrolment window open if asked. */
    private long season(boolean openWindow) throws Exception {
        LocalDate today = LocalDate.now();
        long seasonId = postForId("/api/seasons", registrarToken, json(
                "name", unique("Enrol Season"), "startsOn", today.toString(),
                "endsOn", today.plusDays(90).toString(), "maxLandHectares", 2.5,
                "distributionDate", today.plusDays(30).toString()));
        if (openWindow) {
            mockMvc.perform(post("/api/seasons/" + seasonId + "/windows/open")
                            .header("Authorization", bearer(registrarToken)))
                    .andExpect(status().isCreated());
        }
        return seasonId;
    }

    private long programmeA() throws Exception {
        return postForId("/api/programmes", registrarToken,
                json("name", unique("Beans"), "districtId", world.districtA()));
    }

    private long offering(long programmeId, long seasonId, double hectares, int places) throws Exception {
        return postForId("/api/offerings", registrarToken, json(
                "programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officerA.getId(),
                "hectaresPerParticipant", hectares, "maxParticipants", places));
    }

    /** An offering in district A, in a season whose window is open. */
    private long openOffering(double hectares, int places) throws Exception {
        return offering(programmeA(), season(true), hectares, places);
    }

    /**
     * Test set-up shortcut (Step 6): the farmer owes 1 000 for the offering's season and has paid it all
     * (confirmed), so the 30% rule passes. The payment rules themselves are tested in ContributionPaymentTest.
     */
    private void ensurePaid(TestFarmer farmer, long offeringId) {
        Long seasonId = jdbc().queryForObject(
                "SELECT season_id FROM programme_offerings WHERE id = ?", Long.class, offeringId);
        Integer existing = jdbc().queryForObject(
                "SELECT count(*) FROM contribution_obligations WHERE farmer_id = ? AND season_id = ?",
                Integer.class, farmer.id(), seasonId);
        if (existing != null && existing > 0) {
            return;
        }
        Long obligationId = jdbc().queryForObject("""
                INSERT INTO contribution_obligations (farmer_id, season_id, amount_due, created_by)
                VALUES (?, ?, 1000, ?) RETURNING id""", Long.class, farmer.id(), seasonId, registrar.getId());
        jdbc().update("""
                INSERT INTO payments (obligation_id, transaction_reference, amount, method, status,
                                      provider_reference, requested_by, confirmed_by, confirmed_at)
                VALUES (?, ?, 1000, 'MOBILE_MONEY', 'CONFIRMED', ?, ?, ?, now())""",
                obligationId, "TEST-" + UUID.randomUUID(), "PROVIDER-" + UUID.randomUUID(),
                registrar.getId(), registrar.getId());
    }

    /** A paid-up farmer enrols. */
    private int enrol(TestFarmer farmer, long offeringId) throws Exception {
        ensurePaid(farmer, offeringId);
        return enrolAs(farmer.token(), offeringId);
    }

    /** Anyone with this token tries to enrol (no payment set-up). */
    private int enrolAs(String token, long offeringId) throws Exception {
        return mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments")
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getStatus();
    }

    private long enrolForId(TestFarmer farmer, long offeringId) throws Exception {
        ensurePaid(farmer, offeringId);
        String body = mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments")
                        .header("Authorization", bearer(farmer.token())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.id");
        return id.longValue();
    }

    private int cancel(String token, long enrolmentId, String reason) throws Exception {
        var request = post("/api/enrolments/" + enrolmentId + "/cancel").header("Authorization", bearer(token));
        if (reason != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(json("reason", reason));
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    private int enrolledCount(long offeringId) {
        return jdbc().queryForObject("SELECT enrolled_count FROM programme_offerings WHERE id = ?",
                Integer.class, offeringId);
    }

    private int activeEnrolmentRows(long offeringId) {
        return jdbc().queryForObject(
                "SELECT count(*) FROM enrolments WHERE offering_id = ? AND status = 'ENROLLED'",
                Integer.class, offeringId);
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    // ---------- enrolment rules ----------

    @Test
    @DisplayName("A farmer enrols (201), sees it in /api/me/enrolments, and cannot enrol twice (409)")
    void enrolAndDuplicate() throws Exception {
        long offeringId = openOffering(1.0, 10);
        TestFarmer farmer = newFarmerA();
        ensurePaid(farmer, offeringId);

        mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments")
                        .header("Authorization", bearer(farmer.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.farmerId").value(farmer.id()))
                .andExpect(jsonPath("$.data.hectares").value(1.0));

        mockMvc.perform(get("/api/me/enrolments").header("Authorization", bearer(farmer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].offeringId").value(offeringId));
        mockMvc.perform(get("/api/offerings/" + offeringId).header("Authorization", bearer(farmer.token())))
                .andExpect(jsonPath("$.data.enrolledCount").value(1))
                .andExpect(jsonPath("$.data.remainingPlaces").value(9));

        assertThat(enrol(farmer, offeringId)).isEqualTo(409);
        assertThat(enrolledCount(offeringId)).isEqualTo(1);
    }

    @Test
    @DisplayName("No enrolment while the window is closed (422), before it opens or after it closes")
    void windowMustBeOpen() throws Exception {
        long seasonId = season(false);
        long offeringId = offering(programmeA(), seasonId, 1.0, 10);
        TestFarmer farmer = newFarmerA();

        assertThat(enrol(farmer, offeringId)).isEqualTo(422);

        mockMvc.perform(post("/api/seasons/" + seasonId + "/windows/open").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/seasons/" + seasonId + "/windows/close").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk());
        assertThat(enrol(farmer, offeringId)).isEqualTo(422);
        assertThat(enrolledCount(offeringId)).isZero();
    }

    @Test
    @DisplayName("Only Active farmers may enrol (422)")
    void farmerMustBeActive() throws Exception {
        long offeringId = openOffering(1.0, 10);
        TestFarmer farmer = newFarmerA();
        jdbc().update("UPDATE farmers SET status = 'SUSPENDED' WHERE id = ?", farmer.id());

        assertThat(enrol(farmer, offeringId)).isEqualTo(422);
        assertThat(enrolledCount(offeringId)).isZero();
    }

    @Test
    @DisplayName("Missing prerequisite training (422); after the training is recorded the farmer may enrol")
    void prerequisitesRequired() throws Exception {
        long programmeId = programmeA();
        long trainingId = postForId("/api/trainings", registrarToken, json("name", unique("Soil Management")));
        mockMvc.perform(post("/api/programmes/" + programmeId + "/prerequisites")
                        .header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("trainingId", trainingId)))
                .andExpect(status().isCreated());
        long offeringId = offering(programmeId, season(true), 1.0, 10);
        TestFarmer farmer = newFarmerA();
        ensurePaid(farmer, offeringId);

        mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments")
                        .header("Authorization", bearer(farmer.token())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Soil Management")));

        mockMvc.perform(post("/api/farmers/" + farmer.id() + "/trainings")
                        .header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("trainingId", trainingId, "completedOn", LocalDate.now().toString())))
                .andExpect(status().isCreated());
        assertThat(enrol(farmer, offeringId)).isEqualTo(201);
    }

    @Test
    @DisplayName("The season land limit counts all of a farmer's offerings in that season (422)")
    void seasonLandLimit() throws Exception {
        long seasonId = season(true);                                  // max 2.5 ha per farmer
        long first = offering(programmeA(), seasonId, 1.5, 10);
        long second = offering(programmeA(), seasonId, 1.5, 10);
        TestFarmer farmer = newFarmerA();

        assertThat(enrol(farmer, first)).isEqualTo(201);       // 1.5 ha
        assertThat(enrol(farmer, second)).isEqualTo(422);      // 3.0 ha > 2.5 ha
        assertThat(enrolledCount(second)).isZero();
    }

    @Test
    @DisplayName("Another district's offering (403); staff cannot enrol (403); unknown offering (404)")
    void wrongDistrictAndRoles() throws Exception {
        long offeringId = openOffering(1.0, 10);                       // district A
        TestFarmer farmerB = newFarmer(world.cooperativeB());          // district B

        assertThat(enrolAs(farmerB.token(), offeringId)).isEqualTo(403);
        assertThat(enrolAs(officerAToken, offeringId)).isEqualTo(403);
        assertThat(enrolAs(registrarToken, offeringId)).isEqualTo(403);
        assertThat(enrolAs(newFarmerA().token(), 999_999_999L)).isEqualTo(404);
    }

    // ---------- concurrency ----------

    @Test
    @DisplayName("One place left, 8 farmers press 'enrol' at the same moment: exactly one gets it")
    void lastPlaceGoesToExactlyOneFarmer() throws Exception {
        long offeringId = openOffering(1.0, 1);
        int attempts = 8;
        List<TestFarmer> farmers = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            TestFarmer farmer = newFarmerA();
            ensurePaid(farmer, offeringId);
            farmers.add(farmer);
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (TestFarmer farmer : farmers) {
            results.add(pool.submit(() -> {
                startTogether.await();
                return enrolAs(farmer.token(), offeringId);
            }));
        }
        startTogether.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(attempts - 1);
        assertThat(enrolledCount(offeringId)).isEqualTo(1);
        assertThat(activeEnrolmentRows(offeringId)).isEqualTo(1);
    }

    // ---------- all or nothing ----------

    @Test
    @DisplayName("A crash after the place was taken leaves nothing behind: no enrolment, place still free")
    void failureHalfWayRollsEverythingBack() throws Exception {
        long offeringId = openOffering(1.0, 5);
        TestFarmer farmer = newFarmerA();

        // Make the database fail on the LAST step (inserting the enrolment row) for this offering only,
        // i.e. after the place has already been taken by the UPDATE before it.
        jdbc().execute("""
                CREATE OR REPLACE FUNCTION test_fail_enrolment_insert() RETURNS trigger AS $$
                BEGIN
                    IF NEW.offering_id = %d THEN
                        RAISE EXCEPTION 'simulated failure in the middle of an enrolment';
                    END IF;
                    RETURN NEW;
                END $$ LANGUAGE plpgsql
                """.formatted(offeringId));
        jdbc().execute("""
                CREATE TRIGGER test_fail_enrolment_insert BEFORE INSERT ON enrolments
                FOR EACH ROW EXECUTE FUNCTION test_fail_enrolment_insert()
                """);
        try {
            assertThat(enrol(farmer, offeringId)).isEqualTo(500);
        } finally {
            jdbc().execute("DROP TRIGGER IF EXISTS test_fail_enrolment_insert ON enrolments");
            jdbc().execute("DROP FUNCTION IF EXISTS test_fail_enrolment_insert()");
        }

        assertThat(enrolledCount(offeringId)).as("the place taken half-way was given back").isZero();
        assertThat(activeEnrolmentRows(offeringId)).isZero();

        // And the system works normally again.
        assertThat(enrol(farmer, offeringId)).isEqualTo(201);
        assertThat(enrolledCount(offeringId)).isEqualTo(1);
    }

    // ---------- cancellation ----------

    @Test
    @DisplayName("Cancel: own (200) gives the place back; twice (409); someone else's (403)")
    void farmerCancels() throws Exception {
        long offeringId = openOffering(1.0, 1);
        TestFarmer first = newFarmerA();
        TestFarmer second = newFarmerA();
        long enrolmentId = enrolForId(first, offeringId);
        assertThat(enrol(second, offeringId)).isEqualTo(409);   // full

        assertThat(cancel(second.token(), enrolmentId, null)).isEqualTo(403);
        assertThat(cancel(first.token(), enrolmentId, null)).isEqualTo(200);
        assertThat(cancel(first.token(), enrolmentId, null)).isEqualTo(409);
        assertThat(enrolledCount(offeringId)).isZero();

        assertThat(enrol(second, offeringId)).isEqualTo(201);   // the freed place is usable
        mockMvc.perform(get("/api/enrolments/" + enrolmentId).header("Authorization", bearer(first.token())))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());
    }

    @Test
    @DisplayName("After the window closes only the Registrar may cancel, and must give a reason (400 without)")
    void registrarCancels() throws Exception {
        long seasonId = season(true);
        long offeringId = offering(programmeA(), seasonId, 1.0, 10);
        TestFarmer farmer = newFarmerA();
        long enrolmentId = enrolForId(farmer, offeringId);
        mockMvc.perform(post("/api/seasons/" + seasonId + "/windows/close").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk());

        assertThat(cancel(farmer.token(), enrolmentId, null)).isEqualTo(422);
        assertThat(cancel(officerAToken, enrolmentId, "not allowed")).isEqualTo(403);
        assertThat(cancel(registrarToken, enrolmentId, null)).isEqualTo(400);
        assertThat(cancel(registrarToken, enrolmentId, "Farmer moved to another district")).isEqualTo(200);
        assertThat(enrolledCount(offeringId)).isZero();
    }

    // ---------- who sees which enrolments ----------

    @Test
    @DisplayName("Enrolled farmers of an offering: its Field Officer and the district's Agronomist only")
    void fieldOfficerSeesOwnOfferingOnly() throws Exception {
        long offeringId = openOffering(1.0, 10);
        TestFarmer farmer = newFarmerA();
        long enrolmentId = enrolForId(farmer, offeringId);
        String url = "/api/offerings/" + offeringId + "/enrolments";

        mockMvc.perform(get(url).header("Authorization", bearer(officerAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].farmerId").value(farmer.id()));
        mockMvc.perform(get(url).header("Authorization", bearer(agronomistAToken))).andExpect(status().isOk());
        mockMvc.perform(get(url).header("Authorization", bearer(registrarToken))).andExpect(status().isOk());

        mockMvc.perform(get(url).header("Authorization", bearer(otherOfficerAToken))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(officerBToken))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(agronomistBToken))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).header("Authorization", bearer(farmer.token()))).andExpect(status().isForbidden());

        // One enrolment: its owner yes, another farmer no (changing the id in the URL does not help).
        mockMvc.perform(get("/api/enrolments/" + enrolmentId).header("Authorization", bearer(farmer.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/enrolments/" + enrolmentId).header("Authorization", bearer(newFarmerA().token())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A programme's prerequisites are frozen while farmers are enrolled in it (409)")
    void prerequisitesFrozenWhileEnrolled() throws Exception {
        long programmeId = programmeA();
        long offeringId = offering(programmeId, season(true), 1.0, 10);
        enrolForId(newFarmerA(), offeringId);
        long trainingId = postForId("/api/trainings", registrarToken, json("name", unique("Late Training")));

        mockMvc.perform(post("/api/programmes/" + programmeId + "/prerequisites")
                        .header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("trainingId", trainingId)))
                .andExpect(status().isConflict());
    }
}
