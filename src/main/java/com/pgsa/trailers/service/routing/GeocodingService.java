package com.pgsa.trailers.service.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Coordinates geocode(String location) {

        try {
            String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("format", "json")
                    .queryParam("q", encoded)
                    .queryParam("limit", "1")
                    .queryParam("addressdetails", "1")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TrailersApp/1.0 (routing-service)");
            headers.set("Accept", "application/json");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Geocoding API failed: " + response.getStatusCode());
            }

            JsonNode arr = objectMapper.readTree(response.getBody());

            if (!arr.isArray() || arr.isEmpty()) {
                throw new RuntimeException("Location not found: " + location);
            }

            JsonNode node = arr.get(0);

            double lat = node.path("lat").asDouble();
            double lon = node.path("lon").asDouble();

            return new Coordinates(lat, lon);

        } catch (Exception e) {
            log.error("Geocoding failed for: {}", location, e);
            throw new RuntimeException("Geocoding failed: " + location, e);
        }
    }
}
