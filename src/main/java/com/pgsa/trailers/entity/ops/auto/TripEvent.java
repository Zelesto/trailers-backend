package com.pgsa.trailers.entity.ops.auto;


import lombok.Getter;

@Getter
public abstract class TripEvent {
    private final Long tripId;

    protected TripEvent(Long tripId) {
        this.tripId = tripId;
    }
}

