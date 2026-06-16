package com.vinod.portfolio.service;

import com.vinod.portfolio.model.ContactMessage;
import com.vinod.portfolio.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Business logic for contact messages.
 * Keeping logic here (not in the controller) follows the standard
 * controller -> service -> repository layering.
 */
@Service
public class ContactService {

    private final ContactMessageRepository repository;

    public ContactService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    public ContactMessage save(ContactMessage message) {
        // Basic sanitization: trim whitespace before storing.
        message.setFirstName(message.getFirstName().trim());
        message.setLastName(message.getLastName().trim());
        message.setEmail(message.getEmail().trim().toLowerCase());
        message.setMessage(message.getMessage().trim());
        return repository.save(message);
    }

    public List<ContactMessage> getAllMessages() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
