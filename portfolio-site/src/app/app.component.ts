import { Component, HostListener, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements AfterViewInit {
  navScrolled = false;
  menuOpen = false;
  stats = { years: 0, companies: 0, projects: 0 };

  @HostListener('window:scroll')
  onScroll() {
    this.navScrolled = window.scrollY > 20;
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
  closeMenu()  { this.menuOpen = false; }

  ngOnInit() {
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
  }
}
