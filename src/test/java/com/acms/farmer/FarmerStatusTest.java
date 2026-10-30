package com.acms.farmer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit test (no Spring, no database): every one of the 16 possible status changes. */
class FarmerStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @DisplayName("Allowed and refused status changes")
    @CsvSource({
            "ACTIVE,    SUSPENDED, true",
            "ACTIVE,    WITHDRAWN, true",
            "ACTIVE,    RETIRED,   true",
            "ACTIVE,    ACTIVE,    false",
            "SUSPENDED, ACTIVE,    true",
            "SUSPENDED, WITHDRAWN, true",
            "SUSPENDED, RETIRED,   true",
            "SUSPENDED, SUSPENDED, false",
            "WITHDRAWN, ACTIVE,    true",
            "WITHDRAWN, SUSPENDED, false",
            "WITHDRAWN, RETIRED,   false",
            "WITHDRAWN, WITHDRAWN, false",
            "RETIRED,   ACTIVE,    false",
            "RETIRED,   SUSPENDED, false",
            "RETIRED,   WITHDRAWN, false",
            "RETIRED,   RETIRED,   false"
    })
    void transitions(FarmerStatus from, FarmerStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "RETIRED -> {0} is refused with a clear message")
    @CsvSource({"ACTIVE", "SUSPENDED", "WITHDRAWN"})
    void retiredIsFinal(FarmerStatus target) {
        Farmer farmer = retiredFarmer();
        assertThatThrownBy(() -> farmer.changeStatus(target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RETIRED");
        assertThat(farmer.getStatus()).isEqualTo(FarmerStatus.RETIRED);
    }

    private static Farmer retiredFarmer() {
        Farmer farmer = new Farmer("RCU-000001", "1199080000000001", "Test Farmer", null, null);
        farmer.changeStatus(FarmerStatus.RETIRED);
        return farmer;
    }
}
