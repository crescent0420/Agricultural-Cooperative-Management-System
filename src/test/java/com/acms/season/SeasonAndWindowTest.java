package com.acms.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.exception.ConflictException;
import com.acms.repository.EnrolmentWindowRepository;
import com.acms.service.SeasonService;
import com.acms.support.IntegrationTestBase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

/** Step 5a: seasons, and the enrolment windows that only the Union Registrar opens and closes. */
class SeasonAndWindowTest extends IntegrationTestBase {

    @Autowired private SeasonService seasonService;
    @Autowired private EnrolmentWindowRepository windowRepository;

    private AppUser registrar;
    private String registrarToken;
    private String farmerToken;

    @BeforeAll
    void createUsers() throws Exception {
        String registrarName = unique("season.registrar");
        String farmerName = unique("season.farmer");
        registrar = createUser(registrarName, Role.UNION_REGISTRAR, null, null);
        createUser(farmerName, Role.FARMER, null, null);
        registrarToken = login(registrarName, PASSWORD);
        farmerToken = login(farmerName, PASSWORD);
    }

    /** A season that runs from today for 90 days. */
    private String seasonJson(String name) {
        LocalDate today = LocalDate.now();
        return json("name", name, "startsOn", today.toString(), "endsOn", today.plusDays(90).toString(),
                "maxLandHectares", 2.5, "distributionDate", today.plusDays(30).toString());
    }

    private long createSeason(String name) throws Exception {
        return postForId("/api/seasons", registrarToken, seasonJson(name));
    }

    @Test
    @DisplayName("Registrar creates a season; every role can read it; enrolment starts closed")
    void createAndRead() throws Exception {
        long id = createSeason(unique("Season A"));

        mockMvc.perform(get("/api/seasons/" + id).header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxLandHectares").value(2.5))
                .andExpect(jsonPath("$.data.enrolmentOpen").value(false));
        mockMvc.perform(get("/api/seasons").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Invalid seasons are refused: bad dates (400), duplicate name (409), non-registrar (403)")
    void validation() throws Exception {
        LocalDate today = LocalDate.now();
        String endsBeforeStart = json("name", unique("bad1"), "startsOn", today.toString(),
                "endsOn", today.minusDays(1).toString(), "maxLandHectares", 1, "distributionDate", today.toString());
        String distributionOutside = json("name", unique("bad2"), "startsOn", today.toString(),
                "endsOn", today.plusDays(10).toString(), "maxLandHectares", 1,
                "distributionDate", today.plusDays(20).toString());
        String zeroLand = json("name", unique("bad3"), "startsOn", today.toString(),
                "endsOn", today.plusDays(10).toString(), "maxLandHectares", 0, "distributionDate", today.toString());

        for (String body : List.of(endsBeforeStart, distributionOutside, zeroLand)) {
            mockMvc.perform(post("/api/seasons").header("Authorization", bearer(registrarToken))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        String name = unique("Season Dup");
        createSeason(name);
        mockMvc.perform(post("/api/seasons").header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(seasonJson(name.toUpperCase())))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/seasons").header("Authorization", bearer(farmerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(seasonJson(unique("farmer try"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Open, open again (409), close, close again (409), reopen: history keeps both windows")
    void windowLifecycle() throws Exception {
        long id = createSeason(unique("Season W"));
        String open = "/api/seasons/" + id + "/windows/open";
        String close = "/api/seasons/" + id + "/windows/close";

        mockMvc.perform(post(open).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openedBy").value(registrar.getUsername()));
        mockMvc.perform(get("/api/seasons/" + id).header("Authorization", bearer(farmerToken)))
                .andExpect(jsonPath("$.data.enrolmentOpen").value(true));
        mockMvc.perform(post(open).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(post(close).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
        mockMvc.perform(post(close).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(post(open).header("Authorization", bearer(registrarToken)))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/seasons/" + id + "/windows").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("Only the Registrar may open a window (403); a season cannot change once a window opened (409)")
    void permissionsAndFreeze() throws Exception {
        long id = createSeason(unique("Season P"));
        mockMvc.perform(post("/api/seasons/" + id + "/windows/open").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/seasons/" + id).header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(seasonJson(unique("Season P renamed"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/seasons/" + id + "/windows/open").header("Authorization", bearer(registrarToken)))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/seasons/" + id).header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(seasonJson(unique("too late"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A window cannot be opened for a season that has already ended (422)")
    void endedSeason() throws Exception {
        LocalDate today = LocalDate.now();
        long id = postForId("/api/seasons", registrarToken, json("name", unique("Old season"),
                "startsOn", today.minusDays(100).toString(), "endsOn", today.minusDays(10).toString(),
                "maxLandHectares", 1, "distributionDate", today.minusDays(50).toString()));
        mockMvc.perform(post("/api/seasons/" + id + "/windows/open").header("Authorization", bearer(registrarToken)))
                .andExpect(status().is(422));
    }

    @Test
    @DisplayName("Two registrars open the same season's window at the same moment: exactly one window opens")
    void concurrentOpenGivesExactlyOneWindow() throws Exception {
        long id = createSeason(unique("Season C"));
        int attempts = 4;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startTogether.await();
                return seasonService.openWindow(id, registrar.getId());
            }));
        }
        startTogether.countDown();

        int opened = 0;
        int refused = 0;
        for (Future<?> result : results) {
            try {
                result.get(30, TimeUnit.SECONDS);
                opened++;
            } catch (ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOfAny(ConflictException.class, DataIntegrityViolationException.class);
                refused++;
            }
        }
        pool.shutdown();

        assertThat(opened).isEqualTo(1);
        assertThat(refused).isEqualTo(attempts - 1);
        assertThat(windowRepository.findBySeasonId(id, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements()).isEqualTo(1);
    }
}
