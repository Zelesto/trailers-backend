package com.pgsa.trailers.service.routing;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Unified routing result used across all providers (ORS, Google, Mapbox)
 * Enterprise-safe + null-safe + API-ready
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutingResult {

    private final Coordinates origin;
    private final Coordinates destination;

    // Core metrics
    private final BigDecimal distanceKm;
    private final BigDecimal durationHours;

    // Provider metadata
    private final String provider;
    private final String geometry;

    public RoutingResult(
            Coordinates origin,
            Coordinates destination,
            BigDecimal distanceKm,
            BigDecimal durationHours,
            String provider,
            String geometry
    ) {
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = safe(distanceKm);
        this.durationHours = safe(durationHours);
        this.provider = provider;
        this.geometry = geometry;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public Coordinates getOrigin() {
        return origin;
    }

    public Coordinates getDestination() {
        return destination;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public String getProvider() {
        return provider;
    }

    public String getGeometry() {
        return geometry;
    }

    // =========================
    // Convenience helpers
    // =========================

    public BigDecimal getDistanceMiles() {
        return distanceKm.multiply(BigDecimal.valueOf(0.621371));
    }

    public BigDecimal getDurationMinutes() {
        return durationHours.multiply(BigDecimal.valueOf(60));
    }

    // =========================
    // Debug / logging
    // =========================

    @Override
    public String toString() {
        return "RoutingResult{" +
                "origin=" + origin +
                ", destination=" + destination +
                ", distanceKm=" + distanceKm +
                ", durationHours=" + durationHours +
                ", provider='" + provider + '\'' +
                '}';
    }
}
