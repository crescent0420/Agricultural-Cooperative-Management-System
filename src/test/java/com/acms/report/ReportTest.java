package com.acms.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Step 9: the management reports - what they count, how they round, that totals equal the sum of the rows,
 * and that district staff only ever see their own district (Field Officers: their own offerings).
 *
 * The test database keeps the data of earlier runs, so every test creates its own season or cooperative and
 * reads its own rows; it never assumes a report contains only its data.
 */
class ReportTest extends IntegrationTestBase {

    private World world;
    private AppUser registrar;
    private String registrarToken;
    private String financeToken;
    private AppUser officerA;
    private String officerAToken;
    private String otherOfficerAToken;
    private AppUser otherOfficerA;
    private String agronomistAToken;

    @BeforeAll
    void createWorldAndStaff() throws Exception {
        registrar = createUser(unique("report.registrar"), Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrar.getUsername(), PASSWORD);
        financeToken = login(createUser(unique("report.finance"), Role.FINANCE_OFFICER, null, null).getUsername(), PASSWORD);
        world = createWorld(registrarToken);
        officerA = createUser(unique("report.officer"), Role.FIELD_OFFICER, world.districtA(), null);
        officerAToken = login(officerA.getUsername(), PASSWORD);
        otherOfficerA = createUser(unique("report.officer2"), Role.FIELD_OFFICER, world.districtA(), null);
        otherOfficerAToken = login(otherOfficerA.getUsername(), PASSWORD);
        agronomistAToken = login(createUser(unique("report.agronomist"), Role.DISTRICT_AGRONOMIST, world.districtA(), null)
                .getUsername(), PASSWORD);
    }

    // ---------- 1. farmers per cooperative ----------

    @Test
    @DisplayName("Farmers per cooperative: counts per status, empty cooperatives listed, totals = sum of rows")
    void farmersPerCooperative() throws Exception {
        long cooperative = postForId("/api/cooperatives", registrarToken,
                json("name", unique("Report Coop"), "districtId", world.districtA()));
        long empty = postForId("/api/cooperatives", registrarToken,
                json("name", unique("Empty Coop"), "districtId", world.districtA()));
        newFarmer(cooperative);
        newFarmer(cooperative);
        long suspended = newFarmer(cooperative);
        jdbc().update("UPDATE farmers SET status = 'SUSPENDED' WHERE id = ?", suspended);

        String body = report(registrarToken, "/api/reports/farmers-per-cooperative?districtId=" + world.districtA());

        String row = "$.data.rows[?(@.cooperativeId == " + cooperative + ")]";
        assertThat(number(body, row + ".active")).isEqualTo(2);
        assertThat(number(body, row + ".suspended")).isEqualTo(1);
        assertThat(number(body, row + ".withdrawn")).isZero();
        assertThat(number(body, row + ".total")).isEqualTo(3);
        assertThat(number(body, "$.data.rows[?(@.cooperativeId == " + empty + ")].total")).isZero();

        List<Integer> districts = JsonPath.read(body, "$.data.rows[*].districtId");
        assertThat(districts).isNotEmpty().allMatch(id -> id == world.districtA());
        List<Integer> totals = JsonPath.read(body, "$.data.rows[*].total");
        assertThat(((Number) JsonPath.read(body, "$.data.totals.total")).longValue())
                .isEqualTo(totals.stream().mapToLong(Integer::longValue).sum());
        assertThat((String) JsonPath.read(body, "$.data.basis")).contains("cooperative");
    }

    @Test
    @DisplayName("District staff see only their own district: another district is 403; no permission is 403; unknown is 404")
    void districtScope() throws Exception {
        String body = report(agronomistAToken, "/api/reports/farmers-per-cooperative");
        assertThat(((Number) JsonPath.read(body, "$.data.scope.districtId")).longValue()).isEqualTo(world.districtA());
        List<Integer> districts = JsonPath.read(body, "$.data.rows[*].districtId");
        assertThat(districts).allMatch(id -> id == world.districtA());

        expectStatus(agronomistAToken, "/api/reports/farmers-per-cooperative?districtId=" + world.districtB(), 403);
        expectStatus(officerAToken, "/api/reports/offering-capacity?districtId=" + world.districtB(), 403);
        expectStatus(financeToken, "/api/reports/farmers-per-cooperative", 403);
        expectStatus(agronomistAToken, "/api/reports/outstanding-contributions", 403);
        expectStatus(registrarToken, "/api/reports/farmers-per-cooperative?districtId=999999999", 404);
        expectStatus(registrarToken, "/api/reports/offering-capacity?seasonId=999999999", 404);
    }

    // ---------- 2. enrolments and capacity ----------

    @Test
    @DisplayName("Capacity: cancelled enrolments free their place; 2 of 3 = 66.67%; a Field Officer sees only their offerings")
    void offeringCapacity() throws Exception {
        long seasonId = season();
        long offering = offering(seasonId, officerA, 3);
        long otherOffering = offering(seasonId, otherOfficerA, 2);
        enrol(offering, newFarmer(world.cooperativeA()));
        enrol(offering, newFarmer(world.cooperativeA()));
        long cancelled = enrol(offering, newFarmer(world.cooperativeA()));
        jdbc().update("UPDATE enrolments SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ? WHERE id = ?",
                registrar.getId(), cancelled);
        jdbc().update("UPDATE programme_offerings SET enrolled_count = enrolled_count - 1 WHERE id = ?", offering);

        String body = report(registrarToken, "/api/reports/offering-capacity?seasonId=" + seasonId);
        String row = "$.data.rows[?(@.offeringId == " + offering + ")]";
        assertThat(number(body, row + ".maxParticipants")).isEqualTo(3);
        assertThat(number(body, row + ".enrolled")).isEqualTo(2);
        assertThat(number(body, row + ".cancelled")).isEqualTo(1);
        assertThat(number(body, row + ".placesLeft")).isEqualTo(1);
        assertThat(decimal(body, row + ".capacityUsedPercent")).isEqualByComparingTo("66.67");
        assertThat(decimal(body, row + ".hectaresEnrolled")).isEqualByComparingTo("2.00");
        assertThat(number(body, "$.data.totals.offerings")).isEqualTo(2);
        assertThat(number(body, "$.data.totals.maxParticipants")).isEqualTo(5);
        assertThat(decimal(body, "$.data.totals.capacityUsedPercent")).isEqualByComparingTo("40.00");

        String officerView = report(officerAToken, "/api/reports/offering-capacity?seasonId=" + seasonId);
        List<Integer> offerings = JsonPath.read(officerView, "$.data.rows[*].offeringId");
        assertThat(offerings).extracting(Integer::longValue).containsExactly(offering);
        String otherView = report(otherOfficerAToken, "/api/reports/offering-capacity?seasonId=" + seasonId);
        List<Integer> others = JsonPath.read(otherView, "$.data.rows[*].offeringId");
        assertThat(others).extracting(Integer::longValue).containsExactly(otherOffering);
    }

    // ---------- 3. outstanding contributions ----------

    @Test
    @DisplayName("Outstanding: only CONFIRMED payments count; 1 400 of 3 000 = 46.66% (rounded down); per district")
    void outstandingContributions() throws Exception {
        long seasonId = season();
        contribution(newFarmer(world.cooperativeA()), seasonId, 1000, 1000, 0);    // fully paid
        contribution(newFarmer(world.cooperativeA()), seasonId, 1000, 400, 200);   // partly paid, 200 pending
        contribution(newFarmer(world.cooperativeA()), seasonId, 1000, 0, 0);       // nothing paid
        contribution(newFarmer(world.cooperativeB()), seasonId, 500, 500, 0);      // other district

        String body = report(financeToken, "/api/reports/outstanding-contributions?seasonId=" + seasonId);
        String row = "$.data.rows[?(@.districtId == " + world.districtA() + ")]";
        assertThat(number(body, row + ".contributions")).isEqualTo(3);
        assertThat(decimal(body, row + ".amountDue")).isEqualByComparingTo("3000.00");
        assertThat(decimal(body, row + ".confirmedPaid")).isEqualByComparingTo("1400.00");
        assertThat(decimal(body, row + ".pendingPayments")).isEqualByComparingTo("200.00");
        assertThat(decimal(body, row + ".outstanding")).isEqualByComparingTo("1600.00");
        assertThat(decimal(body, row + ".collectedPercent")).isEqualByComparingTo("46.66");
        assertThat(number(body, row + ".fullyPaid")).isEqualTo(1);
        assertThat(number(body, row + ".partlyPaid")).isEqualTo(1);
        assertThat(number(body, row + ".unpaid")).isEqualTo(1);

        assertThat(number(body, "$.data.totals.contributions")).isEqualTo(4);
        assertThat(decimal(body, "$.data.totals.amountDue")).isEqualByComparingTo("3500.00");
        assertThat(decimal(body, "$.data.totals.outstanding")).isEqualByComparingTo("1600.00");
        assertThat(decimal(body, "$.data.totals.collectedPercent")).isEqualByComparingTo("54.28");

        String districtB = report(registrarToken,
                "/api/reports/outstanding-contributions?seasonId=" + seasonId + "&districtId=" + world.districtB());
        List<Integer> districts = JsonPath.read(districtB, "$.data.rows[*].districtId");
        assertThat(districts).extracting(Integer::longValue).containsExactly(world.districtB());
    }

    // ---------- 4. grade distribution ----------

    @Test
    @DisplayName("Grades: only PUBLISHED results count; A 2 (66.67%), C 1 (33.33%), average 80.00, 1 not yet published")
    void gradeDistribution() throws Exception {
        long seasonId = season();
        long offering = offering(seasonId, officerA, 10);
        publishedResult(enrol(offering, newFarmer(world.cooperativeA())), 85, "A");
        publishedResult(enrol(offering, newFarmer(world.cooperativeA())), 90, "A");
        publishedResult(enrol(offering, newFarmer(world.cooperativeA())), 65, "C");
        long draft = enrol(offering, newFarmer(world.cooperativeA()));
        jdbc().update("INSERT INTO inspection_results (enrolment_id, created_by) VALUES (?, ?)", draft, officerA.getId());

        String body = report(agronomistAToken, "/api/reports/grade-distribution?seasonId=" + seasonId);
        String row = "$.data.rows[?(@.offeringId == " + offering + ")]";
        assertThat(number(body, row + ".activeEnrolments")).isEqualTo(4);
        assertThat(number(body, row + ".published")).isEqualTo(3);
        assertThat(number(body, row + ".notYetPublished")).isEqualTo(1);
        assertThat(decimal(body, row + ".averageFinalScore")).isEqualByComparingTo("80.00");
        assertThat(number(body, row + ".grades[?(@.grade == 'A')].count")).isEqualTo(2);
        assertThat(decimal(body, row + ".grades[?(@.grade == 'A')].percent")).isEqualByComparingTo("66.67");
        assertThat(number(body, row + ".grades[?(@.grade == 'C')].count")).isEqualTo(1);
        assertThat(decimal(body, row + ".grades[?(@.grade == 'C')].percent")).isEqualByComparingTo("33.33");
        assertThat(number(body, "$.data.totals.published")).isEqualTo(3);

        String otherOfficer = report(otherOfficerAToken, "/api/reports/grade-distribution?seasonId=" + seasonId);
        List<Object> rows = JsonPath.read(otherOfficer, "$.data.rows");
        assertThat(rows).isEmpty();
    }

    // ---------- set-up shortcuts (the business rules themselves are tested in their own classes) ----------

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    private long newFarmer(long cooperativeId) throws Exception {
        return postForId("/api/farmers", registrarToken,
                json("fullName", "Report Farmer", "nationalId", nationalId(), "cooperativeId", cooperativeId));
    }

    private long season() throws Exception {
        LocalDate today = LocalDate.now();
        return postForId("/api/seasons", registrarToken, json(
                "name", unique("Report Season"), "startsOn", today.minusDays(10).toString(),
                "endsOn", today.plusDays(60).toString(), "maxLandHectares", 5,
                "distributionDate", today.plusDays(20).toString()));
    }

    private long offering(long seasonId, AppUser officer, int maxParticipants) throws Exception {
        long programmeId = postForId("/api/programmes", registrarToken,
                json("name", unique("Sorghum"), "districtId", world.districtA()));
        return postForId("/api/offerings", registrarToken, json(
                "programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officer.getId(),
                "hectaresPerParticipant", 1.0, "maxParticipants", maxParticipants));
    }

    private long enrol(long offeringId, long farmerId) {
        Long id = jdbc().queryForObject("""
                INSERT INTO enrolments (offering_id, farmer_id, hectares, enrolled_by)
                VALUES (?, ?, 1.0, ?) RETURNING id""", Long.class, offeringId, farmerId, registrar.getId());
        jdbc().update("UPDATE programme_offerings SET enrolled_count = enrolled_count + 1 WHERE id = ?", offeringId);
        return id;
    }

    private void contribution(long farmerId, long seasonId, int due, int confirmed, int pending) {
        Long obligationId = jdbc().queryForObject("""
                INSERT INTO contribution_obligations (farmer_id, season_id, amount_due, created_by)
                VALUES (?, ?, ?, ?) RETURNING id""", Long.class, farmerId, seasonId, due, registrar.getId());
        if (confirmed > 0) {
            jdbc().update("""
                    INSERT INTO payments (obligation_id, transaction_reference, amount, method, status,
                                          provider_reference, requested_by, confirmed_by, confirmed_at)
                    VALUES (?, ?, ?, 'MOBILE_MONEY', 'CONFIRMED', ?, ?, ?, now())""",
                    obligationId, "TEST-" + UUID.randomUUID(), confirmed, "PROVIDER-" + UUID.randomUUID(),
                    registrar.getId(), registrar.getId());
        }
        if (pending > 0) {
            jdbc().update("""
                    INSERT INTO payments (obligation_id, transaction_reference, amount, method, requested_by)
                    VALUES (?, ?, ?, 'MOBILE_MONEY', ?)""",
                    obligationId, "TEST-" + UUID.randomUUID(), pending, registrar.getId());
        }
    }

    private void publishedResult(long enrolmentId, int finalScore, String grade) {
        jdbc().update("""
                INSERT INTO inspection_results (enrolment_id, status, final_score, grade, created_by,
                                                submitted_by, submitted_at, published_by, published_at)
                VALUES (?, 'PUBLISHED', ?, ?, ?, ?, now(), ?, now())""",
                enrolmentId, finalScore, grade, officerA.getId(), officerA.getId(), registrar.getId());
    }

    // ---------- reading the reports ----------

    private String report(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void expectStatus(String token, String url, int expected) throws Exception {
        mockMvc.perform(get(url).header("Authorization", bearer(token))).andExpect(status().is(expected));
    }

    /** A filter path ([?(...)]) gives a list: this returns its single value. */
    private static Object single(String body, String path) {
        Object value = JsonPath.read(body, path);
        if (value instanceof List<?> list) {
            assertThat(list).as(path).hasSize(1);
            return list.get(0);
        }
        return value;
    }

    private static long number(String body, String path) {
        return ((Number) single(body, path)).longValue();
    }

    private static BigDecimal decimal(String body, String path) {
        return new BigDecimal(String.valueOf(single(body, path)));
    }
}
