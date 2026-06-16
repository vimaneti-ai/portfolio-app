package com.vinod.portfolio.repository;

import com.vinod.portfolio.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access for contact messages.
 * Spring Data JPA generates the implementation automatically.
 */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // Returns newest messages first, for the admin view.
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
}
