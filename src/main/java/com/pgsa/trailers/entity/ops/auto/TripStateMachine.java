package com.pgsa.trailers.entity.ops.auto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TripStateMachine {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put("DRAFT", Set.of("PLANNED", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("PLANNED", Set.of("ASSIGNED", "IN_PROGRESS", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("ASSIGNED", Set.of("IN_PROGRESS", "ON_HOLD", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("IN_PROGRESS", Set.of("COMPLETED", "ON_HOLD", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("ACTIVE", Set.of("COMPLETED", "ON_HOLD", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("ON_HOLD", Set.of("IN_PROGRESS", "ACTIVE", "COMPLETED", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("PENDING", Set.of("PLANNED", "ASSIGNED", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("COMPLETED", Set.of("FINALIZED", "CLOSED"));
        ALLOWED_TRANSITIONS.put("FINALIZED", Set.of());
        ALLOWED_TRANSITIONS.put("CLOSED", Set.of());
        ALLOWED_TRANSITIONS.put("CANCELLED", Set.of());
    }

    public static void validateTransition(String current, String next) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Invalid trip state transition: " + current + " → " + next);
        }
    }
}
