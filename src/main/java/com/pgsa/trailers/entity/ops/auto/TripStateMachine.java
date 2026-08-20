package com.pgsa.trailers.entity.ops.auto;

import java.util.EnumSet;

import java.util.Map;
import java.util.Set;



public class TripStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = Map.of(
            "DRAFT", Set.of("PLANNED", "CANCELLED"),
            "PLANNED", Set.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("COMPLETED"),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of()
    );

    public static void validateTransition(String current, String next) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Invalid trip state transition: " + current + " → " + next);
        }
    }
}

