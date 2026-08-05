package org.dev.bike.admin;

import org.dev.bike.station.BikeStationRepository;
import org.dev.bike.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BikeStationRepository bikeStationRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void returnsTotalUserCount() {
        when(userRepository.count()).thenReturn(12L);

        TotalUserCountResponse response = adminDashboardService.getTotalUserCount();

        assertThat(response.totalUserCount()).isEqualTo(12L);
        verify(userRepository).count();
    }

    @Test
    void returnsTotalStationCount() {
        when(bikeStationRepository.count()).thenReturn(2_750L);

        TotalStationCountResponse response = adminDashboardService.getTotalStationCount();

        assertThat(response.totalStationCount()).isEqualTo(2_750L);
        verify(bikeStationRepository).count();
    }
}
