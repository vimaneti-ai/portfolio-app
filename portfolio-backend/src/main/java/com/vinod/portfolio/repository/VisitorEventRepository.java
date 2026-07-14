package com.vinod.portfolio.repository;

import com.vinod.portfolio.model.VisitorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitorEventRepository extends JpaRepository<VisitorEvent, Long> {

    @Query("SELECT COUNT(DISTINCT v.sessionId) FROM VisitorEvent v")
    long countDistinctSessions();

    @Query("SELECT COUNT(v) FROM VisitorEvent v WHERE v.eventType = :type")
    long countByEventType(@Param("type") String type);

    List<VisitorEvent> findTop20ByOrderByCreatedAtDesc();

    List<VisitorEvent> findAllByCreatedAtAfter(LocalDateTime since);
}