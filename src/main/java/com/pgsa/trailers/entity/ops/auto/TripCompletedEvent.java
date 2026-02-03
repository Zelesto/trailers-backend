package com.pgsa.trailers.entity.ops.auto;


public class TripCompletedEvent extends TripEvent {
    public TripCompletedEvent(Long tripId) {
        super(tripId);
    }
}