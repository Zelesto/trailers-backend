package com.pgsa.trailers.service.routing;

public interface RoutingProvider {

    String name();

    boolean supports(String vehicleType);

    RoutingResult calculate(Coordinates origin,
                            Coordinates destination,
                            String vehicleType);
}
