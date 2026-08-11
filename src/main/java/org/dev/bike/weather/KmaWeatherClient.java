package org.dev.bike.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class KmaWeatherClient {

    private static final String API_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/%s";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final String encodedApiKey;

    public KmaWeatherClient(@Value("${kma.weather.api-key}") String apiKey) {
        // data.go.kr에서 받은 일반 인증키와 URL 인코딩 인증키를 모두 허용한다.
        String decodedApiKey = apiKey.contains("%")
                ? URLDecoder.decode(apiKey, StandardCharsets.UTF_8)
                : apiKey;
        this.encodedApiKey = URLEncoder.encode(decodedApiKey, StandardCharsets.UTF_8);
    }

    public Map<String, String> getCurrentConditions(GridCoordinate grid) {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        LocalDateTime observationBase = now.minusMinutes(45).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime forecastBase = now.minusMinutes(45)
                .withMinute(now.minusMinutes(45).getMinute() < 30 ? 0 : 30)
                .withSecond(0).withNano(0);

        Map<String, String> values = new HashMap<>();
        values.putAll(request("getUltraSrtNcst", observationBase, grid, false));
        Map<String, String> forecast = request("getUltraSrtFcst", forecastBase, grid, true);
        // 실황 API에 없는 SKY만 예보에서 보완하고, 강수형태는 실황값이 없을 때만 사용한다.
        if (forecast.containsKey("SKY")) values.put("SKY", forecast.get("SKY"));
        if (!values.containsKey("PTY") && forecast.containsKey("PTY")) {
            values.put("PTY", forecast.get("PTY"));
        }
        values.put("baseDate", observationBase.format(DATE));
        values.put("baseTime", observationBase.format(TIME));
        return values;
    }

    private Map<String, String> request(
            String operation, LocalDateTime base, GridCoordinate grid, boolean selectFirstForecast
    ) {
        String query = "serviceKey=" + encodedApiKey
                + "&pageNo=1&numOfRows=1000&dataType=JSON"
                + "&base_date=" + base.format(DATE)
                + "&base_time=" + base.format(TIME)
                + "&nx=" + grid.nx() + "&ny=" + grid.ny();
        URI uri = URI.create(API_URL.formatted(operation) + "?" + query);

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("기상청 API HTTP 오류: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body()).path("response");
            String resultCode = root.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                throw new IllegalStateException("기상청 API 오류: "
                        + root.path("header").path("resultMsg").asText(resultCode));
            }

            JsonNode items = root.path("body").path("items").path("item");
            Map<String, String> result = new HashMap<>();
            String firstForecastTime = null;
            for (JsonNode item : items) {
                if (selectFirstForecast) {
                    String forecastTime = item.path("fcstDate").asText() + item.path("fcstTime").asText();
                    if (firstForecastTime == null) firstForecastTime = forecastTime;
                    if (!firstForecastTime.equals(forecastTime)) continue;
                }
                String category = item.path("category").asText();
                String value = selectFirstForecast
                        ? item.path("fcstValue").asText()
                        : item.path("obsrValue").asText();
                result.put(category, value);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("기상청 응답을 처리하지 못했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("기상청 요청이 중단되었습니다.", e);
        }
    }
}
