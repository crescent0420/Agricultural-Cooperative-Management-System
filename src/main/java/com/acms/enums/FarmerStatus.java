package com.acms.enums;

public enum FarmerStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN,
    RETIRED;

    /**
     * Checks whether a transition to the target status is allowed.
     */
    public boolean canChangeTo(FarmerStatus target) {
        return switch (this) {
            case ACTIVE ->
                    target == SUSPENDED
                    || target == WITHDRAWN
                    || target == RETIRED;

            case SUSPENDED ->
                    target == ACTIVE
                    || target == WITHDRAWN
                    || target == RETIRED;

            case WITHDRAWN -> target == ACTIVE;

            case RETIRED -> false;
        };
    }
}
