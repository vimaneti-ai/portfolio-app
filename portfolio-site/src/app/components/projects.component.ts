import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../services/api.service';
import { Project } from '../models/models';

/**
 * Projects section. Loads projects from the backend on init,
 * and supports filtering by category (all / frontend / backend / fullstack).
 */
@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css'],
})
export class ProjectsComponent implements OnInit {

  projects: Project[] = [];
  loading = true;
  error = '';
  activeFilter = 'all';

  filters = [
    { key: 'all', label: 'All' },
    { key: 'frontend', label: 'Frontend' },
    { key: 'backend', label: 'Backend' },
    { key: 'fullstack', label: 'Full-stack' },
  ];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.error = '';
    this.api.getProjects(this.activeFilter).subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load projects. Please try again later.';
        this.loading = false;
      },
    });
  }

  setFilter(key: string): void {
    if (this.activeFilter === key) return;
    this.activeFilter = key;
    this.loadProjects();
  }

  // Turns "Angular,TypeScript,SQL" into an array for the template.
  tagList(tags: string): string[] {
    return tags ? tags.split(',').map((t) => t.trim()) : [];
  }
}
