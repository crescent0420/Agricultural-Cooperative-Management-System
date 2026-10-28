package com.acms.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.entity.Role;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Rwanda's provinces and districts are seeded by a migration and can no longer be created by hand. */
class RwandaReferenceDataTest extends IntegrationTestBase {

    private static final Map<String, List<String>> OFFICIAL = Map.of(
            "Kigali City", List.of("Gasabo", "Kicukiro", "Nyarugenge"),
            "Northern Province", List.of("Burera", "Gakenke", "Gicumbi", "Musanze", "Rulindo"),
            "Southern Province", List.of("Gisagara", "Huye", "Kamonyi", "Muhanga", "Nyamagabe",
                    "Nyanza", "Nyaruguru", "Ruhango"),
            "Eastern Province", List.of("Bugesera", "Gatsibo", "Kayonza", "Kirehe", "Ngoma",
                    "Nyagatare", "Rwamagana"),
            "Western Province", List.of("Karongi", "Ngororero", "Nyabihu", "Nyamasheke", "Rubavu",
                    "Rusizi", "Rutsiro"));

    private String registrarToken;
    private String farmerToken;

    @BeforeAll
    void createUsers() throws Exception {
        String registrar = unique("ref.registrar");
        String farmer = unique("ref.farmer");
        createUser(registrar, Role.UNION_REGISTRAR, null, null);
        createUser(farmer, Role.FARMER, null, null);
        registrarToken = login(registrar, PASSWORD);
        farmerToken = login(farmer, PASSWORD);
    }

    @Test
    @DisplayName("5 provinces (Kigali City and 4 provinces) are seeded")
    void provincesAreSeeded() throws Exception {
        String body = mockMvc.perform(get("/api/provinces").header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$.data.content[*].name");
        assertThat(names).containsExactlyInAnyOrderElementsOf(OFFICIAL.keySet());
    }

    @Test
    @DisplayName("All 30 districts are seeded, each in its correct province")
    void districtsAreSeededPerProvince() throws Exception {
        String provinces = mockMvc.perform(get("/api/provinces").header("Authorization", bearer(farmerToken)))
                .andReturn().getResponse().getContentAsString();
        int total = 0;
        for (Map.Entry<String, List<String>> province : OFFICIAL.entrySet()) {
            List<Number> ids = JsonPath.read(provinces, "$.data.content[?(@.name == '" + province.getKey() + "')].id");
            String districts = mockMvc.perform(get("/api/districts")
                            .param("provinceId", String.valueOf(ids.get(0).longValue()))
                            .param("size", "100")
                            .header("Authorization", bearer(farmerToken)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> names = JsonPath.read(districts, "$.data.content[*].name");
            assertThat(names).as(province.getKey()).containsExactlyInAnyOrderElementsOf(province.getValue());
            total += names.size();
        }
        assertThat(total).isEqualTo(30);
    }

    @Test
    @DisplayName("Districts can no longer be created through the API, not even by the Registrar (403)")
    void districtsCannotBeCreated() throws Exception {
        mockMvc.perform(post("/api/districts").header("Authorization", bearer(registrarToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json("name", unique("Not a district"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A district shows its province")
    void districtShowsItsProvince() throws Exception {
        mockMvc.perform(get("/api/districts/" + seededDistrictId("Huye")).header("Authorization", bearer(farmerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Huye"))
                .andExpect(jsonPath("$.data.provinceName").value("Southern Province"));
    }
}
