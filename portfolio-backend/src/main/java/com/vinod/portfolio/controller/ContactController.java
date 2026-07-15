package com.vinod.portfolio.controller;

import com.vinod.portfolio.model.ContactMessage;
import com.vinod.portfolio.service.ContactService;
import com.vinod.portfolio.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;
    private final RateLimiterService rateLimiter;

    public ContactController(ContactService contactService, RateLimiterService rateLimiter) {
        this.contactService = contactService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(
            @Valid @RequestBody ContactMessage message,
            HttpServletRequest request) {

        String ip = clientIp(request);
        if (!rateLimiter.tryConsume(ip)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "error",
                                 "message", "Too many submissions. Please wait before trying again."));
        }

        contactService.save(message);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("status", "success",
                             "message", "Thanks for reaching out. I'll get back to you soon."));
    }

    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAll() {
        return ResponseEntity.ok(contactService.getAllMessages());
    }

    // CloudFront sets X-Forwarded-For; fall back to remoteAddr for local dev
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
