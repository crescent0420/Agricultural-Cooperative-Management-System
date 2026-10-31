package com.acms.config;

import com.acms.entity.District;
import com.acms.entity.Province;
import com.acms.repository.DistrictRepository;
import com.acms.repository.ProvinceRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rwanda's provinces and districts, from the data file seed/rwanda-districts.csv, checked at EVERY start-up
 * (after Flyway has built the tables):
 *  - a province or district missing from the database is added;
 *  - a district that exists without a province (typed in by hand long ago) gets its official province;
 *  - nothing that exists is renamed, moved or deleted, so ids used by cooperatives, programmes and staff
 *    never change.
 * Running it again changes nothing. To add or correct an entry, edit the CSV and restart.
 *
 * A broken file stops the start-up with the line number: running with half the reference data would be worse.
 * It runs before DemoDataSeeder (@Order), which needs the districts.
 */
@Component
@Order(1)
public class ReferenceDataSeeder implements ApplicationRunner {

    public static final String DATA_FILE = "seed/rwanda-districts.csv";
    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSeeder.class);

    /** One line of the file. */
    public record Entry(String province, String district) {
    }

    /** What one run did. */
    public record Result(int provincesInFile, int districtsInFile, int provincesAdded, int districtsAdded,
                         int provincesAssigned) {
    }

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final TransactionTemplate transaction;

    public ReferenceDataSeeder(ProvinceRepository provinceRepository, DistrictRepository districtRepository,
                               PlatformTransactionManager transactionManager) {
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    public Result seed() {
        List<Entry> entries = read(new ClassPathResource(DATA_FILE));
        Result result = transaction.execute(status -> apply(entries));
        if (result.provincesAdded() + result.districtsAdded() + result.provincesAssigned() == 0) {
            log.info("Reference data: all {} provinces and {} districts of {} are present.",
                    result.provincesInFile(), result.districtsInFile(), DATA_FILE);
        } else {
            log.info("Reference data from {}: {} province(s) and {} district(s) added, {} district(s) given their province.",
                    DATA_FILE, result.provincesAdded(), result.districtsAdded(), result.provincesAssigned());
        }
        return result;
    }

    private Result apply(List<Entry> entries) {
        Map<String, Province> provinces = new LinkedHashMap<>();
        int provincesAdded = 0;
        int districtsAdded = 0;
        int provincesAssigned = 0;

        for (Entry entry : entries) {
            String key = entry.province().toLowerCase();
            if (!provinces.containsKey(key)) {
                Province province = provinceRepository.findByNameIgnoreCase(entry.province()).orElse(null);
                if (province == null) {
                    province = provinceRepository.save(new Province(entry.province()));
                    provincesAdded++;
                }
                provinces.put(key, province);
            }
            Province province = provinces.get(key);

            District district = districtRepository.findByNameIgnoreCase(entry.district()).orElse(null);
            if (district == null) {
                districtRepository.save(new District(entry.district(), province));
                districtsAdded++;
            } else if (district.getProvince() == null) {
                district.assignProvince(province);
                provincesAssigned++;
            } else if (!district.getProvince().getId().equals(province.getId())) {
                log.warn("Reference data: district '{}' is in province '{}' in the database but '{}' in {}; left unchanged.",
                        district.getName(), district.getProvince().getName(), province.getName(), DATA_FILE);
            }
        }
        return new Result(provinces.size(), entries.size(), provincesAdded, districtsAdded, provincesAssigned);
    }

    /** The file's lines: blank lines and lines starting with # are skipped, then the header "province,district". */
    public static List<Entry> read(Resource resource) {
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            boolean headerSeen = false;
            while ((line = reader.readLine()) != null) {
                number++;
                String text = line.strip();
                if (text.isEmpty() || text.startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    if (!text.equalsIgnoreCase("province,district")) {
                        throw broken(number, "the first line must be the header 'province,district'");
                    }
                    headerSeen = true;
                    continue;
                }
                String[] parts = text.split(",", -1);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    throw broken(number, "expected 'province,district', found '" + text + "'");
                }
                entries.add(new Entry(parts[0].strip(), parts[1].strip()));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + DATA_FILE + ": " + ex.getMessage(), ex);
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException(DATA_FILE + " lists no district.");
        }
        return entries;
    }

    private static IllegalStateException broken(int line, String problem) {
        return new IllegalStateException(DATA_FILE + ", line " + line + ": " + problem + ".");
    }
}
