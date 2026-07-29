package org.dev.bike.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class SeoulBikeRealtimeClient {

    private static final String REALTIME_STATION_API_FORMAT =
            "http://openapi.seoul.go.kr:8088/%s/json/bikeList/1/1/%s";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${seoul-bike.api-key}")
    private String seoulBikeApiKey;

    public Integer findRealtimeBikeCount(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return null;
        }

        try {
            String encodedStationId = URLEncoder.encode(stationId, StandardCharsets.UTF_8);
            String encodedApiKey = URLEncoder.encode(seoulBikeApiKey, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            REALTIME_STATION_API_FORMAT.formatted(encodedApiKey, encodedStationId)))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode row = objectMapper.readTree(response.body()).path("rentBikeStatus").path("row");
            if (!row.isArray() || row.isEmpty()) {
                return null;
            }

            return parseInteger(row.get(0).path("parkingBikeTotCnt"));
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private Integer parseInteger(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isInt()) {
            return node.intValue();
        }

        try {
            return Integer.parseInt(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
