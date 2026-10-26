package com.acms.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.repository.AppUserRepository;
import com.acms.repository.DistrictRepository;
import com.acms.repository.FarmerRepository;
import com.jayway.jsonpath.JsonPath;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared set-up for every integration test.
 *  - @SpringBootTest starts the whole application (security, services, database) without a real web server.
 *  - MockMvc sends fake HTTP requests through the real security filters and controllers.
 *  - The "test" profile points at the separate acms_test database.
 *  - JavaMailSender is replaced by a Mockito mock: emails are captured instead of sent.
 * All test classes extend this one, so Spring starts the application once and reuses it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Test-Password-2026!";

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired protected MockMvc mockMvc;
    @Autowired protected AppUserRepository appUserRepository;
    @Autowired protected DistrictRepository districtRepository;
    @Autowired protected FarmerRepository farmerRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected DataSource dataSource;

    @MockitoBean protected JavaMailSender mailSender;

    /** Safety net: refuse to run anywhere except the test database. */
    @BeforeAll
    void checkThisIsTheTestDatabase() {
        String database = new JdbcTemplate(dataSource).queryForObject("SELECT current_database()", String.class);
        assertThat(database)
                .as("Tests must run against acms_test, never against your real database")
                .isEqualTo("acms_test");
    }

    // ---------- test data ----------

    /** A username nobody has used yet, so tests never collide with each other or with older runs. */
    protected static String unique(String prefix) {
        return prefix + "." + (System.currentTimeMillis() % 1_000_000) + COUNTER.incrementAndGet();
    }

    protected static String nationalId() {
        StringBuilder id = new StringBuilder("1");
        for (int i = 0; i < 15; i++) {
            id.append(RANDOM.nextInt(10));
        }
        return id.toString();
    }

    /** A ready-to-use account: email verified, no forced password change, password = PASSWORD. */
    protected AppUser createUser(String username, Role role, Long districtId, Long farmerId) {
        AppUser user = new AppUser(username, username + "@example.test", passwordEncoder.encode(PASSWORD), role);
        user.markEmailVerified();
        user.changePasswordHash(passwordEncoder.encode(PASSWORD));   // also clears "must change password"
        if (districtId != null) {
            user.assignDistrict(districtRepository.findById(districtId).orElseThrow());
        }
        if (farmerId != null) {
            user.assignFarmer(farmerRepository.findById(farmerId).orElseThrow());
        }
        return appUserRepository.save(user);
    }

    /** Two districts, each with one cooperative and one farmer, created through the real API. */
    protected record World(long districtA, long districtB, long cooperativeA, long cooperativeB,
                           long farmerA, long farmerB) {
    }

    protected World createWorld(String registrarToken) throws Exception {
        long districtA = postForId("/api/districts", registrarToken, json("name", unique("district-a")));
        long districtB = postForId("/api/districts", registrarToken, json("name", unique("district-b")));
        long cooperativeA = postForId("/api/cooperatives", registrarToken,
                json("name", unique("coop-a"), "districtId", districtA));
        long cooperativeB = postForId("/api/cooperatives", registrarToken,
                json("name", unique("coop-b"), "districtId", districtB));
        long farmerA = postForId("/api/farmers", registrarToken,
                json("fullName", "Test Farmer A", "nationalId", nationalId(), "cooperativeId", cooperativeA));
        long farmerB = postForId("/api/farmers", registrarToken,
                json("fullName", "Test Farmer B", "nationalId", nationalId(), "cooperativeId", cooperativeB));
        return new World(districtA, districtB, cooperativeA, cooperativeB, farmerA, farmerB);
    }

    // ---------- HTTP helpers ----------

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected long postForId(String url, String token, String jsonBody) throws Exception {
        String body = mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");
        return id.longValue();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** json("a", "x", "b", 5) gives {"a":"x","b":5}. Numbers and booleans are not quoted. */
    protected static String json(Object... keysAndValues) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(keysAndValues[i]).append("\":");
            Object value = keysAndValues[i + 1];
            if (value == null || value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append('"').append(value.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
        }
        return json.append('}').toString();
    }

    // ---------- captured emails ----------

    /** The last email "sent" to this address during the current test. */
    protected SimpleMailMessage lastEmailTo(String address) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        List<SimpleMailMessage> toAddress = captor.getAllValues().stream()
                .filter(message -> message.getTo() != null && Arrays.asList(message.getTo()).contains(address))
                .toList();
        assertThat(toAddress).as("an email to " + address).isNotEmpty();
        return toAddress.get(toAddress.size() - 1);
    }

    protected static String extract(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("text matching " + regex).isTrue();
        return matcher.group(1);
    }

    protected static String oneTimePassword(SimpleMailMessage email) {
        return extract(email.getText(), "One-time password: (\\S+)");
    }

    protected static String verificationToken(SimpleMailMessage email) {
        return extract(email.getText(), "token=([A-Za-z0-9_-]+)");
    }
}
