import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ContactRequest, ContactResponse } from '../models/models';

/**
 * Single service that talks to the Spring Boot backend.
 * Both the projects list and the contact form go through here.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  // In production, point this at your deployed backend URL.
  private readonly baseUrl = 'http://3.138.107.152:8080/api';

  constructor(private http: HttpClient) {}

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
