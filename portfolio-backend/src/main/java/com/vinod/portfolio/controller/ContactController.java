package com.vinod.portfolio.controller;

import com.vinod.portfolio.model.ContactMessage;
import com.vinod.portfolio.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the contact form.
 *
 *   POST /api/contact        -> save a new message (used by the website)
 *   GET  /api/contact        -> list all messages (admin / personal use)
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // The @Valid annotation triggers the validation rules on ContactMessage.
    // If validation fails, GlobalExceptionHandler returns the field errors.
    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody ContactMessage message) {
        contactService.save(message);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("status", "success",
                             "message", "Thanks for reaching out. I'll get back to you soon."));
    }

    // In a real deployment you would protect this endpoint with authentication.
    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAll() {
        return ResponseEntity.ok(contactService.getAllMessages());
    }
}
