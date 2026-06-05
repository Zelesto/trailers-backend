package com.pgsa.trailers.service.routing.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.service.routing.*;
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
        return true;
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

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode route = json.get("features").get(0);

            double meters = route.at("/properties/segments/0/distance").asDouble();
            double seconds = route.at("/properties/segments/0/duration").asDouble();

            return new RoutingResult(
                    origin,
                    destination,
                    BigDecimal.valueOf(meters / 1000),
                    BigDecimal.valueOf(seconds / 3600),
                    name(),
                    route.get("geometry").toString()
            );

        } catch (Exception e) {
            throw new RuntimeException("ORS failed", e);
        }
    }
}
