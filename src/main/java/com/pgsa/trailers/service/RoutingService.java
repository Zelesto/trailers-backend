package com.pgsa.trailers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouteservice.api.key:}")
    private String orsApiKey;

    @Value("${openrouteservice.api.url:https://api.openrouteservice.org}")
    private String orsApiUrl;

    public RoutingResult calculateRoute(String origin, String destination, String vehicleType) {
        try {
            // Validate inputs
            if (origin == null || origin.trim().isEmpty()) {
                throw new IllegalArgumentException("Origin location cannot be null or empty");
            }
            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination location cannot be null or empty");
            }
            if (orsApiKey == null || orsApiKey.trim().isEmpty()) {
                throw new IllegalStateException("OpenRouteService API key is not configured");
            }

            log.info("Calculating route from '{}' to '{}' for vehicle type: {}", origin, destination, vehicleType);

            // Step 1: Geocode locations using Nominatim (OpenStreetMap)
            Coordinates originCoords = geocodeLocation(origin);
            Coordinates destCoords = geocodeLocation(destination);

            log.info("Coordinates: Origin [lat={}, lng={}], Destination [lat={}, lng={}]",
                    originCoords.getLat(), originCoords.getLng(),
                    destCoords.getLat(), destCoords.getLng());

            // Step 2: Calculate route using OpenRouteService Directions API
            String profile = getVehicleProfile(vehicleType);

            // Build URL with query parameters
            String url = UriComponentsBuilder.fromHttpUrl(orsApiUrl + "/v2/directions/" + profile)
                    .queryParam("api_key", orsApiKey)
                    .queryParam("start", originCoords.getLng() + "," + originCoords.getLat())
                    .queryParam("end", destCoords.getLng() + "," + destCoords.getLat())
                    .build()
                    .toUriString();

            log.debug("Calling ORS API: {}", url.replace(orsApiKey, "***"));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Make the request
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            log.debug("ORS API Response Status: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("ORS API returned error: " + response.getStatusCode());
            }

            // Parse response
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // Check for errors in response
            if (jsonNode.has("error")) {
                String errorMsg = jsonNode.path("error").path("message").asText("Unknown error");
                throw new RuntimeException("ORS API error: " + errorMsg);
            }

            if (!jsonNode.has("features") || jsonNode.get("features").size() == 0) {
                throw new RuntimeException("No route found between locations");
            }

            // Extract route information
            JsonNode route = jsonNode.get("features").get(0);
            JsonNode properties = route.path("properties");
            JsonNode summary = properties.path("summary");

            if (summary.isMissingNode()) {
                // Try alternative path structure
                JsonNode segments = properties.path("segments");
                if (segments.isArray() && segments.size() > 0) {
                    summary = segments.get(0).path("summary");
                }
            }

            if (summary.isMissingNode()) {
                throw new RuntimeException("Could not parse route information from response");
            }

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            BigDecimal distanceKm = new BigDecimal(distanceMeters / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal durationHours = new BigDecimal(durationSeconds / 3600.0)
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Route calculated successfully: {} km, {} hours ({} minutes)",
                    distanceKm, durationHours, BigDecimal.valueOf(durationSeconds / 60.0).setScale(0, RoundingMode.HALF_UP));

            return new RoutingResult(originCoords, destCoords, distanceKm, durationHours);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Route calculation failed: {}", e.getMessage());
            throw new RuntimeException("Route calculation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error calculating route: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate route: " + e.getMessage());
        }
    }

    private Coordinates geocodeLocation(String location) throws Exception {
        log.debug("Geocoding location: {}", location);

        // Encode the location for URL
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

        // Build Nominatim URL
        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", encodedLocation)
                .queryParam("limit", "1")
                .queryParam("countrycodes", "za") // South Africa
                .queryParam("addressdetails", "1")
                .build()
                .toUriString();

        log.debug("Calling Nominatim API: {}", url);

        // Set headers required by Nominatim
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrailersApp/1.0 (theo.zwane@company.com)");
        headers.set("Accept-Language", "en");
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Geocoding API returned error: " + response.getStatusCode());
        }

        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        if (!jsonNode.isArray() || jsonNode.size() == 0) {
            throw new RuntimeException("Location not found: " + location);
        }

        JsonNode firstResult = jsonNode.get(0);
        double lat = firstResult.path("lat").asDouble();
        double lon = firstResult.path("lon").asDouble();

        String displayName = firstResult.path("display_name").asText();
        log.debug("Geocoded '{}' to: {} (lat={}, lon={})", location, displayName, lat, lon);

        return new Coordinates(lat, lon);
    }

    private String getVehicleProfile(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return "driving-car";
        }

        String normalizedType = vehicleType.trim().toUpperCase();

        switch (normalizedType) {
            case "TRUCK":
            case "TRAILER":
            case "HGV":
            case "HEAVY":
                return "driving-hgv";
            case "VAN":
            case "DELIVERY":
                return "driving-car"; // ORS doesn't have a specific van profile
            case "BIKE":
            case "BICYCLE":
                return "cycling-regular";
            case "WALK":
            case "FOOT":
                return "foot-walking";
            case "CAR":
            default:
                return "driving-car";
        }
    }

    /**
     * Simplified route calculation without geocoding (uses coordinates directly)
     */
    public RoutingResult calculateRouteDirect(double startLat, double startLng,
                                              double endLat, double endLng,
                                              String vehicleType) {
        try {
            if (orsApiKey == null || orsApiKey.trim().isEmpty()) {
                throw new IllegalStateException("OpenRouteService API key is not configured");
            }

            log.info("Calculating direct route from [{}, {}] to [{}, {}]",
                    startLat, startLng, endLat, endLng);

            String profile = getVehicleProfile(vehicleType);

            String url = UriComponentsBuilder.fromHttpUrl(orsApiUrl + "/v2/directions/" + profile)
                    .queryParam("api_key", orsApiKey)
                    .queryParam("start", startLng + "," + startLat)
                    .queryParam("end", endLng + "," + endLat)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (jsonNode.has("error")) {
                String errorMsg = jsonNode.path("error").path("message").asText("Unknown error");
                throw new RuntimeException("ORS API error: " + errorMsg);
            }

            if (!jsonNode.has("features") || jsonNode.get("features").size() == 0) {
                throw new RuntimeException("No route found between coordinates");
            }

            JsonNode route = jsonNode.get("features").get(0);
            JsonNode summary = route.path("properties").path("summary");

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            BigDecimal distanceKm = new BigDecimal(distanceMeters / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal durationHours = new BigDecimal(durationSeconds / 3600.0)
                    .setScale(2, RoundingMode.HALF_UP);

            return new RoutingResult(
                    new Coordinates(startLat, startLng),
                    new Coordinates(endLat, endLng),
                    distanceKm,
                    durationHours
            );

        } catch (Exception e) {
            log.error("Error in direct route calculation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate direct route: " + e.getMessage());
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class RoutingResult {
        private final Coordinates origin;
        private final Coordinates destination;
        private final BigDecimal distance; // km
        private final BigDecimal duration; // hours

        public BigDecimal getDistanceMiles() {
            return distance.multiply(BigDecimal.valueOf(0.621371)).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal getDurationMinutes() {
            return duration.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP);
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class Coordinates {
        private final double lat;
        private final double lng;

        public String toString() {
            return String.format("%.6f,%.6f", lat, lng);
        }
    }
}