// Shared TypeScript interfaces for the API responses.

export interface Project {
  id: number;
  title: string;
  kicker: string;
  description: string;
  metric: string;
  category: 'frontend' | 'backend' | 'fullstack';
  tags: string;          // comma-separated, split in the component
  displayOrder: number;
}

export interface ContactRequest {
  firstName: string;
  lastName: string;
  email: string;
  message: string;
}

export interface ContactResponse {
  status: string;
  message: string;
}
