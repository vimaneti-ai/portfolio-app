package com.vinod.portfolio.controller;

import com.vinod.portfolio.model.Project;
import com.vinod.portfolio.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST endpoints for projects.
 *
 *   GET /api/projects                  -> all projects in display order
 *   GET /api/projects?category=backend -> filter by category
 *
 * The Angular Projects section calls these instead of hardcoding cards.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<Project>> getProjects(
            @RequestParam(required = false) String category) {

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            return ResponseEntity.ok(projectService.getProjectsByCategory(category));
        }
        return ResponseEntity.ok(projectService.getAllProjects());
    }
}
