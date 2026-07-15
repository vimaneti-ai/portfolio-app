package com.vinod.portfolio.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.vinod.portfolio.dto.VisitorEventRequest;
import com.vinod.portfolio.model.VisitorEvent;
import com.vinod.portfolio.repository.VisitorEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class VisitorAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(VisitorAnalyticsService.class);

    private final VisitorEventRepository repository;
    private DatabaseReader geoReader;

    public VisitorAnalyticsService(VisitorEventRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void initGeoDb() {
        try {
            InputStream is = new ClassPathResource("GeoLite2-City.mmdb").getInputStream();
            geoReader = new DatabaseReader.Builder(is).build();
            log.info("GeoLite2-City database loaded");
        } catch (Exception e) {
            log.warn("GeoLite2-City.mmdb not found — location lookup disabled. "
                   + "Download from maxmind.com and place in src/main/resources/");
        }
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

        if (!isPrivateOrLocalIp(ip)) {
            GeoLocation loc = lookupLocation(ip);
            if (loc != null) {
                event.setCountry(loc.country());
                event.setRegion(loc.region());
                event.setCity(loc.city());
                event.setTimezone(loc.timezone());
            }
        }

        repository.save(event);
    }

    private GeoLocation lookupLocation(String ip) {
        if (geoReader == null) return null;
        try {
            InetAddress addr = InetAddress.getByName(ip);
            CityResponse response = geoReader.city(addr);
            return new GeoLocation(
                    response.getCountry().getName(),
                    response.getMostSpecificSubdivision().getName(),
                    response.getCity().getName(),
                    response.getLocation().getTimeZone()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp : request.getRemoteAddr();
    }

    private boolean isPrivateOrLocalIp(String ip) {
        if (ip == null || ip.isBlank()) return true;
        return ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
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

    // Order matters: Edge contains "Chrome", so check Edg first
    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg"))     return "Edge";
        if (ua.contains("Chrome"))  return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari"))  return "Safari";
        return "Other";
    }

    private String parseOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows"))              return "Windows";
        if (ua.contains("Android"))              return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS"))               return "macOS";
        if (ua.contains("Linux"))                return "Linux";
        return "Other";
    }

    private String parseDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone")) return "mobile";
        if (ua.contains("iPad") || ua.contains("Tablet")) return "tablet";
        return "desktop";
    }

    private record GeoLocation(String country, String region, String city, String timezone) {}
}
