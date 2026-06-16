# Portfolio Website — Full-Stack Build

A personal portfolio site built with **Angular 17 + Spring Boot + MySQL**, deployed on **Vercel**.

Two features make this a real full-stack app instead of a static page:

1. **Projects from a database** — the Projects section loads from `GET /api/projects` instead of being hardcoded
2. **Contact form** — Angular reactive form → Spring Boot REST API → MySQL

The design matches `vinod-portfolio.html` — dark theme with animated hero, about, experience, projects, skills, and contact sections.

---

## Architecture

```
   Browser
      |
      v
  Angular 17 app  (portfolio-site — lives outside this repo)
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
portfolio-backend/        Spring Boot app (Java 17, Maven)
  src/main/java/com/vinod/portfolio/
    controller/           REST endpoints (contact, projects, error handling)
    service/              business logic
    repository/           Spring Data JPA interfaces
    model/                JPA entities (ContactMessage, Project)
    config/               CORS configuration
  src/main/resources/
    application.properties  MySQL connection settings
  pom.xml

portfolio-frontend/       Source components (not a runnable app)
  src/app/                Copy these into your Angular project to use them
    components/           ProjectsComponent + ContactComponent (standalone)
    services/             api.service.ts — single HTTP client for all API calls
    models/               TypeScript interfaces

portfolio-db/
  schema.sql              Creates portfolio_db, both tables, and seeds projects
```

> The live Angular app is at `/Users/vinod/Projects/portfolio-site` (Angular 17, NgModule-based).
> `portfolio-frontend` contains the source components that were copied into it.

---

## Local setup

> See [SETUP.md](SETUP.md) for a full walkthrough including issues hit and fixes.

### Prerequisites

- Java 17+
- Maven (`/usr/local/maven` on this machine — add to PATH if needed)
- MySQL (`/usr/local/mysql` on this machine — official installer, not Homebrew)
- Node 20+ and Angular CLI 17 (`npm install -g @angular/cli@17`)

### 1. Database

```bash
/usr/local/mysql/bin/mysql -u root -p < portfolio-db/schema.sql
```

Creates `portfolio_db` with two tables and seeds your projects.

### 2. Backend

Edit `application.properties` and set your MySQL password. Then:

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
cd /Users/vinod/Projects/portfolio-site
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
| GET  | `/api/contact` | List messages (protect this in production) |

---

## Before deploying

- Set your real MySQL password in `application.properties`
- Update allowed origins in `WebConfig.java` with your Vercel URL
- Update `baseUrl` in `api.service.ts` with your deployed backend URL
- Protect `GET /api/contact` with authentication
