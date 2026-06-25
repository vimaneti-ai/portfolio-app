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

- **portfolio-site** — The live Angular 17 app at `portfolio-app/portfolio-site/` (inside this repo). Uses `NgModule`-based architecture with standalone components imported directly into `AppModule`. All page layout (nav, hero, about, experience, skills) lives in `app.component.html`; projects load dynamically via `<app-projects>` and the contact form via `<app-contact>`. Global styles (dark theme, CSS variables, all component styles) are in `src/styles.css`. `api.service.ts` intentionally uses relative `baseUrl = '/api'` so the custom domain does not trigger CORS errors. **Always edit files here — not in `portfolio-frontend/`.**
- **portfolio-frontend** — Source components only (not a runnable app). These were copied into `portfolio-site` during setup. Do not edit here.
- **portfolio-backend** — Spring Boot 3 / Java 17 REST API. Standard layered structure: Controller → Service → Repository (Spring Data JPA). `@EnableAsync` is set on `PortfolioApplication`. CORS is configured in `WebConfig.java` — currently allows `http://localhost:4200`, the CloudFront domain, and both custom domain origins.
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
- Hero name single line: "Vinod Kumar" in white (`--hi`), "Maneti" in animated violet→teal gradient
- LinkedIn and GitHub social pill buttons (SVG icons) below the hero name
- Stats counter bar (6+ years, 2 companies, 4 projects — counts up on load)
- Glassmorphism about cards with gradient border on hover
- Vertical timeline for experience with glowing violet dot markers
- Spinning gradient border on project cards (violet→teal→bronze) on hover
- Contact form (`<app-contact>`) wired into the page — saves to MySQL and sends email

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
  pkill -f portfolio-1.0.0.jar && sleep 3
  nohup java -jar portfolio-1.0.0.jar --spring.config.location=application.properties > app.log 2>&1 &

# Frontend (api.service.ts uses relative /api)
npm run build
# Re-upload dist/portfolio-site/browser/ to S3
# Then create CloudFront invalidation: /*
```

If `scp`/`ssh` to EC2 times out, check the EC2 security group SSH rule. It may
still allow only an old home IP. Update port `22` source to **My IP**.

## Remaining TODOs

- `GET /api/contact` is unprotected — add authentication before going public
- Set Spring Boot to auto-start on EC2 reboot (systemd service) — currently must restart manually after EC2 stop/start
- Restrict public EC2 `8080` exposure; random scanner traffic has already shown up in Tomcat logs
- Review npm audit findings and update frontend dependencies safely
- Resume PDF is at `portfolio-site/src/assets/Vinod_Resume.pdf` — nav Résumé button links here
