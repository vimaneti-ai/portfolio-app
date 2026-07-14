package com.vinod.portfolio.service;

import com.vinod.portfolio.model.ContactMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ContactEmailService emailService;

    private ContactMessage message;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "notificationEmail", "vinodben594@gmail.com");

        message = new ContactMessage();
        message.setFirstName("Jane");
        message.setLastName("Doe");
        message.setEmail("jane@example.com");
        message.setMessage("I'd love to connect.");
    }

    @Test
    void sendNotification_sendsToNotificationEmail() {
        emailService.sendNotification(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("vinodben594@gmail.com");
    }

    @Test
    void sendNotification_subjectContainsSenderName() {
        emailService.sendNotification(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("Jane", "Doe");
    }

    @Test
    void sendNotification_bodyContainsSenderEmailAndMessage() {
        emailService.sendNotification(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String body = captor.getValue().getText();
        assertThat(body).contains("jane@example.com");
        assertThat(body).contains("I'd love to connect.");
    }

    @Test
    void sendConfirmation_sendsToSubmitterEmail() {
        emailService.sendConfirmation(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("jane@example.com");
    }

    @Test
    void sendConfirmation_hasCorrectSubject() {
        emailService.sendConfirmation(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Got your message — Vinod Kumar Maneti");
    }

    @Test
    void sendConfirmation_bodyAddressesSubmitterByFirstName() {
        emailService.sendConfirmation(message);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Hi Jane");
    }

    @Test
    void sendNotification_callsMailSenderExactlyOnce() {
        emailService.sendNotification(message);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendConfirmation_callsMailSenderExactlyOnce() {
        emailService.sendConfirmation(message);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
