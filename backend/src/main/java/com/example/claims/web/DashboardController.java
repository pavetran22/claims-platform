package com.example.claims.web;

import com.example.claims.claim.Actor;
import com.example.claims.claim.Role;
import com.example.claims.dashboard.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/workload")
    public DashboardService.Workload workload(@RequestHeader("X-User-Id") String userId,
                                              @RequestHeader("X-User-Role") Role role) {
        return dashboard.workload(new Actor(userId, role));
    }

    @GetMapping("/exposure")
    public List<DashboardService.MarketExposure> exposure(@RequestHeader("X-User-Id") String userId,
                                                          @RequestHeader("X-User-Role") Role role) {
        return dashboard.exposure(new Actor(userId, role));
    }
}
