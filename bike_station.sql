CREATE TABLE bike_station (
                              id        BIGSERIAL PRIMARY KEY,
                              rent_id   VARCHAR(20) NOT NULL UNIQUE,
                              rent_no   VARCHAR(20),
                              rent_nm   VARCHAR(100),
                              sta_loc   VARCHAR(50),
                              hold_num  INT,
                              sta_add1  VARCHAR(200),
                              sta_add2  VARCHAR(200),
                              geom      GEOMETRY(Point, 4326) NOT NULL
);

CREATE INDEX idx_bike_station_geom
    ON bike_station
        USING GIST (geom);

CREATE INDEX idx_sta_loc
    ON bike_station (sta_loc);

-- CREATE TABLE bike_station (
--                               id       BIGSERIAL PRIMARY KEY,
--                               rent_id  VARCHAR(20) NOT NULL UNIQUE,
--                               rent_no  VARCHAR(20),
--                               rent_nm  VARCHAR(100),
--                               sta_loc  VARCHAR(50),
--                               hold_num INT,
--                               sta_add1 VARCHAR(200),
--                               sta_add2 VARCHAR(200),
--                               sta_lat  NUMERIC(10,8),
--                               sta_long NUMERIC(11,8)
-- );
-- CREATE INDEX idx_sta_loc ON bike_station (sta_loc);