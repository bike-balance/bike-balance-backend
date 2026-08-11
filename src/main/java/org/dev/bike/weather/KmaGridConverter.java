package org.dev.bike.weather;

import org.springframework.stereotype.Component;

@Component
public class KmaGridConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    public GridCoordinate convert(double latitude, double longitude) {
        double radius = EARTH_RADIUS_KM / GRID_KM;
        double slat1 = Math.toRadians(STANDARD_LATITUDE_1);
        double slat2 = Math.toRadians(STANDARD_LATITUDE_2);
        double olon = Math.toRadians(ORIGIN_LONGITUDE);
        double olat = Math.toRadians(ORIGIN_LATITUDE);

        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2))
                / Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn)
                * Math.cos(slat1) / sn;
        double ro = radius * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);
        double ra = radius * sf
                / Math.pow(Math.tan(Math.PI * 0.25 + Math.toRadians(latitude) * 0.5), sn);
        double theta = Math.toRadians(longitude) - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new GridCoordinate(nx, ny);
    }
}
