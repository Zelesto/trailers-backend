package com.pgsa.trailers.entity.ops.auto;

public class TripStartedEvent extends TripEvent {
    public TripStartedEvent(Long tripId) {
        super(tripId);
    }
}
