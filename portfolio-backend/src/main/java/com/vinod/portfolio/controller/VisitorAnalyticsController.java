package com.vinod.portfolio.controller;

import com.vinod.portfolio.dto.AnalyticsSummaryDTO;
import com.vinod.portfolio.dto.VisitorEventRequest;
import com.vinod.portfolio.service.AnalyticsDashboardService;
import com.vinod.portfolio.service.VisitorAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class VisitorAnalyticsController {

    private final VisitorAnalyticsService service;
    private final AnalyticsDashboardService dashboardService;

    public VisitorAnalyticsController(VisitorAnalyticsService service,
                                      AnalyticsDashboardService dashboardService) {
        this.service = service;
        this.dashboardService = dashboardService;
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> track(
            @RequestBody VisitorEventRequest request,
            HttpServletRequest servletRequest) {
        service.track(request, servletRequest);
        return ResponseEntity.ok(Map.of("status", "tracked"));
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}