package com.acms.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acms.config.ApiTags;
import com.acms.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the API documentation complete: every endpoint in the live OpenAPI description (/v3/api-docs)
 * must have one of the 13 tags, a summary and a description, and must appear in both tables of
 * docs/endpoints.md. A new endpoint therefore cannot be shipped undocumented.
 */
class OpenApiDocumentationTest extends IntegrationTestBase {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final List<String> TAGS_IN_ORDER = List.of(
            ApiTags.AUTHENTICATION, ApiTags.USER_ADMINISTRATION, ApiTags.AUDIT_AND_PERMISSIONS,
            ApiTags.REFERENCE_DATA, ApiTags.FARMERS, ApiTags.TRAINING_RECORDS, ApiTags.SEASONS,
            ApiTags.OFFERINGS, ApiTags.ENROLMENT, ApiTags.CONTRIBUTIONS_AND_PAYMENTS,
            ApiTags.INSPECTIONS_AND_RESULTS, ApiTags.COLLECTION_PERMITS, ApiTags.MANAGEMENT_REPORTS);

    private String apiDocs;
    /** "POST /api/farmers" -> that operation's JSON object. */
    private final Map<String, Map<String, Object>> operations = new HashMap<>();

    @BeforeAll
    void readApiDocs() throws Exception {
        apiDocs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, Map<String, Object>> paths = JsonPath.read(apiDocs, "$.paths");
        paths.forEach((path, item) -> item.forEach((method, operation) -> {
            if (HTTP_METHODS.contains(method)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> op = (Map<String, Object>) operation;
                operations.put(method.toUpperCase() + " " + path, op);
            }
        }));
    }

    @Test
    @DisplayName("Exactly the 13 tags are declared, each with a description")
    void tagsAreDeclared() {
        List<String> names = JsonPath.read(apiDocs, "$.tags[*].name");
        assertThat(names).containsExactlyInAnyOrderElementsOf(TAGS_IN_ORDER);
        List<Object> descriptions = JsonPath.read(apiDocs, "$.tags[*].description");
        assertThat(descriptions).hasSize(TAGS_IN_ORDER.size())
                .allSatisfy(d -> assertThat(d).isInstanceOf(String.class).asString().isNotBlank());
        String apiDescription = JsonPath.read(apiDocs, "$.info.description");
        assertThat(apiDescription).contains("Authorization: Bearer");
    }

    @Test
    @DisplayName("Every endpoint has exactly one known tag, a summary and a description")
    void everyEndpointIsDocumented() {
        assertThat(operations).isNotEmpty();
        List<String> problems = new ArrayList<>();
        operations.forEach((endpoint, op) -> {
            Object tags = op.get("tags");
            if (!(tags instanceof List<?> list) || list.size() != 1 || !TAGS_IN_ORDER.contains(list.getFirst())) {
                problems.add(endpoint + ": tags " + tags);
            }
            if (!(op.get("summary") instanceof String summary) || summary.isBlank()) {
                problems.add(endpoint + ": no summary");
            }
            if (!(op.get("description") instanceof String description) || description.isBlank()) {
                problems.add(endpoint + ": no description");
            }
        });
        assertThat(problems).as("undocumented endpoints").isEmpty();
    }

    @Test
    @DisplayName("Login, register and verify-email need no token; the rest inherit the Bearer requirement")
    void publicEndpointsHaveNoSecurityRequirement() {
        for (String endpoint : List.of("POST /api/auth/login", "POST /api/auth/register", "GET /api/auth/verify-email")) {
            assertThat(operations.get(endpoint)).as(endpoint).containsKey("security");
            assertThat((List<?>) operations.get(endpoint).get("security")).as(endpoint).isEmpty();
        }
        List<Object> globalSecurity = JsonPath.read(apiDocs, "$.security");
        assertThat(globalSecurity).as("every other endpoint inherits the Bearer requirement").isNotEmpty();
    }

    @Test
    @DisplayName("docs/endpoints.md lists every endpoint in both of its tables, and nothing else")
    void endpointTableMatchesTheApi() throws Exception {
        String table = Files.readString(Path.of("docs", "endpoints.md"), StandardCharsets.UTF_8);
        Matcher row = Pattern.compile("(?m)^\\| (GET|POST|PUT|PATCH|DELETE) \\| `([^`]+)` \\|").matcher(table);
        Map<String, Integer> rowsPerEndpoint = new HashMap<>();
        while (row.find()) {
            rowsPerEndpoint.merge(row.group(1) + " " + row.group(2), 1, Integer::sum);
        }

        assertThat(new TreeSet<>(rowsPerEndpoint.keySet()))
                .as("endpoints in docs/endpoints.md")
                .isEqualTo(new TreeSet<>(operations.keySet()));
        rowsPerEndpoint.forEach((endpoint, rows) ->
                assertThat(rows).as(endpoint + " must appear in both tables").isEqualTo(2));
    }
}
