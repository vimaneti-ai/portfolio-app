import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { AnalyticsSummary, CountItem } from '../models/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  username = '';
  password = '';
  loggedIn = false;
  error = '';
  loading = false;
  summary: AnalyticsSummary | null = null;

  constructor(private api: ApiService) {}

  ngOnInit() {}

  login() {
    this.error = '';
    this.loading = true;
    this.api.getAnalyticsSummary(this.username, this.password).subscribe({
      next: (data) => {
        this.summary = data;
        this.loggedIn = true;
        this.loading = false;
      },
      error: () => {
        this.error = 'Invalid credentials';
        this.loading = false;
      }
    });
  }

  maxCount(items: CountItem[]): number {
    return items.length ? Math.max(...items.map(i => i.count)) : 1;
  }

  barWidth(count: number, items: CountItem[]): string {
    return `${Math.round((count / this.maxCount(items)) * 100)}%`;
  }

  maxDailyCount(): number {
    if (!this.summary?.last30Days.length) return 1;
    return Math.max(...this.summary.last30Days.map(d => d.count));
  }

  barHeight(count: number): string {
    return `${Math.round((count / this.maxDailyCount()) * 100)}%`;
  }
}
