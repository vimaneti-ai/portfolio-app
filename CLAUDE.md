# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

### Local development
```
Browser → Angular (:4200) → Spring Boot (:8080) → MySQL (local)
```

### Production (AWS — live at https://www.vinodmaneti.com)
```
Browser
   |
   v
CloudFront (HTTPS)  E2EZ2L1KSZ1EQ8 — www.vinodmaneti.com / d3v7l3ap9v1bme.cloudfront.net
   |-- /* (default)  → S3 static website (Angular build, HTTP only from CF side)
   |-- /api/*        → EC2 t3.micro :8080 via EC2 DNS hostname
                          |
                          v
                     RDS MySQL db.t4g.micro (portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com)
```

EC2 Elastic IP: `3.150.38.140` (fixed — does not change on stop/start)

Custom domain:
- `https://www.vinodmaneti.com` is the primary public URL.
- `https://vinodmaneti.com` redirects to `https://www.vinodmaneti.com` through IONOS forwarding.
- ACM certificate `c40ade75-49ae-4a42-9354-d663e6048cde` is in `us-east-1` and covers both root and `www`.
- IONOS has `www` CNAME → `d3v7l3ap9v1bme.cloudfront.net`.

- **portfolio-site** — The live Angular 17 app at `portfolio-app/portfolio-site/` (inside this repo). Uses `NgModule`-based architecture with standalone components imported directly into `AppModule`. All page layout (nav, hero, about, experience, skills) lives in `app.component.html`; projects load dynamically via `<app-projects>` and the contact form via `<app-contact>`. Global styles (light theme, CSS variables, all component styles) are in `src/styles.css`. `api.service.ts` intentionally uses relative `baseUrl = '/api'` so the custom domain does not trigger CORS errors. **Always edit files here — not in `portfolio-frontend/`.**
- **portfolio-frontend** — Source components only (not a runnable app). These were copied into `portfolio-site` during setup. Do not edit here.
- **portfolio-backend** — Spring Boot 3 / Java 17 REST API. Standard layered structure: Controller → Service → Repository (Spring Data JPA). `@EnableAsync` is set on `PortfolioApplication`. CORS is configured in `WebConfig.java` — currently allows `http://localhost:4200`, the CloudFront domain, and both custom domain origins.
- **portfolio-db** — `schema.sql` creates `portfolio_db` and its three tables (`contact_messages`, `projects`, `visitor_events`), then seeds the project rows. Hibernate is set to `validate` mode, so the schema must exist before the backend starts.

The `Project.tags` field is a comma-separated string in both the database and the TypeScript model — `tagList()` in `projects.component.ts` splits it at render time.

## Design & theme

The site uses a clean light theme inspired by marco.fyi, with 8 CSS custom properties in `styles.css`:

```
--bg       #ffffff   page background
--surface  #f7f7f9   card / alternate section background
--edge     #e8e8ed   borders
--hi       #111118   headings and body text
--lo       #44445a   secondary / dimmed text
--accent   #5b21b6   primary accent (indigo)
--teal     #0891b2   secondary accent
--bronze   #c2410c   project kickers, metric pills
```

Key UI features implemented:
- **Centered floating pill nav** — fixed at top, frosted glass background, active link highlights as user scrolls (IntersectionObserver on each section with threshold 0.3). Links: About, Experience, Projects, Skills, Contact, Resume.
- Hero name "Vinod Kumar Maneti" on a single line (`white-space: nowrap`, `clamp(1.8rem, 4vw, 3.6rem)`) — "Vinod Kumar " in `--hi`, "Maneti" in `--accent`
- LinkedIn and GitHub social pill buttons (SVG icons) below the hero name
- Stats counter bar (6+ years, 2 companies, 4 projects — counts up on load with cubic ease)
- About cards with border + shadow on hover (no glassmorphism)
- Vertical timeline for experience — dot (16px, accent fill) precisely centered on the 2px line (`padding-left: 32px`, line at `left: 5px`, dot at `left: -34px`)
- Project cards lift on hover with accent border
- Contact form (`<app-contact>`) wired into the page — saves to MySQL and sends email
- Visitor analytics — `page_view` fired on app init and click events on Resume/LinkedIn/GitHub links, stored in `visitor_events` table

## Visitor analytics

`VisitorAnalyticsService.java` handles `POST /api/analytics/track`. On each event it:

1. Reads the real client IP from `X-Forwarded-For` (set by CloudFront) or falls back to `X-Real-IP` / `remoteAddr`
2. SHA-256 hashes the IP and stores a truncated version (`a.b.c.0` for IPv4)
3. Parses browser, OS, and device type from the User-Agent string
4. Calls `ip-api.com` for approximate GeoIP (country, region, city, timezone) — skipped for localhost and private IP ranges (127.x, 10.x, 192.168.x, 172.16–31.x)
5. Saves a `VisitorEvent` row

The frontend (`app.component.ts`) fires `page_view` on init and `link_click` events for Resume, LinkedIn, and GitHub interactions via `ApiService.trackEvent()`. See [PRIVACY.md](PRIVACY.md) for the public-facing privacy notice.

## Email (contact form notifications)

`ContactEmailService.java` uses `JavaMailSender` (Gmail SMTP) to send two emails on every contact form submission:
1. A notification to `vinodben594@gmail.com` with the sender's details
2. A confirmation reply to the person who submitted the form

Both sends are `@Async` — the API response is not delayed. Local credentials are
stored in the external, gitignored `portfolio-backend/config/application.properties`
(created from `application.properties.example`). No real `application.properties`
belongs under `src/main/resources`, because Maven would package it inside the JAR.
The Gmail password is a 16-character App Password — not the account password.

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
mvn clean package            # remove old output and build JAR
```

Create `portfolio-backend/config/application.properties` from
`src/main/resources/application.properties.example`, set its permissions to
`600`, and add the local credentials there. Run from `portfolio-backend/` so
Spring Boot automatically loads the external `config/application.properties`.
After packaging, `jar tf target/portfolio-1.0.0.jar | grep application.properties`
must list only `application.properties.example`.

### Frontend
```bash
cd portfolio-site
ng serve                     # dev server on :4200
npm run build                # production build
```

If the Angular CLI fails with `Cannot find module './bootstrap'`, local
`node_modules` is damaged. Repair with:
```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
rm -rf node_modules
npm ci
npm run build
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
curl -X POST http://localhost:8080/api/analytics/track \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test","eventType":"page_view","pageUrl":"/"}'
```

## GitHub

Repository: **https://github.com/vimaneti-ai/portfolio-app**

`portfolio-backend/config/application.properties` is gitignored and external to
the JAR. Credentials must never be placed under `src/main/resources` or committed.

## Production deployment (AWS)

See [DEPLOYMENT.md](DEPLOYMENT.md) for full step-by-step guide.

Key resources:
- **Primary URL:** `https://www.vinodmaneti.com`
- **Root redirect:** `https://vinodmaneti.com` → `https://www.vinodmaneti.com`
- **CloudFront:** `d3v7l3ap9v1bme.cloudfront.net` (distribution `E2EZ2L1KSZ1EQ8`)
- **ACM certificate:** `c40ade75-49ae-4a42-9354-d663e6048cde` in `us-east-1`
- **EC2 Elastic IP:** `3.150.38.140` (SSH key: `~/.ssh/portfolio-key.pem`)
- **RDS endpoint:** `portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com`
- **S3 bucket:** `vinod-portfolio-2026` (versioning enabled)

Redeployment — after any code change:
```bash
# Backend
mvn clean package -DskipTests
jar tf target/portfolio-1.0.0.jar | grep application.properties
scp -i ~/.ssh/portfolio-key.pem target/portfolio-1.0.0.jar ec2-user@3.150.38.140:~/
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140
  sudo systemctl restart portfolio   # systemd manages the process
  sudo systemctl status portfolio    # verify active (running)

# Frontend (api.service.ts uses relative /api)
npm run build
# Re-upload dist/portfolio-site/browser/ to S3
# Then create CloudFront invalidation: /*
```

If `scp`/`ssh` to EC2 times out, check the EC2 security group SSH rule. It may
still allow only an old home IP. Update port `22` source to **My IP**.

Spring Boot is managed by systemd (`/etc/systemd/system/portfolio.service`) — it
auto-starts on EC2 reboot and restarts automatically if the process crashes.

## Remaining TODOs

- **Resume PDF** — at `portfolio-site/src/assets/Vinod_Resume.pdf`; nav Resume link and hero social pill link here.

## Completed infrastructure

- **EC2 systemd service** — `portfolio.service` created, enabled, and running. Spring Boot auto-starts on reboot and auto-restarts on crash.
- **EC2 port 8080 restricted** — `0.0.0.0/0` inbound rule removed from security group; only CloudFront can reach the API.
- **SEO** — `index.html` has `<title>`, `<meta name="description">`, and Open Graph tags for LinkedIn/Slack previews.
- **npm audit** — safe fixes applied (`npm audit fix`); remaining 48 vulnerabilities are all dev-only webpack internals, not fixable without breaking Angular 17.
- **Stray files** — `apache-maven-3.9.9-bin.tar.gz` removed from git tracking; `*.tar.gz` added to `.gitignore`.
- **JUnit tests** — 45 unit tests across 5 service classes: `ContactService`, `ContactEmailService`, `ProjectService`, `VisitorAnalyticsService`, `AnalyticsDashboardService`. All passing.
- **Admin analytics dashboard** — `/admin` route shows login screen + visitor stats, daily bar chart, browser/OS/device/country breakdowns, and recent events table. Protected by HTTP Basic Auth (`GET /api/analytics/summary`).
- **GitHub Actions CI/CD** — `.github/workflows/deploy.yml` auto-deploys on every push to `main`: runs all unit tests, then deploys backend JAR to EC2 via SSH and frontend build to S3 + CloudFront in parallel.

## Security

`GET /api/contact` is protected with HTTP Basic Auth via Spring Security (`SecurityConfig.java`).
Credentials are set via `app.admin.username` and `app.admin.password` in the external
`portfolio-backend/config/application.properties` (never committed). Access it with:
```bash
curl -u admin:YOUR_PASSWORD https://www.vinodmaneti.com/api/contact
```
