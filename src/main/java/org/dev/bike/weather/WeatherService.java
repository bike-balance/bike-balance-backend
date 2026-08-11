package org.dev.bike.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final KmaGridConverter gridConverter;
    private final KmaWeatherClient weatherClient;

    public WeatherResponse getCurrentWeather(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("올바른 위도와 경도를 입력해 주세요.");
        }

        Map<String, String> values = weatherClient.getCurrentConditions(
                gridConverter.convert(latitude, longitude));
        String precipitationType = values.getOrDefault("PTY", "0");
        String sky = values.getOrDefault("SKY", "1");
        Condition condition = resolveCondition(precipitationType, sky);
        double temperature = parseTemperature(values.get("T1H"));
        String rainfall = formatRainfall(values.get("RN1"));

        return new WeatherResponse(condition.label, condition.icon, temperature, rainfall,
                values.get("baseDate"), values.get("baseTime"));
    }

    private Condition resolveCondition(String pty, String sky) {
        return switch (pty) {
            case "1", "5" -> new Condition("비", "🌧️");
            case "2",  "6" -> new Condition("비/눈", "🌨️");
            case "3", "7" -> new Condition("눈", "❄️");
            default -> switch (sky) {
                case "3" -> new Condition("구름많음", "⛅");
                case "4" -> new Condition("흐림", "☁️");
                default -> new Condition("맑음", "☀️");
            };
        };
    }

    private double parseTemperature(String value) {
        if (value == null) throw new IllegalStateException("기온 정보가 없습니다.");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("올바르지 않은 기온 정보입니다.", e);
        }
    }

    private String formatRainfall(String value) {
        if (value == null || value.isBlank() || "0".equals(value) || "강수없음".equals(value)) {
            return "없음";
        }
        return value.contains("mm") ? value : value + "mm";
    }

    private record Condition(String label, String icon) {
    }
}
