package com.agendapro.controller;

import com.agendapro.dto.DashboardStatsDTO;
import com.agendapro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/establishments/{establishmentId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardStatsDTO> getStats(@PathVariable UUID establishmentId) {
        return ResponseEntity.ok(dashboardService.getDashboardStats(establishmentId));
    }
}
