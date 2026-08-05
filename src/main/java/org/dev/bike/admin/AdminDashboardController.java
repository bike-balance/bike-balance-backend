package org.dev.bike.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/users/count")
    public ResponseEntity<TotalUserCountResponse> getTotalUserCount() {
        return ResponseEntity.ok(adminDashboardService.getTotalUserCount());
    }

    @GetMapping("/stations/count")
    public ResponseEntity<TotalStationCountResponse> getTotalStationCount() {
        return ResponseEntity.ok(adminDashboardService.getTotalStationCount());
    }
}
