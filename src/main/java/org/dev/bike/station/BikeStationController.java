package org.dev.bike.station;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BikeStationController {

    private final BikeStationService bikeStationService;

    @GetMapping
    public List<BikeStationMarkerResponse> findStationsInBounds(
            @RequestParam double south,
            @RequestParam double west,
            @RequestParam double north,
            @RequestParam double east
    ) {
        return bikeStationService.findStationsInBounds(south, west, north, east);
    }
    // 현재 위치 기반 가까운 대여소 5곳 조회 api
    @GetMapping("/nearby")
    public List<BikeStationNearbyResponse> findNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer limit
    ) {
        return bikeStationService.findNearbyStations(lat, lng, limit);
    }

    @GetMapping("/{id}")
    public BikeStationDetailResponse findStationDetail(@PathVariable Long id) {
        return bikeStationService.findStationDetail(id);
    }
}
