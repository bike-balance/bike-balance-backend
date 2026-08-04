package org.dev.bike.station;

import lombok.RequiredArgsConstructor;
import org.dev.bike.redis.RealtimeBikeCountCache;
import org.dev.bike.redis.SeoulBikeRealtimeClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BikeStationService {

    private final BikeStationRepository bikeStationRepository;
    private final SeoulBikeRealtimeClient seoulBikeRealtimeClient;
    private final RealtimeBikeCountCache realtimeBikeCountCache;

    public List<BikeStationMarkerResponse> findStationsInBounds(
            double south,
            double west,
            double north,
            double east
    ) {
        validateBounds(south, west, north, east);
        return bikeStationRepository.findStationsInBounds(south, west, north, east);
    }

    public BikeStationDetailResponse findStationDetail(Long id) {
        BikeStationDetail station = bikeStationRepository.findStationDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대여소를 찾을 수 없습니다."));
        Integer realtimeBikeCount = realtimeBikeCountCache.get(
                station.rentId(),
                () -> seoulBikeRealtimeClient.findRealtimeBikeCount(station.rentId())
        );

        return BikeStationDetailResponse.from(station, realtimeBikeCount);
    }

    public List<BikeStationNearbyResponse> findNearbyStations(double lat, double lng, Integer limit) {
        validateCoordinate(lat, lng);

        int normalizedLimit = limit == null ? 5 : limit;
        if (normalizedLimit < 1 || normalizedLimit > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 개수는 1개 이상 20개 이하만 가능합니다.");
        }

        return bikeStationRepository.findNearbyStations(lat, lng, normalizedLimit);
    }

    private void validateBounds(double south, double west, double north, double east) {
        if (!Double.isFinite(south) || !Double.isFinite(west) || !Double.isFinite(north) || !Double.isFinite(east)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지도 범위가 올바르지 않습니다.");
        }
        if (south < -90 || south > 90 || north < -90 || north > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "위도 범위가 올바르지 않습니다.");
        }
        if (west < -180 || west > 180 || east < -180 || east > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경도 범위가 올바르지 않습니다.");
        }
        if (south > north || west > east) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지도 범위가 올바르지 않습니다.");
        }
    }

    private void validateCoordinate(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "좌표가 올바르지 않습니다.");
        }
        if (lat < -90 || lat > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "위도 범위가 올바르지 않습니다.");
        }
        if (lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경도 범위가 올바르지 않습니다.");
        }
    }
}
