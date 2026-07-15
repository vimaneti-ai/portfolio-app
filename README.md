# Portfolio Website — Full-Stack Build

A personal portfolio site built with **Angular 17 + Spring Boot + MySQL**, fully deployed on AWS with a custom domain.

**Live site: https://www.vinodmaneti.com**

GitHub: **https://github.com/vimaneti-ai/portfolio-app**

---

## What this app does

1. **Projects from a database** — the Projects section loads dynamically from `GET /api/projects`
2. **Contact form** — Angular reactive form → Spring Boot REST API → MySQL + async Gmail notification to site owner and confirmation reply to sender
3. **Visitor analytics** — page views and link clicks tracked to MySQL with browser, OS, device, and approximate GeoIP (hashed/truncated IP, no GPS). Admin dashboard at `/admin` shows live charts and a recent events table, secured with HTTP Basic Auth
4. **45 unit tests** — JUnit 5 + Mockito across all 5 service classes (`ContactService`, `ContactEmailService`, `ProjectService`, `VisitorAnalyticsService`, `AnalyticsDashboardService`)
5. **CI/CD** — GitHub Actions pipeline runs all tests on every push, then auto-deploys backend to EC2 and frontend to S3 + CloudFront in parallel
6. **SEO** — `<title>`, `<meta name="description">`, and Open Graph tags for LinkedIn/search previews

---

## Architecture

### Local development
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
  MySQL database    (contact_messages, projects, visitor_events)
```

### Production (AWS)
```
   Browser (HTTPS)
      |
      v
  CloudFront  www.vinodmaneti.com
      |-- /* (default)   S3 bucket (Angular static build)
      |-- /api/*         EC2 t3.micro  3.150.38.140 :8080
                              |
                              v
                         RDS MySQL db.t4g.micro
```

---

## Project structure

```
portfolio-app/
  portfolio-backend/        Spring Boot app (Java 17, Maven)
    config/
      application.properties          Local secrets (gitignored, never packaged)
    src/main/java/com/vinod/portfolio/
      controller/           REST endpoints (contact, projects, analytics, error handling)
      service/              business logic
      repository/           Spring Data JPA interfaces
      model/                JPA entities (ContactMessage, Project, VisitorEvent)
      config/               CORS configuration (WebConfig.java)
    src/main/resources/
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
        app.component.ts    Active section tracking (IntersectionObserver), stats counter animation
        components/         ProjectsComponent (dynamic from API), ContactComponent
        services/           api.service.ts — all HTTP calls go through here
      styles.css            Global dark theme — all CSS variables and component styles
      assets/               Resume PDF: Vinod_Resume.pdf (nav Resume link points here)

  portfolio-db/
    schema.sql              Creates portfolio_db, analytics/contact/project tables, seeds projects
```

---

## Design & theme

Clean light theme inspired by marco.fyi, with 8 CSS tokens in `portfolio-site/src/styles.css`:

| Token | Value | Role |
|-------|-------|------|
| `--bg` | `#ffffff` | Page background |
| `--surface` | `#f7f7f9` | Cards, alternate sections |
| `--edge` | `#e8e8ed` | Borders |
| `--hi` | `#111118` | Headings, body text |
| `--lo` | `#44445a` | Secondary text |
| `--accent` | `#5b21b6` | Primary accent (indigo) |
| `--teal` | `#0891b2` | Secondary accent |
| `--bronze` | `#c2410c` | Project kickers, metric pills |

UI features:
- Centered floating pill nav — frosted glass, active link tracks current section as you scroll
- Hero name "Vinod Kumar Maneti" on a single line in `--hi` / `--accent`
- LinkedIn + GitHub social pill buttons with SVG icons in the hero
- Stats counter bar — counts up on load (6+ years, 2 companies, 4 projects)
- About cards with border + shadow on hover
- Vertical experience timeline with dot markers precisely aligned to the line
- Project cards lift on hover with accent border
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

Create the external local configuration and restrict its permissions:
```bash
mkdir -p portfolio-backend/config
cp portfolio-backend/src/main/resources/application.properties.example \
   portfolio-backend/config/application.properties
chmod 600 portfolio-backend/config/application.properties
```

Fill in the MySQL and Gmail values in `portfolio-backend/config/application.properties`.
This file is gitignored and remains outside `src/main/resources`, so Maven does not
package the credentials inside the JAR. Spring Boot loads `config/application.properties`
automatically when started from `portfolio-backend/`.

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

In production, the Angular app uses a relative API base URL (`/api`) from
`portfolio-site/src/app/services/api.service.ts`. This lets the same build work
on the custom domain and the CloudFront domain without browser CORS errors.

---

## API reference

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET  | `/api/projects` | public | All projects in display order |
| GET  | `/api/projects?category=backend` | public | Filter by category |
| POST | `/api/contact` | public | Submit a contact message |
| GET  | `/api/contact` | Basic Auth | List stored messages (admin only) |
| POST | `/api/analytics/track` | public | Store a page view or click event |
| GET  | `/api/analytics/summary` | Basic Auth | Visitor analytics summary for admin dashboard |

Analytics records are intentionally lightweight. The frontend sends only session/event/page/referrer data. The backend adds request timestamp, user agent, browser, OS, device type, hashed/truncated IP, and approximate location from server-side IP lookup. No browser GPS permission popup is used. Localhost/private IPs intentionally leave location fields blank.

`GET /api/contact` is protected with Spring Security HTTP Basic Auth. Public visitors can still submit the contact form with `POST /api/contact`, but only an admin with credentials from the external `application.properties` can list stored messages.

See [PRIVACY.md](PRIVACY.md) for the portfolio analytics privacy note.

Production RDS has the `visitor_events` table. Long-term database access should
go through EC2 (`Mac → SSH to EC2 → RDS`) instead of direct Mac → RDS access,
because home/public IP allowlist rules change over time.

---

## Deployment

The app is live on AWS. See [DEPLOYMENT.md](DEPLOYMENT.md) for the full setup guide and all issues/fixes encountered.

**To redeploy after a code change:**

Deployment is fully automated via GitHub Actions (`.github/workflows/deploy.yml`). Every push to `main` triggers:
1. All 45 unit tests
2. Backend JAR build → SCP to EC2 → `systemctl restart portfolio`
3. Angular build → S3 sync → CloudFront invalidation `/*`

```bash
git push origin main   # triggers full deploy automatically
```

**Manual deploy (fallback only):**

Backend:
```bash
cd portfolio-backend
mvn clean package -DskipTests
jar tf target/portfolio-1.0.0.jar | grep application.properties
scp -i ~/.ssh/portfolio-key.pem target/portfolio-1.0.0.jar ec2-user@3.150.38.140:~/
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140
  sudo systemctl restart portfolio
```

Frontend:
```bash
cd portfolio-site && npm run build
aws s3 sync dist/portfolio-site/browser/ s3://vinod-portfolio-2026 --delete
aws cloudfront create-invalidation --distribution-id E2EZ2L1KSZ1EQ8 --paths "/*"
```

## Engineering decisions

### Rate limiting on the contact form

`POST /api/contact` is a public endpoint that writes to MySQL and fires a Gmail SMTP call on every submission. Without any guard, a bot could flood both the database and the inbox.

**Approach — token bucket via Bucket4j:**  
Each client IP gets a bucket of 5 tokens. Every submission consumes one token; the bucket refills to 5 every hour. Requests that arrive with an empty bucket receive `HTTP 429 Too Many Requests` immediately — no DB write, no email. Bucket4j implements the algorithm correctly (thread-safe, no hand-rolled locking needed).

**IP extraction:**  
The app sits behind CloudFront, which rewrites the source IP. The real client IP is in the `X-Forwarded-For` header set by CloudFront; the code reads that first and falls back to `remoteAddr` for local development where no proxy is present.

**Why not a Spring filter or Spring Security?**  
A `@Service` injected directly into the controller is simpler for a single endpoint and keeps the logic easy to test in isolation — four unit tests cover the allow/block/independence/fresh-IP cases without any Spring context needed.

---

## Known limitations

- npm audit has 48 unresolved frontend dependency warnings — all dev-only webpack internals, not fixable without upgrading past Angular 17
