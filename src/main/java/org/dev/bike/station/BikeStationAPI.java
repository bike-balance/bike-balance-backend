package org.dev.bike.station;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BikeStationAPI {

    private final JdbcClient jdbcClient;

    @GetMapping
    public List<BikeStationResponse> findStationsInBounds(
            @RequestParam double south,
            @RequestParam double west,
            @RequestParam double north,
            @RequestParam double east
    ) {
        validateBounds(south, west, north, east);

        return jdbcClient.sql("""
                        SELECT
                            id,
                            rent_id,
                            rent_no,
                            rent_nm,
                            sta_loc,
                            hold_num,
                            sta_add1,
                            sta_add2,
                            ST_Y(geom) AS lat,
                            ST_X(geom) AS lng
                        FROM bike_station
                        WHERE geom && ST_MakeEnvelope(:west, :south, :east, :north, 4326)
                          AND ST_Contains(ST_MakeEnvelope(:west, :south, :east, :north, 4326), geom)
                        ORDER BY rent_nm
                        LIMIT 1000
                        """)
                .param("south", south)
                .param("west", west)
                .param("north", north)
                .param("east", east)
                .query(BikeStationResponse.class)
                .list();
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

    public record BikeStationResponse(
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
}