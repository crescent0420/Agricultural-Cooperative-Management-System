package com.acms.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acms.config.ReferenceDataSeeder;
import com.acms.config.ReferenceDataSeeder.Entry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/** Unit tests (no Spring, no database): the data file and how it is read. */
class ReferenceDataFileTest {

    @Test
    @DisplayName("The file lists Rwanda's 30 districts in 5 provinces, with no duplicate")
    void officialFile() {
        List<Entry> entries = ReferenceDataSeeder.read(new ClassPathResource(ReferenceDataSeeder.DATA_FILE));
        assertThat(entries).hasSize(30);
        assertThat(entries.stream().map(Entry::province).distinct())
                .containsExactlyInAnyOrder("Kigali City", "Northern Province", "Southern Province",
                        "Eastern Province", "Western Province");
        assertThat(entries.stream().map(e -> e.district().toLowerCase()).collect(Collectors.toSet())).hasSize(30);
        assertThat(entries).contains(new Entry("Southern Province", "Huye"), new Entry("Northern Province", "Musanze"));
    }

    @Test
    @DisplayName("Comments and blank lines are skipped; spaces around values are removed")
    void commentsAndSpaces() {
        List<Entry> entries = ReferenceDataSeeder.read(file("""
                # a comment

                province,district
                  Kigali City ,  Gasabo
                """));
        assertThat(entries).containsExactly(new Entry("Kigali City", "Gasabo"));
    }

    @Test
    @DisplayName("A broken file stops the start-up with the line number")
    void brokenFile() {
        assertThatThrownBy(() -> ReferenceDataSeeder.read(file("province,district\nKigali City\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("line 2");
        assertThatThrownBy(() -> ReferenceDataSeeder.read(file("name,place\nA,B\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("header");
        assertThatThrownBy(() -> ReferenceDataSeeder.read(file("province,district\n")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no district");
    }

    private static ByteArrayResource file(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
