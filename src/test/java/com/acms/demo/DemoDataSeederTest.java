package com.acms.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.config.DemoDataSeeder;
import com.acms.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Step 10: the demo accounts the README lists - created only on an empty system, all usable at once. */
@Order(2)
class DemoDataSeederTest extends IntegrationTestBase {

    private static final String DEMO_PASSWORD = "Demo-Password-2026!";

    @Autowired
    private DemoDataSeeder seeder;

    @BeforeAll
    void emptyTheSystem() {
        // Safe only because the base class has checked this is acms_test (see FirstUserRegistrationTest).
        new JdbcTemplate(dataSource).execute(
                "TRUNCATE TABLE app_users, cooperatives, permit_attempts, audit_log RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("Empty system: one account per role, the admin first; every account can log in at once; "
            + "running it again changes nothing; a short password is refused")
    void seedsAnEmptySystemOnce() throws Exception {
        assertThat(seeder.seed(DEMO_PASSWORD)).containsExactly(
                "demo.admin", "demo.registrar", "demo.finance", "demo.officer", "demo.agronomist",
                "demo.officer.musanze", "demo.farmer", "demo.farmer.musanze");

        // the first account is the System Admin, so the first-user rule still holds
        assertThat(new JdbcTemplate(dataSource).queryForObject(
                "SELECT role FROM app_users ORDER BY id LIMIT 1", String.class)).isEqualTo("SYSTEM_ADMIN");

        // no forced password change: the demo can start straight away
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "demo.registrar", "password", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));

        String officer = login("demo.officer", DEMO_PASSWORD);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(officer)))
                .andExpect(jsonPath("$.data.role").value("FIELD_OFFICER"))
                .andExpect(jsonPath("$.data.districtId").value((int) seededDistrictId("Huye")));

        // the farmer account is linked to a farmer registered the normal way (real membership number)
        String farmer = login("demo.farmer", DEMO_PASSWORD);
        mockMvc.perform(get("/api/me/profile").header("Authorization", bearer(farmer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Aline Uwase"))
                .andExpect(jsonPath("$.data.membershipNumber").value(org.hamcrest.Matchers.startsWith("RCU-")));

        // the system is no longer empty: a second run (for example the next start-up) does nothing
        assertThat(seeder.seed(DEMO_PASSWORD)).isEmpty();
        assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT count(*) FROM app_users", Integer.class))
                .isEqualTo(8);

        assertThatThrownBy(() -> seeder.seed("short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_SEED_PASSWORD");
    }
}
