package org.dev.bike.weather;

public record WeatherResponse(
        String condition,
        String icon,
        double temperature,
        String rainfall,
        String baseDate,
        String baseTime
) {
}
