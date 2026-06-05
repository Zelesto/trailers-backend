package com.pgsa.trailers.service.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingEngine {

    private final List<RoutingProvider> providers;
    private final GeocodingService geocodingService;

    /**
     * Main entry with provider failover
     */
    public RoutingResult calculateRoute(String origin, String destination, String vehicleType) {

        Coordinates originCoords = geocodingService.geocode(origin);
        Coordinates destCoords = geocodingService.geocode(destination);

        Exception lastError = null;

        for (RoutingProvider provider : providers) {
            try {
                if (!provider.supports(vehicleType)) continue;

                log.info("Trying routing provider: {}", provider.name());

                return provider.calculate(originCoords, destCoords, vehicleType);

            } catch (Exception e) {
                lastError = e;
                log.warn("Provider {} failed: {}", provider.name(), e.getMessage());
            }
        }

        throw new RuntimeException("All routing providers failed", lastError);
    }

    /**
     * Direct coordinate routing (no geocoding)
     */
    public RoutingResult calculateRouteDirect(double startLat, double startLng,
                                              double endLat, double endLng,
                                              String vehicleType) {

        Coordinates origin = new Coordinates(startLat, startLng);
        Coordinates dest = new Coordinates(endLat, endLng);

        for (RoutingProvider provider : providers) {
            try {
                if (!provider.supports(vehicleType)) continue;

                return provider.calculate(origin, dest, vehicleType);

            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.name(), e.getMessage());
            }
        }

        throw new RuntimeException("All routing providers failed");
    }
}
