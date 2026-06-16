package com.vinod.portfolio.repository;

import com.vinod.portfolio.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access for projects.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Projects in the order they should appear on the page.
    List<Project> findAllByOrderByDisplayOrderAsc();

    // Used by the category filter (frontend / backend / fullstack).
    List<Project> findByCategoryOrderByDisplayOrderAsc(String category);
}
