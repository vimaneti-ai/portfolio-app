package com.vinod.portfolio.service;

import com.vinod.portfolio.model.ContactMessage;
import com.vinod.portfolio.repository.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactMessageRepository repository;

    @Mock
    private ContactEmailService emailService;

    @InjectMocks
    private ContactService contactService;

    private ContactMessage message;

    @BeforeEach
    void setUp() {
        message = new ContactMessage();
        message.setFirstName("  Vinod  ");
        message.setLastName("  Maneti  ");
        message.setEmail("  VINOD@EXAMPLE.COM  ");
        message.setMessage("  Hello there  ");
    }

    @Test
    void save_trimsWhitespaceFromAllFields() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContactMessage result = contactService.save(message);

        assertThat(result.getFirstName()).isEqualTo("Vinod");
        assertThat(result.getLastName()).isEqualTo("Maneti");
        assertThat(result.getMessage()).isEqualTo("Hello there");
    }

    @Test
    void save_lowercasesEmail() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContactMessage result = contactService.save(message);

        assertThat(result.getEmail()).isEqualTo("vinod@example.com");
    }

    @Test
    void save_persistsToRepository() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        contactService.save(message);

        verify(repository, times(1)).save(any(ContactMessage.class));
    }

    @Test
    void save_sendsNotificationAndConfirmationEmails() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        contactService.save(message);

        verify(emailService, times(1)).sendNotification(any(ContactMessage.class));
        verify(emailService, times(1)).sendConfirmation(any(ContactMessage.class));
    }

    @Test
    void save_returnsSavedMessage() {
        ContactMessage saved = new ContactMessage();
        saved.setId(42L);
        when(repository.save(any())).thenReturn(saved);

        ContactMessage result = contactService.save(message);

        assertThat(result.getId()).isEqualTo(42L);
    }

    @Test
    void getAllMessages_delegatesToRepository() {
        ContactMessage msg = new ContactMessage();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(msg));

        List<ContactMessage> result = contactService.getAllMessages();

        assertThat(result).hasSize(1);
        verify(repository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllMessages_returnsEmptyListWhenNoMessages() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ContactMessage> result = contactService.getAllMessages();

        assertThat(result).isEmpty();
    }
}
