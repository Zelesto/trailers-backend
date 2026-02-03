package com.pgsa.trailers.service.util;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TripNumberGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(1);

    public String generate() {
        long seq = SEQUENCE.getAndIncrement();
        int year = Year.now().getValue();
        return "TRP-" + year + "-" + String.format("%06d", seq);
    }
}
