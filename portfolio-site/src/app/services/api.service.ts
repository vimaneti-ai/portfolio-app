import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ContactRequest, ContactResponse, VisitorEventRequest } from '../models/models';

/**
 * Single service that talks to the Spring Boot backend.
 * Both the projects list and the contact form go through here.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly baseUrl =
    ['localhost', '127.0.0.1'].includes(window.location.hostname)
      ? 'http://localhost:8080/api'
      : '/api';

  constructor(private http: HttpClient) {}

  trackVisitorEvent(payload: VisitorEventRequest): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.baseUrl}/analytics/track`, payload);
  }

  // ----- Projects -----
  getProjects(category: string = 'all'): Observable<Project[]> {
    const url =
      category && category !== 'all'
        ? `${this.baseUrl}/projects?category=${category}`
        : `${this.baseUrl}/projects`;
    return this.http.get<Project[]>(url);
  }

  // ----- Contact -----
  sendMessage(payload: ContactRequest): Observable<ContactResponse> {
    return this.http.post<ContactResponse>(`${this.baseUrl}/contact`, payload);
  }
}
