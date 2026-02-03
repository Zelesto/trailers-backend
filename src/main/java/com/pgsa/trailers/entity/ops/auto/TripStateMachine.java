package com.pgsa.trailers.entity.ops.auto;

import java.util.EnumSet;
import com.pgsa.trailers.enums.TripStatus;
import java.util.Map;

import static com.pgsa.trailers.enums.TripStatus.*;

public class TripStateMachine {

    private static final Map<TripStatus, EnumSet<TripStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(PLANNED, CANCELLED),
            PLANNED, EnumSet.of(ASSIGNED, CANCELLED),
            ASSIGNED, EnumSet.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, EnumSet.of(COMPLETED),
            COMPLETED, EnumSet.noneOf(TripStatus.class),
            CANCELLED, EnumSet.noneOf(TripStatus.class)
    );

    public static void validateTransition(
            TripStatus current,
            TripStatus next
    ) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TripStatus.class))
                .contains(next)) {
            throw new IllegalStateException(
                    "Invalid trip state transition: " + current + " → " + next
            );
        }
    }
}

