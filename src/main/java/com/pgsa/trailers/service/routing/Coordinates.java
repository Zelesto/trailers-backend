package com.pgsa.trailers.service.routing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class Coordinates {

    private double lat;
    private double lng;

    /**
     * Used for ORS / Mapbox / Google APIs (lng,lat format)
     */
    public String toQuery() {
        return lng + "," + lat;
    }

    /**
     * Safer string representation for logs/debugging
     */
    @Override
    public String toString() {
        return String.format("Coordinates{lat=%.6f, lng=%.6f}", lat, lng);
    }

    /**
     * Defensive equality (important for caching / comparisons)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinates that)) return false;
        return Double.compare(that.lat, lat) == 0 &&
               Double.compare(that.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lng);
    }
}
