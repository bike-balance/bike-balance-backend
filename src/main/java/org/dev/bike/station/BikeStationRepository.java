package org.dev.bike.station;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BikeStationRepository {

    private final JdbcClient jdbcClient;

    public List<BikeStationMarkerResponse> findStationsInBounds(
            double south,
            double west,
            double north,
            double east
    ) {
        return jdbcClient.sql("""
                        SELECT
                            id,
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
                .query(BikeStationMarkerResponse.class)
                .list();
    }

    public Optional<BikeStationDetail> findStationDetail(Long id) {
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
                        WHERE id = :stationId
                        """)
                .param("stationId", id)
                .query(BikeStationDetail.class)
                .optional();
    }
}
