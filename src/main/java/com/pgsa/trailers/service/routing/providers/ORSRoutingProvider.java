package com.pgsa.trailers.service.routing.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.service.routing.Coordinates;
import com.pgsa.trailers.service.routing.RoutingProvider;
import com.pgsa.trailers.service.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ORSRoutingProvider implements RoutingProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouteservice.api.key}")
    private String apiKey;

    @Override
    public String name() {
        return "openrouteservice";
    }

    @Override
    public boolean supports(String vehicleType) {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public RoutingResult calculate(Coordinates origin,
                                   Coordinates destination,
                                   String vehicleType) {

        try {
            String url = "https://api.openrouteservice.org/v2/directions/driving-hgv";

            String body = """
            {
              "coordinates": [
                [%f, %f],
                [%f, %f]
              ]
            }
            """.formatted(
                    origin.getLng(), origin.getLat(),
                    destination.getLng(), destination.getLat()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("ORS API returned invalid response: " + response.getStatusCode());
            }

            JsonNode json = objectMapper.readTree(response.getBody());

            if (!json.has("features") || json.get("features").isEmpty()) {
                throw new RuntimeException("ORS returned no route data");
            }

            JsonNode route = json.get("features").get(0);

            double meters = route.at("/properties/segments/0/distance").asDouble();
            double seconds = route.at("/properties/segments/0/duration").asDouble();

            String geometry = route.has("geometry")
                    ? route.get("geometry").toString()
                    : null;

            return new RoutingResult(
                    origin,
                    destination,
                    BigDecimal.valueOf(meters / 1000.0),
                    BigDecimal.valueOf(seconds / 3600.0),
                    name(),
                    geometry
            );

        } catch (Exception e) {
            throw new RuntimeException("ORS routing failed: " + e.getMessage(), e);
        }
    }
}
