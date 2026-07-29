package org.dev.bike.station;

public record BikeStationDetailResponse(
        Long id,
        String rentId,
        String rentNo,
        String rentNm,
        String staLoc,
        Integer holdNum,
        String staAdd1,
        String staAdd2,
        Double lat,
        Double lng,
        Integer realtimeBikeCount
) {
    public static BikeStationDetailResponse from(BikeStationDetail station, Integer realtimeBikeCount) {
        return new BikeStationDetailResponse(
                station.id(),
                station.rentId(),
                station.rentNo(),
                station.rentNm(),
                station.staLoc(),
                station.holdNum(),
                station.staAdd1(),
                station.staAdd2(),
                station.lat(),
                station.lng(),
                realtimeBikeCount
        );
    }
}
