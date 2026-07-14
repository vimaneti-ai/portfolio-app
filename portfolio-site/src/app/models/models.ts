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
export interface VisitorEventRequest {
  sessionId: string;
  eventType: string;
  eventName?: string;
  pageUrl: string;
  referrer?: string;
}

export interface CountItem { name: string; count: number; }
export interface DailyCount { date: string; count: number; }
export interface RecentEvent {
  eventType: string; eventName: string; pageUrl: string;
  country: string; city: string; browser: string;
  deviceType: string; createdAt: string;
}
export interface AnalyticsSummary {
  totalEvents: number;
  uniqueSessions: number;
  pageViews: number;
  browsers: CountItem[];
  operatingSystems: CountItem[];
  deviceTypes: CountItem[];
  countries: CountItem[];
  last30Days: DailyCount[];
  recentEvents: RecentEvent[];
}