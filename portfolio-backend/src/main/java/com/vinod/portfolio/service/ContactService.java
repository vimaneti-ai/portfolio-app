package com.vinod.portfolio.service;

import com.vinod.portfolio.model.ContactMessage;
import com.vinod.portfolio.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ContactService {

    private final ContactMessageRepository repository;
    private final ContactEmailService emailService;

    public ContactService(ContactMessageRepository repository, ContactEmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public ContactMessage save(ContactMessage message) {
        message.setFirstName(message.getFirstName().trim());
        message.setLastName(message.getLastName().trim());
        message.setEmail(message.getEmail().trim().toLowerCase());
        message.setMessage(message.getMessage().trim());
        ContactMessage saved = repository.save(message);
        emailService.sendNotification(saved);
        emailService.sendConfirmation(saved);
        return saved;
    }

    public List<ContactMessage> getAllMessages() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
