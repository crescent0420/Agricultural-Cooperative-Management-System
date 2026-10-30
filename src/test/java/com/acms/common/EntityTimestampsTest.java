package com.acms.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.enums.Role;
import com.acms.support.IntegrationTestBase;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** BaseEntity fills created_at and updated_at for every entity, and every entity table has both columns. */
class EntityTimestampsTest extends IntegrationTestBase {

    @Test
    @DisplayName("On create both times are set and equal; on update only updated_at moves")
    void createdAndUpdated() throws Exception {
        String token = login(createUser(unique("ts.registrar"), Role.UNION_REGISTRAR, null, null).getUsername(), PASSWORD);
        long id = postForId("/api/cooperatives", token,
                json("name", unique("Timestamp Coop"), "districtId", seededDistrictId("Huye")));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Timestamp[] before = times(jdbc, id);
        assertThat(before[0]).isNotNull().isEqualTo(before[1]);

        Thread.sleep(20);
        mockMvc.perform(put("/api/cooperatives/" + id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json("name", unique("Renamed Coop"))))
                .andExpect(status().isOk());
        Timestamp[] after = times(jdbc, id);
        assertThat(after[0]).isEqualTo(before[0]);
        assertThat(after[1]).isAfter(before[1]);
    }

    @Test
    @DisplayName("Every entity table has created_at and updated_at, both required")
    void everyTableHasBothColumns() {
        List<String> missing = new JdbcTemplate(dataSource).queryForList("""
                SELECT t.table_name FROM information_schema.tables t
                 WHERE t.table_schema = 'public' AND t.table_type = 'BASE TABLE'
                   AND t.table_name NOT IN ('flyway_schema_history', 'programme_prerequisites')
                   AND (SELECT count(*) FROM information_schema.columns c
                         WHERE c.table_schema = 'public' AND c.table_name = t.table_name
                           AND c.column_name IN ('created_at', 'updated_at') AND c.is_nullable = 'NO') < 2""",
                String.class);
        assertThat(missing).as("tables without both timestamp columns").isEmpty();
    }

    private static Timestamp[] times(JdbcTemplate jdbc, long id) {
        return jdbc.queryForObject("SELECT created_at, updated_at FROM cooperatives WHERE id = ?",
                (rs, n) -> new Timestamp[] {rs.getTimestamp(1), rs.getTimestamp(2)}, id);
    }
}
