package com.vinod.portfolio.service;

import com.vinod.portfolio.model.ContactMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
public class ContactEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notification-email}")
    private String notificationEmail;

    public ContactEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendNotification(ContactMessage msg) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(notificationEmail);
        email.setSubject("Portfolio contact from " + msg.getFirstName() + " " + msg.getLastName());
        email.setText(
            "Name:    " + msg.getFirstName() + " " + msg.getLastName() + "\n" +
            "Email:   " + msg.getEmail() + "\n\n" +
            msg.getMessage()
        );
        mailSender.send(email);
    }

    @Async
    public void sendConfirmation(ContactMessage msg) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(msg.getEmail());
        email.setSubject("Got your message — Vinod Kumar Maneti");
        email.setText(
            "Hi " + msg.getFirstName() + ",\n\n" +
            "Thanks for reaching out. I'll get back to you within a couple of days.\n\n" +
            "— Vinod"
        );
        mailSender.send(email);
    }
}
