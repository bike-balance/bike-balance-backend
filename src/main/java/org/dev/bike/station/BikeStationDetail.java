package org.dev.bike.station;

public record BikeStationDetail(
        Long id,
        String rentId,
        String rentNo,
        String rentNm,
        String staLoc,
        Integer holdNum,
        String staAdd1,
        String staAdd2,
        Double lat,
        Double lng
) {
}
