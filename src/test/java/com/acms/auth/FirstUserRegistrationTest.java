package com.acms.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.acms.dto.RegisterRequest;
import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.service.AccountOnboardingService;
import com.acms.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Supervisor rule: the first registered user becomes System Administrator, everyone after that is a FARMER.
 * Runs FIRST (@Order(1)) because it needs an empty users table.
 */
@Order(1)
class FirstUserRegistrationTest extends IntegrationTestBase {

    @Autowired
    private AccountOnboardingService onboardingService;

    @BeforeAll
    void emptyTheUsersTable() {
        // TRUNCATE ... CASCADE also empties tables that point at app_users (verification links,
        // training records). Safe only because the base class has checked this is acms_test.
        new JdbcTemplate(dataSource).execute("TRUNCATE TABLE app_users RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("Five people register at the same moment on an empty system: exactly one becomes admin")
    void exactlyOneAdministratorEvenUnderConcurrentRegistration() throws Exception {
        int people = 5;
        ExecutorService pool = Executors.newFixedThreadPool(people);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < people; i++) {
            String username = unique("first");
            results.add(pool.submit(() -> {
                startTogether.await();   // every thread waits here, then all are released at once
                return onboardingService.register(new RegisterRequest(username, username + "@example.test"));
            }));
        }
        startTogether.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);   // re-throws if any registration failed
        }
        pool.shutdown();

        List<AppUser> users = appUserRepository.findAll();
        assertThat(users).hasSize(people);
        assertThat(users).filteredOn(u -> u.getRole() == Role.SYSTEM_ADMIN).hasSize(1);
        assertThat(users).filteredOn(u -> u.getRole() == Role.FARMER).hasSize(people - 1);
    }
}
