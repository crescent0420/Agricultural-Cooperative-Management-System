package com.acms.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** Step 6: contributions, payments (idempotent, confirmed), balances, permanent history and the 30% rule. */
class ContributionPaymentTest extends IntegrationTestBase {

    private World world;
    private String registrarToken;
    private String financeToken;
    private String farmerAToken;   // the account of world.farmerA()
    private String farmerBToken;   // the account of world.farmerB()
    private AppUser officerA;

    @BeforeAll
    void createWorldAndUsers() throws Exception {
        String registrarName = unique("pay.registrar");
        createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        registrarToken = login(registrarName, PASSWORD);
        String financeName = unique("pay.finance");
        createUser(financeName, Role.FINANCE_OFFICER, null, null);
        financeToken = login(financeName, PASSWORD);
        world = createWorld(registrarToken);

        officerA = createUser(unique("pay.officer.a"), Role.FIELD_OFFICER, world.districtA(), null);
        farmerAToken = login(createUser(unique("pay.farmer.a"), Role.FARMER, null, world.farmerA()).getUsername(), PASSWORD);
        farmerBToken = login(createUser(unique("pay.farmer.b"), Role.FARMER, null, world.farmerB()).getUsername(), PASSWORD);
    }

    // ---------- helpers ----------

    private long season(boolean openWindow) throws Exception {
        LocalDate today = LocalDate.now();
        long seasonId = postForId("/api/seasons", registrarToken, json(
                "name", unique("Pay Season"), "startsOn", today.toString(),
                "endsOn", today.plusDays(90).toString(), "maxLandHectares", 2.5,
                "distributionDate", today.plusDays(30).toString()));
        if (openWindow) {
            mockMvc.perform(post("/api/seasons/" + seasonId + "/windows/open")
                    .header("Authorization", bearer(registrarToken))).andExpect(status().isCreated());
        }
        return seasonId;
    }

    /** A new farmer in cooperative A, so each test has a farmer without any contribution yet. */
    private long newFarmerA() throws Exception {
        return postForId("/api/farmers", registrarToken,
                json("fullName", "Paying Farmer", "nationalId", nationalId(), "cooperativeId", world.cooperativeA()));
    }

    private long contribution(long farmerId, long seasonId, double amount) throws Exception {
        return postForId("/api/contributions", financeToken,
                json("farmerId", farmerId, "seasonId", seasonId, "amountDue", amount));
    }

    private String paymentJson(String reference, long contributionId, double amount) {
        return json("transactionReference", reference, "contributionId", contributionId,
                "amount", amount, "method", "MOBILE_MONEY");
    }

    private MvcResult requestPayment(String body) throws Exception {
        return mockMvc.perform(post("/api/payments").header("Authorization", bearer(financeToken))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private long paymentId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** Requests and confirms a payment; returns its id. */
    private long pay(long contributionId, double amount) throws Exception {
        MvcResult result = requestPayment(paymentJson(reference(), contributionId, amount));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        long id = paymentId(result);
        confirm(id, "PROV-" + UUID.randomUUID()).andExpect(status().isOk());
        return id;
    }

    private org.springframework.test.web.servlet.ResultActions confirm(long paymentId, String providerReference)
            throws Exception {
        return mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                .header("Authorization", bearer(financeToken))
                .contentType(MediaType.APPLICATION_JSON).content(json("providerReference", providerReference)));
    }

    private static String reference() {
        return "TX-" + UUID.randomUUID();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    private int enrol(String token, long offeringId) throws Exception {
        return mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments")
                .header("Authorization", bearer(token))).andReturn().getResponse().getStatus();
    }

    // ---------- contributions ----------

    @Test
    @DisplayName("Finance Officer records a contribution (201); duplicate (409); other roles (403); bad input (400/404)")
    void createContribution() throws Exception {
        long seasonId = season(false);
        long farmerId = newFarmerA();
        long id = contribution(farmerId, seasonId, 10000);

        mockMvc.perform(get("/api/contributions/" + id).header("Authorization", bearer(financeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue").value(10000.0))
                .andExpect(jsonPath("$.outstanding").value(10000.0))
                .andExpect(jsonPath("$.paidPercent").value(0.0))
                .andExpect(jsonPath("$.enrolmentAllowed").value(false));

        String again = json("farmerId", farmerId, "seasonId", seasonId, "amountDue", 5000);
        for (String[] attempt : new String[][] {{financeToken, "409"}, {registrarToken, "403"}, {farmerAToken, "403"}}) {
            mockMvc.perform(post("/api/contributions").header("Authorization", bearer(attempt[0]))
                            .contentType(MediaType.APPLICATION_JSON).content(again))
                    .andExpect(status().is(Integer.parseInt(attempt[1])));
        }
        mockMvc.perform(post("/api/contributions").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("farmerId", farmerId, "seasonId", seasonId, "amountDue", 0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/contributions").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("farmerId", 999_999_999L, "seasonId", seasonId, "amountDue", 100)))
                .andExpect(status().isNotFound());
    }

    // ---------- payments ----------

    @Test
    @DisplayName("A payment counts only once confirmed; confirming twice with the same reference is harmless")
    void paymentCountsOnlyWhenConfirmed() throws Exception {
        long id = contribution(newFarmerA(), season(false), 10000);
        MvcResult requested = requestPayment(paymentJson(reference(), id, 4000));
        assertThat(requested.getResponse().getStatus()).isEqualTo(201);
        long paymentId = paymentId(requested);

        mockMvc.perform(get("/api/contributions/" + id).header("Authorization", bearer(financeToken)))
                .andExpect(jsonPath("$.confirmedPaid").value(0.0))
                .andExpect(jsonPath("$.pendingAmount").value(4000.0))
                .andExpect(jsonPath("$.outstanding").value(10000.0));

        // Provider references are unique in the whole database, so every test run uses new ones.
        String providerReference = "MTN-" + UUID.randomUUID();
        confirm(paymentId, providerReference).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/contributions/" + id).header("Authorization", bearer(financeToken)))
                .andExpect(jsonPath("$.confirmedPaid").value(4000.0))
                .andExpect(jsonPath("$.outstanding").value(6000.0))
                .andExpect(jsonPath("$.paidPercent").value(40.0))
                .andExpect(jsonPath("$.enrolmentAllowed").value(true))
                .andExpect(jsonPath("$.inputCollectionAllowed").value(false));

        confirm(paymentId, providerReference).andExpect(status().isOk());          // the operator resent it
        confirm(paymentId, "MTN-" + UUID.randomUUID()).andExpect(status().isConflict());   // a different one
        mockMvc.perform(post("/api/payments/" + paymentId + "/fail").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("reason", "too late")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("The same payment request sent again returns the original (200) and creates nothing new")
    void retryNeverCreatesASecondPayment() throws Exception {
        long id = contribution(newFarmerA(), season(false), 10000);
        String reference = reference();
        String body = paymentJson(reference, id, 2500);

        MvcResult first = requestPayment(body);
        MvcResult retry = requestPayment(body);   // "no reply because of a network failure", so the client resends

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(retry.getResponse().getStatus()).isEqualTo(200);
        assertThat(paymentId(retry)).isEqualTo(paymentId(first));
        assertThat(jdbc().queryForObject("SELECT count(*) FROM payments WHERE transaction_reference = ?",
                Integer.class, reference)).isEqualTo(1);

        // Reusing the reference for a DIFFERENT payment is an error, not a retry.
        assertThat(requestPayment(paymentJson(reference, id, 3000)).getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("Six identical retries arriving at the same moment still create exactly one payment")
    void concurrentRetriesCreateOnePayment() throws Exception {
        long id = contribution(newFarmerA(), season(false), 10000);
        String reference = reference();
        String body = paymentJson(reference, id, 1000);
        int attempts = 6;

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();
                return requestPayment(body).getResponse().getStatus();
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
        assertThat(jdbc().queryForObject("SELECT count(*) FROM payments WHERE transaction_reference = ?",
                Integer.class, reference)).isEqualTo(1);
    }

    @Test
    @DisplayName("No overpayment: pending payments count against what can still be requested (422)")
    void noOverpayment() throws Exception {
        long id = contribution(newFarmerA(), season(false), 1000);
        MvcResult first = requestPayment(paymentJson(reference(), id, 600));
        assertThat(first.getResponse().getStatus()).isEqualTo(201);

        assertThat(requestPayment(paymentJson(reference(), id, 500)).getResponse().getStatus()).isEqualTo(422);

        // After the first one fails, its 600 is free again.
        mockMvc.perform(post("/api/payments/" + paymentId(first) + "/fail").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("reason", "Insufficient mobile money balance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
        assertThat(requestPayment(paymentJson(reference(), id, 500)).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("A failed payment never counts and can never be confirmed (409); failing needs a reason (400)")
    void failedPayment() throws Exception {
        long id = contribution(newFarmerA(), season(false), 1000);
        long paymentId = paymentId(requestPayment(paymentJson(reference(), id, 400)));

        mockMvc.perform(post("/api/payments/" + paymentId + "/fail").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/payments/" + paymentId + "/fail").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("reason", "Bank rejected the transfer")))
                .andExpect(status().isOk());
        confirm(paymentId, "BK-" + UUID.randomUUID()).andExpect(status().isConflict());

        mockMvc.perform(get("/api/contributions/" + id).header("Authorization", bearer(financeToken)))
                .andExpect(jsonPath("$.confirmedPaid").value(0.0))
                .andExpect(jsonPath("$.outstanding").value(1000.0));
    }

    // ---------- permanent history ----------

    @Test
    @DisplayName("Financial history cannot be deleted or rewritten, even with SQL run directly")
    void historyIsPermanent() throws Exception {
        long id = contribution(newFarmerA(), season(false), 1000);
        long paymentId = pay(id, 300);

        assertThatThrownBy(() -> jdbc().update("UPDATE payments SET amount = 1 WHERE id = ?", paymentId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("can never change");
        assertThatThrownBy(() -> jdbc().update("UPDATE payments SET status = 'FAILED', failed_at = now(), "
                + "failed_by = requested_by, failure_reason = 'x', confirmed_at = NULL, confirmed_by = NULL, "
                + "provider_reference = NULL WHERE id = ?", paymentId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("can no longer change");
        assertThatThrownBy(() -> jdbc().update("DELETE FROM payments WHERE id = ?", paymentId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("can never be deleted");
        assertThatThrownBy(() -> jdbc().update("DELETE FROM contribution_obligations WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("can never be deleted");
    }

    @Test
    @DisplayName("Adjusting the amount owed keeps the old value, new value, reason and author; never below what was paid")
    void adjustments() throws Exception {
        long id = contribution(newFarmerA(), season(false), 10000);
        pay(id, 4000);
        String url = "/api/contributions/" + id + "/adjustments";

        mockMvc.perform(post(url).header("Authorization", bearer(financeToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("newAmountDue", 8000, "reason", "Fertiliser price reduced")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue").value(8000.0))
                .andExpect(jsonPath("$.outstanding").value(4000.0))
                .andExpect(jsonPath("$.paidPercent").value(50.0));
        mockMvc.perform(get(url).header("Authorization", bearer(financeToken)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].oldAmount").value(10000.0))
                .andExpect(jsonPath("$.content[0].newAmount").value(8000.0))
                .andExpect(jsonPath("$.content[0].reason").value("Fertiliser price reduced"));

        String[][] refused = {
                {json("newAmountDue", 3000, "reason", "below the 4000 paid"), "422"},
                {json("newAmountDue", 8000, "reason", "no change"), "400"},
                {json("newAmountDue", 9000), "400"}};
        for (String[] attempt : refused) {
            mockMvc.perform(post(url).header("Authorization", bearer(financeToken))
                            .contentType(MediaType.APPLICATION_JSON).content(attempt[0]))
                    .andExpect(status().is(Integer.parseInt(attempt[1])));
        }
        mockMvc.perform(post(url).header("Authorization", bearer(registrarToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("newAmountDue", 9000, "reason", "registrar try")))
                .andExpect(status().isForbidden());
    }

    // ---------- who sees and changes what ----------

    @Test
    @DisplayName("Farmers see only their own balance; Finance Officers cannot touch field or farmer data")
    void accessRules() throws Exception {
        long seasonId = season(false);
        long ownId = contribution(world.farmerA(), seasonId, 1000);
        long otherId = contribution(world.farmerB(), seasonId, 1000);

        mockMvc.perform(get("/api/me/contributions?seasonId=" + seasonId).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownId));
        mockMvc.perform(get("/api/contributions/" + ownId).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/contributions/" + otherId).header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isForbidden());   // changing the id in the URL does not help (IDOR)
        mockMvc.perform(get("/api/contributions").header("Authorization", bearer(farmerAToken)))
                .andExpect(status().isForbidden());
        assertThat(requestPaymentAs(farmerAToken, paymentJson(reference(), ownId, 100))).isEqualTo(403);

        // Finance Officers must not change field or quality data.
        mockMvc.perform(patch("/api/farmers/" + world.farmerA() + "/status").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("status", "SUSPENDED")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/farmers/" + world.farmerA()).header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("fullName", "Changed")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/offerings").header("Authorization", bearer(financeToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private int requestPaymentAs(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/payments").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
    }

    // ---------- the 30% rule ----------

    @Test
    @DisplayName("Enrolment needs 30% CONFIRMED: none recorded, 29.99% and pending money are refused (422); 30% enrols")
    void thirtyPercentRule() throws Exception {
        long seasonId = season(true);
        long programmeId = postForId("/api/programmes", registrarToken,
                json("name", unique("Sorghum"), "districtId", world.districtA()));
        long offeringId = postForId("/api/offerings", registrarToken, json(
                "programmeId", programmeId, "seasonId", seasonId, "fieldOfficerId", officerA.getId(),
                "hectaresPerParticipant", 1.0, "maxParticipants", 10));

        // world.farmerA has no contribution for this new season yet.
        mockMvc.perform(post("/api/offerings/" + offeringId + "/enrolments").header("Authorization", bearer(farmerAToken)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No input contribution")));

        long id = contribution(world.farmerA(), seasonId, 10000);
        pay(id, 2999.99);                                                   // 29.99%
        assertThat(enrol(farmerAToken, offeringId)).isEqualTo(422);

        long pending = paymentId(requestPayment(paymentJson(reference(), id, 0.01)));
        assertThat(enrol(farmerAToken, offeringId)).isEqualTo(422);       // 30% only if the pending 0.01 counted

        confirm(pending, "MTN-" + UUID.randomUUID()).andExpect(status().isOk());
        assertThat(enrol(farmerAToken, offeringId)).isEqualTo(201);       // exactly 30.00% confirmed
    }
}
