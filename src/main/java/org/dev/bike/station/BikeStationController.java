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

    @GetMapping("/{id}")
    public BikeStationDetailResponse findStationDetail(@PathVariable Long id) {
        return bikeStationService.findStationDetail(id);
    }
}
