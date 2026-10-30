package com.acms.config;

import com.acms.dto.FarmerRegistrationRequest;
import com.acms.entity.AppUser;
import com.acms.entity.Cooperative;
import com.acms.entity.District;
import com.acms.enums.AuditEventType;
import com.acms.enums.Role;
import com.acms.repository.AppUserRepository;
import com.acms.repository.CooperativeRepository;
import com.acms.repository.FarmerRepository;
import com.acms.service.AuditLogService;
import com.acms.service.FarmerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo accounts and data, for the demonstration and for a reviewer who starts the system for the first time.
 *
 * Safe by design:
 *  - OFF unless APP_DEMO_SEED=true in .env;
 *  - runs only on an EMPTY system (no users and no farmers), so it can never touch real data, and running
 *    the application again does nothing;
 *  - the password is never in the code: every demo account gets DEMO_SEED_PASSWORD from .env
 *    (at least 12 characters, like any password), and it is never written to the log;
 *  - all or nothing: one transaction; if anything fails, nothing is created.
 *
 * The first account created is the System Admin, so the "first user becomes admin" rule still holds.
 * Farmers are registered through FarmerService, acting as the demo registrar, so they get real membership
 * numbers and the normal validation. Demo accounts have a verified email (@acms.example, a domain reserved
 * for examples) and do not have to change their password at the first login, so the demo can start at once.
 */
@Component
@Order(2)   // after ReferenceDataSeeder: the demo accounts need the districts
public class DemoDataSeeder implements ApplicationRunner {

    public static final String EMAIL_DOMAIN = "acms.example";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** username, role, district (null = the whole Union) */
    private record Account(String username, Role role, String district) {
    }

    private static final List<Account> STAFF = List.of(
            new Account("demo.admin", Role.SYSTEM_ADMIN, null),          // first: becomes the System Admin
            new Account("demo.registrar", Role.UNION_REGISTRAR, null),
            new Account("demo.finance", Role.FINANCE_OFFICER, null),
            new Account("demo.officer", Role.FIELD_OFFICER, "Huye"),
            new Account("demo.agronomist", Role.DISTRICT_AGRONOMIST, "Huye"),
            new Account("demo.officer.musanze", Role.FIELD_OFFICER, "Musanze"));

    private final boolean enabled;
    private final String password;
    private final AppUserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;
    private final FarmerService farmerService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaction;

    @PersistenceContext
    private EntityManager entityManager;

    public DemoDataSeeder(@Value("${acms.demo-seed.enabled:false}") boolean enabled,
                          @Value("${acms.demo-seed.password:}") String password,
                          AppUserRepository userRepository,
                          FarmerRepository farmerRepository,
                          CooperativeRepository cooperativeRepository,
                          FarmerService farmerService,
                          AuditLogService auditLogService,
                          PasswordEncoder passwordEncoder,
                          PlatformTransactionManager transactionManager) {
        this.enabled = enabled;
        this.password = password;
        this.userRepository = userRepository;
        this.farmerRepository = farmerRepository;
        this.cooperativeRepository = cooperativeRepository;
        this.farmerService = farmerService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            seed(password);
        }
    }

    /**
     * Creates the demo data on an empty system. Returns the usernames created (empty when the system already
     * had data and nothing was done).
     */
    public List<String> seed(String demoPassword) {
        if (demoPassword == null || demoPassword.length() < 12) {
            throw new IllegalStateException("APP_DEMO_SEED=true needs DEMO_SEED_PASSWORD in .env "
                    + "(at least 12 characters): every demo account gets that password.");
        }
        List<String> created = transaction.execute(status -> {
            if (userRepository.count() > 0 || farmerRepository.count() > 0) {
                return List.of();
            }
            return createDemoData(demoPassword);
        });
        if (created == null || created.isEmpty()) {
            log.info("Demo data: skipped, the system already has users or farmers (demo data is only added to an empty system).");
            return List.of();
        }
        log.info("Demo data created. Accounts (password = DEMO_SEED_PASSWORD from .env): {}", String.join(", ", created));
        return created;
    }

    private List<String> createDemoData(String demoPassword) {
        List<String> created = new ArrayList<>();

        // ---- staff ----
        AppUser admin = null;
        AppUser registrar = null;
        for (Account account : STAFF) {
            AppUser user = account(account.username(), account.role(), demoPassword);
            if (account.district() != null) {
                user.assignDistrict(district(account.district()));
            }
            user = userRepository.save(user);
            created.add(user.getUsername());
            if (account.role() == Role.SYSTEM_ADMIN) {
                admin = user;
            } else if (account.role() == Role.UNION_REGISTRAR) {
                registrar = user;
            }
        }

        // ---- cooperatives, one per district ----
        Cooperative huye = cooperative("Huye Maize Growers Cooperative", "Huye");
        Cooperative musanze = cooperative("Musanze Potato Growers Cooperative", "Musanze");

        // ---- farmers, registered the normal way, as the demo registrar ----
        long aline = asUser(registrar, () -> register("Aline Uwase", "1199080000000011", "+250788000011", huye));
        asUser(registrar, () -> register("Jean Habimana", "1198570000000022", "+250788000022", huye));
        asUser(registrar, () -> register("Claudine Mukamana", "1199270000000033", null, huye));
        long eric = asUser(registrar, () -> register("Eric Niyonzima", "1198870000000044", "+250788000044", musanze));

        // ---- farmer accounts: each linked to one farmer profile ----
        AppUser farmerAccount = account("demo.farmer", Role.FARMER, demoPassword);
        farmerAccount.assignFarmer(farmerRepository.findById(aline).orElseThrow());
        created.add(userRepository.save(farmerAccount).getUsername());
        AppUser otherFarmer = account("demo.farmer.musanze", Role.FARMER, demoPassword);
        otherFarmer.assignFarmer(farmerRepository.findById(eric).orElseThrow());
        created.add(userRepository.save(otherFarmer).getUsername());

        auditLogService.recordInCurrentTransaction(AuditEventType.USER_CREATED, true, admin.getId(), admin.getUsername(),
                "DemoData", null, "Demo data created: " + created.size() + " accounts, 2 cooperatives, 4 farmers", null);
        return created;
    }

    /** A ready-to-use account: email verified, no forced password change. */
    private AppUser account(String username, Role role, String demoPassword) {
        String hash = passwordEncoder.encode(demoPassword);
        AppUser user = new AppUser(username, username + "@" + EMAIL_DOMAIN, hash, role);
        user.markEmailVerified();
        user.changePasswordHash(hash);   // also clears "must change password"
        return user;
    }

    private long register(String fullName, String nationalId, String phone, Cooperative cooperative) {
        return farmerService.register(new FarmerRegistrationRequest(fullName, nationalId, phone, cooperative.getId())).id();
    }

    /** The cooperative with this name in this district (names are unique per district), created if missing. */
    private Cooperative cooperative(String name, String districtName) {
        District district = district(districtName);
        return entityManager.createQuery("""
                        SELECT c FROM Cooperative c WHERE c.district = :district AND lower(c.name) = lower(:name)""",
                        Cooperative.class)
                .setParameter("district", district)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> cooperativeRepository.save(new Cooperative(name, district)));
    }

    private District district(String name) {
        return entityManager.createQuery("SELECT d FROM District d WHERE lower(d.name) = lower(:name)", District.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    /** Runs the action as this user (FarmerService checks who is asking), then restores the previous identity. */
    private <T> T asUser(AppUser user, Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(String.valueOf(user.getId()), null, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
