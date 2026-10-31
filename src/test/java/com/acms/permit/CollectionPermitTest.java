package com.acms.permit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.enums.Role;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** Step 8: collection permits - the rules, a unique verifiable number, and a record of every attempt. */
class CollectionPermitTest extends IntegrationTestBase {

    private static final String PERMIT_NUMBER = "^CP-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$";

    private World world;
    private AppUser registrar;
    private String registrarToken;
    private String financeToken;
    private AppUser officerA;

    /** A farmer of cooperative A with their own account. */
    private record TestFarmer(long id, String token) {
    }

    @BeforeAll
    void createWorldAndStaff() throws Exception {
        String registrarName = unique("permit.registrar");
        registrar = createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        financeToken = login(createUser(unique("permit.finance"), Role.FINANCE_OFFICER, null, null).getUsername(), PASSWORD);
        world = createWorld(registrarToken);
        officerA = createUser(unique("permit.officer"), Role.FIELD_OFFICER, world.districtA(), null);
    }

    // ---------- set-up shortcuts (the enrolment and payment rules are tested in their own classes) ----------

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    private TestFarmer newFarmer() throws Exception {
        long farmerId = postForId("/api/farmers", registrarToken,
                json("fullName", "Permit Farmer", "nationalId", nationalId(), "cooperativeId", world.cooperativeA()));
        AppUser account = createUser(unique("permit.farmer"), Role.FARMER, null, farmerId);
        return new TestFarmer(farmerId, login(account.getUsername(), PASSWORD));
    }

    /** A season whose distribution day is `distributionInDays` from today, an offering in it, and its id. */
    private long offering(int distributionInDays) throws Exception {
        LocalDate today = LocalDate.now();
        long seasonId = postForId("/api/seasons", registrarToken, json(
                "name", unique("Permit Season"), "startsOn", today.minusDays(10).toString(),
                "endsOn", today.plusDays(60).toString(), "maxLandHectares", 5,
                "distributionDate", today.plusDays(distributionInDays).toString()));
        long programmeId = postForId("/api/programmes", registrarToken,
                json("name", unique("Wheat"), "districtId", world.districtA()));
        return postForId("/api/offerings", registrarToken, json(
                "programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officerA.getId(),
                "hectaresPerParticipant", 1.0, "maxParticipants", 50));
    }

    private long enrolDirectly(TestFarmer farmer, long offeringId) {
        Long id = jdbc().queryForObject("""
                INSERT INTO enrolments (offering_id, farmer_id, hectares, enrolled_by)
                VALUES (?, ?, 1.0, ?) RETURNING id""", Long.class, offeringId, farmer.id(), registrar.getId());
        jdbc().update("UPDATE programme_offerings SET enrolled_count = enrolled_count + 1 WHERE id = ?", offeringId);
        return id;
    }

    /** The farmer owes 1 000 for the offering's season and has paid `paid` of it (confirmed). */
    private void contribution(TestFarmer farmer, long offeringId, int paid) {
        Long seasonId = jdbc().queryForObject("SELECT season_id FROM programme_offerings WHERE id = ?", Long.class, offeringId);
        Long obligationId = jdbc().queryForObject("""
                INSERT INTO contribution_obligations (farmer_id, season_id, amount_due, created_by)
                VALUES (?, ?, 1000, ?) RETURNING id""", Long.class, farmer.id(), seasonId, registrar.getId());
        if (paid > 0) {
            jdbc().update("""
                    INSERT INTO payments (obligation_id, transaction_reference, amount, method, status,
                                          provider_reference, requested_by, confirmed_by, confirmed_at)
                    VALUES (?, ?, ?, 'MOBILE_MONEY', 'CONFIRMED', ?, ?, ?, now())""",
                    obligationId, "TEST-" + UUID.randomUUID(), paid, "PROVIDER-" + UUID.randomUUID(),
                    registrar.getId(), registrar.getId());
        }
    }

    /** A farmer who meets every rule: enrolled, Active, 100% paid, distribution in 20 days. */
    private record Ready(TestFarmer farmer, long enrolmentId) {
    }

    private Ready readyFarmer() throws Exception {
        TestFarmer farmer = newFarmer();
        long offeringId = offering(20);
        long enrolmentId = enrolDirectly(farmer, offeringId);
        contribution(farmer, offeringId, 1000);
        return new Ready(farmer, enrolmentId);
    }

    private MvcResult requestPermit(String token, long enrolmentId) throws Exception {
        return mockMvc.perform(post("/api/enrolments/" + enrolmentId + "/permit").header("Authorization", bearer(token)))
                .andReturn();
    }

    private String permitNumber(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.permitNumber");
    }

    private List<String> outcomes(long enrolmentId) throws Exception {
        String body = mockMvc.perform(get("/api/permits/attempts?size=100&enrolmentId=" + enrolmentId)
                        .header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.content[*].outcome");
    }

    // ---------- issuing ----------

    @Test
    @DisplayName("All rules met: a permit with a unique number (201); asking again returns the same permit (200)")
    void issueAndRepeat() throws Exception {
        Ready ready = readyFarmer();

        MvcResult first = requestPermit(ready.farmer().token(), ready.enrolmentId());
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        String number = permitNumber(first);
        assertThat(number).matches(PERMIT_NUMBER);

        MvcResult again = requestPermit(ready.farmer().token(), ready.enrolmentId());
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(permitNumber(again)).isEqualTo(number);

        assertThat(outcomes(ready.enrolmentId())).containsExactlyInAnyOrder("ISSUED", "ALREADY_ISSUED");
        mockMvc.perform(get("/api/me/permits").header("Authorization", bearer(ready.farmer().token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].permitNumber", Matchers.hasItem(number)));
    }

    @Test
    @DisplayName("Refused (422) and recorded: not fully paid, no contribution, suspended, cancelled, distribution day")
    void refusalsAreRecorded() throws Exception {
        // 60% paid
        TestFarmer partly = newFarmer();
        long offering1 = offering(20);
        long partial = enrolDirectly(partly, offering1);
        contribution(partly, offering1, 600);
        mockMvc.perform(post("/api/enrolments/" + partial + "/permit").header("Authorization", bearer(partly.token())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("100%")));

        // no contribution recorded at all
        TestFarmer unpaid = newFarmer();
        long none = enrolDirectly(unpaid, offering(20));
        assertThat(requestPermit(unpaid.token(), none).getResponse().getStatus()).isEqualTo(422);

        // farmer suspended
        Ready suspended = readyFarmer();
        jdbc().update("UPDATE farmers SET status = 'SUSPENDED' WHERE id = ?", suspended.farmer().id());
        assertThat(requestPermit(suspended.farmer().token(), suspended.enrolmentId()).getResponse().getStatus()).isEqualTo(422);

        // enrolment cancelled
        Ready cancelled = readyFarmer();
        jdbc().update("UPDATE enrolments SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ? WHERE id = ?",
                registrar.getId(), cancelled.enrolmentId());
        assertThat(requestPermit(cancelled.farmer().token(), cancelled.enrolmentId()).getResponse().getStatus()).isEqualTo(422);

        // the distribution day is today: too late for a permit
        TestFarmer late = newFarmer();
        long offeringToday = offering(0);
        long lateEnrolment = enrolDirectly(late, offeringToday);
        contribution(late, offeringToday, 1000);
        assertThat(requestPermit(late.token(), lateEnrolment).getResponse().getStatus()).isEqualTo(422);

        for (long enrolmentId : List.of(partial, none, suspended.enrolmentId(), cancelled.enrolmentId(), lateEnrolment)) {
            assertThat(outcomes(enrolmentId)).as("attempts of enrolment " + enrolmentId).containsExactly("REFUSED");
        }
        assertThat(jdbc().queryForObject("SELECT count(*) FROM collection_permits WHERE enrolment_id IN (?, ?, ?, ?, ?)",
                Integer.class, partial, none, suspended.enrolmentId(), cancelled.enrolmentId(), lateEnrolment)).isZero();
        String reason = jdbc().queryForObject("SELECT reason FROM permit_attempts WHERE enrolment_id = ?", String.class, partial);
        assertThat(reason).contains("100%");
    }

    @Test
    @DisplayName("Another farmer's enrolment (403) and an unknown one (404) are refused AND recorded")
    void idorAndUnknownAreRecorded() throws Exception {
        Ready owner = readyFarmer();
        TestFarmer intruder = newFarmer();

        assertThat(requestPermit(intruder.token(), owner.enrolmentId()).getResponse().getStatus()).isEqualTo(403);
        assertThat(requestPermit(intruder.token(), 999_999_999L).getResponse().getStatus()).isEqualTo(404);
        assertThat(requestPermit(registrarToken, owner.enrolmentId()).getResponse().getStatus()).isEqualTo(403);

        assertThat(jdbc().queryForObject(
                "SELECT count(*) FROM permit_attempts WHERE farmer_id = ? AND outcome = 'REFUSED'",
                Integer.class, intruder.id())).isEqualTo(2);
        assertThat(outcomes(owner.enrolmentId())).containsExactly("REFUSED");   // the intruder's try is on record
    }

    @Test
    @DisplayName("Five requests at the same moment: one permit, one 201 and four 200")
    void concurrentRequestsGiveOnePermit() throws Exception {
        Ready ready = readyFarmer();
        int attempts = 5;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();
                return requestPermit(ready.farmer().token(), ready.enrolmentId()).getResponse().getStatus();
            }));
        }
        startTogether.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 200).hasSize(attempts - 1);
        assertThat(jdbc().queryForObject("SELECT count(*) FROM collection_permits WHERE enrolment_id = ?",
                Integer.class, ready.enrolmentId())).isEqualTo(1);
    }

    // ---------- verification at the store ----------

    @Test
    @DisplayName("Storekeeper verification: valid now; invalid after the enrolment is cancelled; farmers cannot verify")
    void verification() throws Exception {
        Ready ready = readyFarmer();
        String number = permitNumber(requestPermit(ready.farmer().token(), ready.enrolmentId()));

        mockMvc.perform(get("/api/permits/verify/" + number.toLowerCase()).header("Authorization", bearer(financeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.problems").isEmpty())
                .andExpect(jsonPath("$.data.permit.farmerId").value(ready.farmer().id()));

        jdbc().update("UPDATE enrolments SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ? WHERE id = ?",
                registrar.getId(), ready.enrolmentId());
        mockMvc.perform(get("/api/permits/verify/" + number).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.problems[0]").value(Matchers.containsString("cancelled")));

        mockMvc.perform(get("/api/permits/verify/" + number).header("Authorization", bearer(ready.farmer().token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/permits/verify/CP-AAAA-AAAA-AAAA").header("Authorization", bearer(financeToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/permits/attempts").header("Authorization", bearer(ready.farmer().token())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Permits and attempts are permanent: the database refuses to change or delete them")
    void permanentRecords() throws Exception {
        Ready ready = readyFarmer();
        requestPermit(ready.farmer().token(), ready.enrolmentId());

        assertThatThrownBy(() -> jdbc().update("DELETE FROM collection_permits WHERE enrolment_id = ?", ready.enrolmentId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("permanent");
        assertThatThrownBy(() -> jdbc().update("UPDATE permit_attempts SET outcome = 'REFUSED', reason = 'x', permit_id = NULL "
                + "WHERE enrolment_id = ?", ready.enrolmentId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("permanent");
    }
}
