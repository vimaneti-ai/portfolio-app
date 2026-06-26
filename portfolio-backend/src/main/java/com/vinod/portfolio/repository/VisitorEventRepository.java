package com.vinod.portfolio.repository;

import com.vinod.portfolio.model.VisitorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorEventRepository extends JpaRepository<VisitorEvent, Long> {
}