package com.vinod.portfolio.service;

import com.vinod.portfolio.dto.VisitorEventRequest;
import com.vinod.portfolio.model.VisitorEvent;
import com.vinod.portfolio.repository.VisitorEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class VisitorAnalyticsService {
    private final VisitorEventRepository repository;

    public VisitorAnalyticsService(VisitorEventRepository repository) {
        this.repository = repository;
    }

    public void track(VisitorEventRequest req, HttpServletRequest servletRequest) {
        String ip = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        VisitorEvent event = new VisitorEvent();
        event.setSessionId(req.getSessionId());
        event.setEventType(req.getEventType());
        event.setEventName(req.getEventName());
        event.setPageUrl(req.getPageUrl());
        event.setReferrer(req.getReferrer());
        event.setUserAgent(userAgent);
        event.setIpHash(hash(ip));
        event.setIpTruncated(truncateIp(ip));
        event.setBrowser(parseBrowser(userAgent));
        event.setOperatingSystem(parseOs(userAgent));
        event.setDeviceType(parseDevice(userAgent));

        repository.save(event);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp : request.getRemoteAddr();
    }

    private String truncateIp(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) return parts[0] + "." + parts[1] + "." + parts[2] + ".0";
        return ip;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Edg")) return "Edge";
        return "Other";
    }

    private String parseOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }

    private String parseDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone")) return "mobile";
        if (ua.contains("iPad") || ua.contains("Tablet")) return "tablet";
        return "desktop";
    }
}