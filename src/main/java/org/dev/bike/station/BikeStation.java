package org.dev.bike.station;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.geo.Point;

@Entity
@Table(name = "bike_station")
@Getter
public class BikeStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rentId;

    private String rentNm;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geom;
}
