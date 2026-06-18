# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

```
Browser → Angular (portfolio-site) → Spring Boot (portfolio-backend) → MySQL (portfolio-db)
```

- **portfolio-site** — The live Angular 17 app at `portfolio-app/portfolio-site/` (inside this repo). Uses `NgModule`-based architecture with standalone components imported directly into `AppModule`. All page layout (nav, hero, about, experience, skills, contact, footer) lives in `app.component.html`; projects are loaded dynamically via `<app-projects>`. Global styles (dark theme, CSS variables, all component styles) are in `src/styles.css`. **Always edit files here — not in `portfolio-frontend/`.**
- **portfolio-frontend** — Source components only (not a runnable app). These were copied into `portfolio-site` during setup. Do not edit here.
- **portfolio-backend** — Spring Boot 3 / Java 17 REST API. Standard layered structure: Controller → Service → Repository (Spring Data JPA). CORS is configured in `WebConfig.java` and must include the deployed Vercel URL for production.
- **portfolio-db** — `schema.sql` creates `portfolio_db` and its two tables, then seeds the project rows. Hibernate is set to `validate` mode, so the schema must exist before the backend starts.

The `Project.tags` field is a comma-separated string in both the database and the TypeScript model — `tagList()` in `projects.component.ts` splits it at render time.

## Design & theme

The site uses a StackHawk-inspired dark theme with 8 CSS custom properties in `styles.css`:

```
--ink     #07070f   page background
--surface #0e0e1c   card / section background
--edge    #1e1e34   borders, grid lines
--hi      #eeeef8   headings and body text
--lo      #6868a0   secondary / dimmed text
--violet  #7c3aed   primary accent, gradients, glows
--teal    #00cfaa   labels, kickers, secondary accent
--bronze  #b87848   project kickers, metric pills
```

Key UI features implemented:
- Animated gradient hero name (violet→teal wave on loop)
- Stats counter bar (6+ years, 2 companies, 4 projects — counts up on load)
- Glassmorphism about cards with gradient border on hover
- Vertical timeline for experience with glowing violet dot markers
- Spinning gradient border on project cards (violet→teal→bronze) on hover

## Local setup

MySQL is installed at `/usr/local/mysql` (official installer, not Homebrew). Connect with:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD'
```

Maven is installed at `/usr/local/maven`. If `mvn` is not on PATH:
```bash
export PATH=/usr/local/maven/bin:$PATH
```

Angular CLI 17 is installed globally (`ng`). Node version is 20.12.1 — do not upgrade Angular CLI beyond v17 without upgrading Node first.

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

Set your MySQL password in `portfolio-backend/src/main/resources/application.properties` before starting. Use `application.properties.example` as a template.

### Frontend
```bash
cd portfolio-site
ng serve                     # dev server on :4200
ng build                     # production build
```

### Run everything (3 terminals)
```bash
# Terminal 1 — backend
export PATH=/usr/local/maven/bin:$PATH
cd portfolio-backend && mvn spring-boot:run

# Terminal 2 — frontend
cd portfolio-site && ng serve

# Terminal 3 — verify API
curl http://localhost:8080/api/projects
```

### Smoke-test the API
```bash
curl http://localhost:8080/api/projects
curl "http://localhost:8080/api/projects?category=backend"
```

## GitHub

Repository: **https://github.com/vimaneti-ai/portfolio-app**

`application.properties` is gitignored — credentials are never committed.

## Key deployment TODOs

- `WebConfig.java` — replace `https://your-portfolio.vercel.app` with the real Vercel URL
- `portfolio-site/src/app/services/api.service.ts` — replace `http://localhost:8080/api` with the deployed backend URL
- `GET /api/contact` is unprotected — add authentication before going public
- Add resume PDF to `portfolio-site/src/assets/vinod-maneti-resume.pdf` for the Résumé button to work
