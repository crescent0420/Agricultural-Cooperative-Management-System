package com.acms.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.acms.common.BaseEntity;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/** Unit tests of the project's structure, so the supervisor's conventions cannot quietly erode. */
class ProjectStructureTest {

    @Test
    @DisplayName("Every entity extends BaseEntity (id, created_at, updated_at)")
    void everyEntityExtendsBaseEntity() throws Exception {
        List<Class<?>> entities = classes("com.acms", new AnnotationTypeFilter(Entity.class));
        assertThat(entities).hasSizeGreaterThanOrEqualTo(24);
        assertThat(entities).allMatch(BaseEntity.class::isAssignableFrom,
                "every @Entity extends BaseEntity");
    }

    @Test
    @DisplayName("Every status and type enum lives in com.acms.enums")
    void enumsLiveInTheEnumsPackage() throws Exception {
        assertThat(classes("com.acms.entity", new AssignableTypeFilter(Enum.class))).isEmpty();
        assertThat(classes("com.acms.enums", new AssignableTypeFilter(Enum.class)))
                .extracting(Class::getSimpleName)
                .containsAll(Set.of("AuditEventType", "EnrolmentStatus", "FarmerStatus", "PaymentMethod",
                        "PaymentStatus", "PermitOutcome", "ResultStatus", "Role", "WindowStatus"));
    }

    private static List<Class<?>> classes(String basePackage,
                                          TypeFilter filter) throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(filter);
        List<Class<?>> found = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            found.add(Class.forName(definition.getBeanClassName()));
        }
        return found;
    }
}
