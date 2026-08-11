package org.dev.bike.weather;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmaGridConverterTest {

    private final KmaGridConverter converter = new KmaGridConverter();

    @Test
    void convertsSeoulCityHallToKmaGrid() {
        GridCoordinate coordinate = converter.convert(37.5666805, 126.9784147);

        assertThat(coordinate).isEqualTo(new GridCoordinate(60, 127));
    }
}
