package com.vinod.portfolio.service;

import com.vinod.portfolio.model.Project;
import com.vinod.portfolio.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Business logic for projects.
 */
@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public List<Project> getAllProjects() {
        return repository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Project> getProjectsByCategory(String category) {
        return repository.findByCategoryOrderByDisplayOrderAsc(category);
    }
}
