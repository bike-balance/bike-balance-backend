package org.dev.bike.admin;

import lombok.RequiredArgsConstructor;
import org.dev.bike.station.BikeStationRepository;
import org.dev.bike.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BikeStationRepository bikeStationRepository;

    public TotalUserCountResponse getTotalUserCount() {
        return new TotalUserCountResponse(userRepository.count());
    }

    public TotalStationCountResponse getTotalStationCount() {
        return new TotalStationCountResponse(bikeStationRepository.count());
    }
}
