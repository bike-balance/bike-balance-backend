package org.dev.bike.station;

public record BikeStationNearbyResponse(
        Long id,
        String rentNm,
        Double lat,
        Double lng,
        Double distanceMeters
) {
}
