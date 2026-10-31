package com.acms.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.acms.config.ReferenceDataSeeder;
import com.acms.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The provinces and districts come from seed/rwanda-districts.csv, checked at every start-up. */
class ReferenceDataSeederTest extends IntegrationTestBase {

    @Autowired
    private ReferenceDataSeeder seeder;

    @Test
    @DisplayName("Start-up check: a missing district is added back from the file; a second run changes nothing")
    void missingDistrictIsRestored() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // Nyagatare is used by no test data (the tests work in Huye and Musanze), so it can be removed safely.
        jdbc.update("DELETE FROM districts WHERE lower(name) = 'nyagatare'");
        assertThat(count(jdbc)).isEqualTo(29);

        ReferenceDataSeeder.Result first = seeder.seed();
        assertThat(first.districtsInFile()).isEqualTo(30);
        assertThat(first.provincesInFile()).isEqualTo(5);
        assertThat(first.districtsAdded()).isEqualTo(1);
        assertThat(first.provincesAdded()).isZero();
        assertThat(count(jdbc)).isEqualTo(30);
        assertThat(jdbc.queryForObject("""
                SELECT p.name FROM districts d JOIN provinces p ON p.id = d.province_id
                 WHERE lower(d.name) = 'nyagatare'""", String.class)).isEqualTo("Eastern Province");

        ReferenceDataSeeder.Result second = seeder.seed();
        assertThat(second.districtsAdded() + second.provincesAdded() + second.provincesAssigned()).isZero();
        assertThat(count(jdbc)).isEqualTo(30);
    }

    @Test
    @DisplayName("Start-up check: a district without a province gets its official province")
    void districtWithoutProvinceIsCompleted() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE districts SET province_id = NULL WHERE lower(name) = 'rutsiro'");

        assertThat(seeder.seed().provincesAssigned()).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT p.name FROM districts d JOIN provinces p ON p.id = d.province_id
                 WHERE lower(d.name) = 'rutsiro'""", String.class)).isEqualTo("Western Province");
    }

    private int count(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM districts d JOIN provinces p ON p.id = d.province_id""", Integer.class);
    }
}
