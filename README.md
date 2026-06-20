# Portfolio Website — Full-Stack Build

A personal portfolio site built with **Angular 17 + Spring Boot + MySQL**.

GitHub: **https://github.com/vimaneti-ai/portfolio-app**

---

## What this app does

Three features make this a real full-stack app instead of a static page:

1. **Projects from a database** — the Projects section loads dynamically from `GET /api/projects`
2. **Contact form** — Angular reactive form → Spring Boot REST API → MySQL
3. **Email notifications** — every contact submission triggers a Gmail notification to the site owner and a confirmation reply to the sender (JavaMailSender + Gmail SMTP, async)

---

## Architecture

```
   Browser
      |
      v
  Angular 17 app  (portfolio-site/)
      |   REST (JSON)
      v
  Spring Boot API   ──  Controller → Service → Repository
      |   JPA / Hibernate
      v
  MySQL database    (contact_messages, projects)
```

---

## Project structure

```
portfolio-app/
  portfolio-backend/        Spring Boot app (Java 17, Maven)
    src/main/java/com/vinod/portfolio/
      controller/           REST endpoints (contact, projects, error handling)
      service/              business logic
      repository/           Spring Data JPA interfaces
      model/                JPA entities (ContactMessage, Project)
      config/               CORS configuration (WebConfig.java)
    src/main/resources/
      application.properties          MySQL connection (gitignored)
      application.properties.example  Template — copy and fill in credentials
    pom.xml

  portfolio-frontend/       Source components (not a runnable app)
    src/app/
      components/           ProjectsComponent + ContactComponent (standalone)
      services/             api.service.ts
      models/               TypeScript interfaces

  portfolio-site/           Live Angular 17 app (NgModule-based)
    src/
      app/
        app.component.html  Full page layout — nav, hero, about, experience, skills, contact, footer
        app.component.ts    Nav scroll, mobile menu, stats counter animation
        components/         ProjectsComponent (dynamic from API), ContactComponent
        services/           api.service.ts — all HTTP calls go through here
      styles.css            Global dark theme — all CSS variables and component styles
      assets/               Put resume PDF here (vinod-maneti-resume.pdf)

  portfolio-db/
    schema.sql              Creates portfolio_db, both tables, seeds projects
```

---

## Design & theme

StackHawk-inspired dark theme with 8 CSS tokens in `portfolio-site/src/styles.css`:

| Token | Value | Role |
|-------|-------|------|
| `--ink` | `#07070f` | Page background |
| `--surface` | `#0e0e1c` | Cards, alternate sections |
| `--edge` | `#1e1e34` | Borders |
| `--hi` | `#eeeef8` | Headings, body text |
| `--lo` | `#6868a0` | Secondary text |
| `--violet` | `#7c3aed` | Primary accent, gradients |
| `--teal` | `#00cfaa` | Labels, kickers |
| `--bronze` | `#b87848` | Project kickers, metric pills |

UI features:
- Hero name single line: "Vinod Kumar" in white, "Maneti" in animated violet→teal gradient
- LinkedIn + GitHub social pill buttons with SVG icons in the hero
- Stats counter bar — counts up on load (6+ years, 2 companies, 4 projects)
- Glassmorphism about cards with gradient border
- Vertical experience timeline with glowing dot markers
- Spinning gradient border on project cards on hover
- Contact form with validation, DB persistence, and email notification

---

## Local setup

> See [SETUP.md](SETUP.md) for a full walkthrough including issues hit and fixes.

### Prerequisites

| Tool | Version / Notes |
|------|----------------|
| Java | 17+ |
| Maven | 3.9.9 at `/usr/local/maven` — add to PATH if needed |
| MySQL | 8.x at `/usr/local/mysql` — official installer, not Homebrew |
| Node | 20.12.1 |
| Angular CLI | 17 (`sudo npm install -g @angular/cli@17`) |

### 1. Database

Copy the example properties file and set your password:
```bash
cp portfolio-backend/src/main/resources/application.properties.example \
   portfolio-backend/src/main/resources/application.properties
```

Load the schema:
```bash
/usr/local/mysql/bin/mysql -u root -p < portfolio-db/schema.sql
```

### 2. Backend

```bash
cd portfolio-backend
mvn spring-boot:run
```

API runs at `http://localhost:8080`. Verify:
```bash
curl http://localhost:8080/api/projects
```

### 3. Frontend

```bash
cd portfolio-site
ng serve
```

App runs at `http://localhost:4200`.

---

## API reference

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET  | `/api/projects` | All projects in display order |
| GET  | `/api/projects?category=backend` | Filter by category |
| POST | `/api/contact` | Submit a contact message |
| GET  | `/api/contact` | List messages (**protect before going public**) |

---

## Before deploying

- Update allowed origins in `WebConfig.java` with your Vercel URL
- Update `baseUrl` in `portfolio-site/src/app/services/api.service.ts` with deployed backend URL
- Protect `GET /api/contact` with authentication
- Resume PDF is at `portfolio-site/src/assets/Vinod_Resume.pdf` (already in place)
- Set `spring.mail.*` and `app.notification-email` in `application.properties` for email to work
