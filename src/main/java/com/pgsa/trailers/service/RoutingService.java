package com.pgsa.trailers.service;

import com.pgsa.trailers.service.routing.RoutingEngine;
import com.pgsa.trailers.service.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RoutingEngine routingEngine;

    public RoutingResult calculateRoute(String origin, String destination, String vehicleType) {
        return routingEngine.calculateRoute(origin, destination, vehicleType);
    }

    public RoutingResult calculateRouteDirect(double startLat, double startLng,
                                              double endLat, double endLng,
                                              String vehicleType) {
        return routingEngine.calculateRouteDirect(startLat, startLng, endLat, endLng, vehicleType);
    }
}
