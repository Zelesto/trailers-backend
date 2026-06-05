package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Google Maps is premium-grade fallback (best accuracy)
 */
@Component
@RequiredArgsConstructor
public class GoogleRoutingProvider implements RoutingProvider {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @Override
    public String name() {
        return "google";
    }

    @Override
    public boolean supports(String vehicleType) {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType) {

        // NOTE:
        // You can plug Google Directions API here.
        // Kept simplified for architecture clarity.

        throw new UnsupportedOperationException(
                "Google provider not fully implemented yet"
        );
    }
}
