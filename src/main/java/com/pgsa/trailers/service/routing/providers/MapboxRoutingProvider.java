package com.pgsa.trailers.service.routing.providers;

import com.pgsa.trailers.service.routing.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MapboxRoutingProvider implements RoutingProvider {

    @Override
    public String name() {
        return "mapbox";
    }

    @Override
    public boolean supports(String vehicleType) {
        return true; // or API key check if you have one
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType,
                                   Map<String, Object> context) {

        // Example placeholder (replace with real Mapbox API call)
        throw new UnsupportedOperationException("Mapbox routing not implemented yet");
    }
}
