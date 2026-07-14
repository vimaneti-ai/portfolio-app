import { Component, AfterViewInit } from '@angular/core';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements AfterViewInit {
  stats = { years: 0, companies: 0, projects: 0 };
  activeSection = 'about';

  readonly sections = [
    { id: 'about',      label: 'About' },
    { id: 'experience', label: 'Experience' },
    { id: 'projects',   label: 'Projects' },
    { id: 'skills',     label: 'Skills' },
    { id: 'contact',    label: 'Contact' },
  ];

  constructor(private api: ApiService) {}

  closeMenu() {}

  ngOnInit() {
    this.trackEvent('page_view', 'site_loaded');
    const targets = { years: 6, companies: 2, projects: 4 };
    const duration = 1800;
    const start = performance.now();
    const tick = (now: number) => {
      const p = Math.min((now - start) / duration, 1);
      const e = 1 - Math.pow(1 - p, 3);
      this.stats.years     = Math.round(e * targets.years);
      this.stats.companies = Math.round(e * targets.companies);
      this.stats.projects  = Math.round(e * targets.projects);
      if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }

  ngAfterViewInit() {
    const io = new IntersectionObserver((entries) => {
      entries.forEach(e => { if (e.isIntersecting) e.target.classList.add('in'); });
    }, { threshold: 0.12 });
    document.querySelectorAll('.reveal').forEach(el => io.observe(el));

    const sectionIo = new IntersectionObserver((entries) => {
      entries.forEach(e => {
        if (e.isIntersecting) this.activeSection = e.target.id;
      });
    }, { threshold: 0.3 });
    this.sections.forEach(s => {
      const el = document.getElementById(s.id);
      if (el) sectionIo.observe(el);
    });
  }

  private getSessionId(): string {
    const key = 'visitorSessionId';
    let sessionId = localStorage.getItem(key);
    if (!sessionId) {
      sessionId = crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      localStorage.setItem(key, sessionId);
    }
    return sessionId;
  }

  trackEvent(eventType: string, eventName: string) {
    this.api.trackVisitorEvent({
      sessionId: this.getSessionId(),
      eventType,
      eventName,
      pageUrl: window.location.pathname,
      referrer: document.referrer
    }).subscribe({ error: () => {} });
  }
}
