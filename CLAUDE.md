# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

```
Browser → Angular (portfolio-site) → Spring Boot (portfolio-backend) → MySQL (portfolio-db)
```

- **portfolio-site** — The live Angular 17 app at `/Users/vinod/Projects/portfolio-site`. This is the actual running frontend. It uses `NgModule`-based architecture with standalone components imported directly into `AppModule`. All page layout (nav, hero, about, experience, skills, contact, footer) lives in `app.component.html`; projects are loaded dynamically via `<app-projects>`. Global styles (dark theme, CSS variables, all component styles) are in `src/styles.css`.
- **portfolio-frontend** — Source components only (not a runnable app). These were copied into `portfolio-site` during setup. If you edit components, edit them in `portfolio-site/src/app/`.
- **portfolio-backend** — Spring Boot 3 / Java 17 REST API. Standard layered structure: Controller → Service → Repository (Spring Data JPA). CORS is configured in `WebConfig.java` and must include the deployed Vercel URL for production.
- **portfolio-db** — `schema.sql` creates `portfolio_db` and its two tables, then seeds the project rows. Hibernate is set to `validate` mode, so the schema must exist before the backend starts.

The `Project.tags` field is a comma-separated string in both the database and the TypeScript model — `tagList()` in `projects.component.ts` splits it at render time.

## Local setup

MySQL is installed at `/usr/local/mysql` (official installer, not Homebrew). Connect with:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD'
```

Maven is installed at `/usr/local/maven`. If `mvn` is not on PATH:
```bash
export PATH=/usr/local/maven/bin:$PATH
```

## Commands

### Database
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' < portfolio-db/schema.sql
```

### Backend
```bash
cd portfolio-backend
mvn spring-boot:run          # dev server on :8080
mvn test                     # run all tests
mvn test -Dtest=ClassName    # run a single test class
mvn package                  # build JAR
```

Set your MySQL password in `portfolio-backend/src/main/resources/application.properties` before starting.

### Frontend
```bash
cd /Users/vinod/Projects/portfolio-site
ng serve                     # dev server on :4200
ng build                     # production build
```

### Smoke-test the API
```bash
curl http://localhost:8080/api/projects
curl "http://localhost:8080/api/projects?category=backend"
```

## Key deployment TODOs

- `WebConfig.java` — replace `https://your-portfolio.vercel.app` with the real Vercel URL
- `portfolio-site/src/app/services/api.service.ts` — replace `http://localhost:8080/api` with the deployed backend URL
- `GET /api/contact` is unprotected and returns all contact form submissions — add auth before going public
