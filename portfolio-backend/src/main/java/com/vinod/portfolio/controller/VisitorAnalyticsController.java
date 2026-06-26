package com.vinod.portfolio.controller;

import com.vinod.portfolio.dto.VisitorEventRequest;
import com.vinod.portfolio.service.VisitorAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class VisitorAnalyticsController {
    private final VisitorAnalyticsService service;

    public VisitorAnalyticsController(VisitorAnalyticsService service) {
        this.service = service;
    }

    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> track(
            @RequestBody VisitorEventRequest request,
            HttpServletRequest servletRequest
    ) {
        service.track(request, servletRequest);
        return ResponseEntity.ok(Map.of("status", "tracked"));
    }
}