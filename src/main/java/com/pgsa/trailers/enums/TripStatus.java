package com.pgsa.trailers.enums;

import lombok.Getter;

@Getter
public enum TripStatus {

    DRAFT("Draft", "Trip is being planned, not yet ready for assignment"),
    PLANNED("Planned", "Trip is planned and ready for assignment"),
    ASSIGNED("Assigned", "Driver and vehicle have been assigned"),
    IN_PROGRESS("In Progress", "Trip is currently in progress"),
    ON_HOLD("On Hold", "Trip is temporarily paused"),
    ACTIVE("Active", "Trip is active and running"),
    PENDING("Pending", "Awaiting approval or action"),
    COMPLETED("Completed", "Trip has been completed successfully"),
    FINALIZED("Finalized", "All costs and metrics have been finalized"),
    CANCELLED("Cancelled", "Trip has been cancelled"),
    CLOSED("Closed", "Trip is closed for further changes");

    private final String displayName;
    private final String description;

    TripStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isActive() {
        return this == PLANNED || this == ASSIGNED || this == IN_PROGRESS || this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FINALIZED || this == CANCELLED || this == CLOSED;
    }

    public boolean canTransitionTo(TripStatus target) {
        return switch (this) {
            case DRAFT -> target == PLANNED || target == CANCELLED;
            case PLANNED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED;
            case IN_PROGRESS, ACTIVE -> target == COMPLETED || target == ON_HOLD || target == CANCELLED;
            case ON_HOLD -> target == IN_PROGRESS || target == ACTIVE || target == COMPLETED || target == CANCELLED;
            case PENDING -> target == PLANNED || target == ASSIGNED || target == CANCELLED;
            case COMPLETED -> target == FINALIZED || target == CLOSED;
            case FINALIZED, CLOSED, CANCELLED -> false;
        };
    }

    public boolean requiresApproval() {
        return this == PENDING || this == PLANNED;
    }

    public static TripStatus fromDisplayName(String displayName) {
        for (TripStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status display name: " + displayName);
    }
}
