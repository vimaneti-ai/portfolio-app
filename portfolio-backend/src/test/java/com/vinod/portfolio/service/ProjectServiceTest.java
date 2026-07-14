package com.vinod.portfolio.service;

import com.vinod.portfolio.model.Project;
import com.vinod.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void getAllProjects_returnsProjectsInDisplayOrder() {
        Project p1 = project("Portfolio Site", "fullstack", 1);
        Project p2 = project("Analytics Dashboard", "backend", 2);
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(p1, p2));

        List<Project> result = projectService.getAllProjects();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Portfolio Site");
        assertThat(result.get(1).getTitle()).isEqualTo("Analytics Dashboard");
        verify(repository, times(1)).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    void getAllProjects_returnsEmptyListWhenNoProjects() {
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        List<Project> result = projectService.getAllProjects();

        assertThat(result).isEmpty();
    }

    @Test
    void getProjectsByCategory_filtersToRequestedCategory() {
        Project p = project("Portfolio Site", "fullstack", 1);
        when(repository.findByCategoryOrderByDisplayOrderAsc("fullstack")).thenReturn(List.of(p));

        List<Project> result = projectService.getProjectsByCategory("fullstack");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("fullstack");
        verify(repository, times(1)).findByCategoryOrderByDisplayOrderAsc("fullstack");
    }

    @Test
    void getProjectsByCategory_returnsEmptyListForUnknownCategory() {
        when(repository.findByCategoryOrderByDisplayOrderAsc("unknown")).thenReturn(List.of());

        List<Project> result = projectService.getProjectsByCategory("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void getProjectsByCategory_passesExactCategoryStringToRepository() {
        when(repository.findByCategoryOrderByDisplayOrderAsc("backend")).thenReturn(List.of());

        projectService.getProjectsByCategory("backend");

        verify(repository).findByCategoryOrderByDisplayOrderAsc("backend");
        verify(repository, never()).findByCategoryOrderByDisplayOrderAsc("frontend");
    }

    private Project project(String title, String category, int order) {
        Project p = new Project();
        p.setTitle(title);
        p.setCategory(category);
        p.setDisplayOrder(order);
        return p;
    }
}
