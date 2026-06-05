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
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TrailersApp/1.0");
            headers.set("Accept", "application/json");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            JsonNode arr = objectMapper.readTree(response.getBody());

            if (!arr.isArray() || arr.size() == 0) {
                throw new RuntimeException("Location not found: " + location);
            }

            JsonNode node = arr.get(0);

            return new Coordinates(
                    node.get("lat").asDouble(),
                    node.get("lon").asDouble()
            );

        } catch (Exception e) {
            throw new RuntimeException("Geocoding failed: " + location, e);
        }
    }
}
