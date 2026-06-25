# Portfolio Website — Full-Stack Build

A personal portfolio site built with **Angular 17 + Spring Boot + MySQL**, deployed on AWS.

**Live:** https://www.vinodmaneti.com  
Root redirect: https://vinodmaneti.com → https://www.vinodmaneti.com  
CloudFront fallback URL: https://d3v7l3ap9v1bme.cloudfront.net

GitHub: **https://github.com/vimaneti-ai/portfolio-app**

---

## What this app does

Three features make this a real full-stack app instead of a static page:

1. **Projects from a database** — the Projects section loads dynamically from `GET /api/projects`
2. **Contact form** — Angular reactive form → Spring Boot REST API → MySQL
3. **Email notifications** — every contact submission triggers a Gmail notification to the site owner and a confirmation reply to the sender (JavaMailSender + Gmail SMTP, async)

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
  MySQL database    (contact_messages, projects)
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
      controller/           REST endpoints (contact, projects, error handling)
      service/              business logic
      repository/           Spring Data JPA interfaces
      model/                JPA entities (ContactMessage, Project)
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

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET  | `/api/projects` | All projects in display order |
| GET  | `/api/projects?category=backend` | Filter by category |
| POST | `/api/contact` | Submit a contact message |
| GET  | `/api/contact` | List messages (**protect before going public**) |

---

## Deployment

The app is live on AWS. See [DEPLOYMENT.md](DEPLOYMENT.md) for the full setup guide and all issues/fixes encountered.

**To redeploy after a code change:**

Backend:
```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-backend
mvn clean package -DskipTests
jar tf target/portfolio-1.0.0.jar | grep application.properties
scp -i ~/.ssh/portfolio-key.pem target/portfolio-1.0.0.jar ec2-user@3.150.38.140:~/
# SSH in, pkill old process, nohup new jar
```

The JAR check should list only `application.properties.example`, never
`BOOT-INF/classes/application.properties`.

Frontend:
```bash
npm run build
# Upload dist/portfolio-site/browser/ to S3 bucket vinod-portfolio-2026
# Create CloudFront invalidation /* to bust the cache
```

If `npm run build` fails with `Cannot find module './bootstrap'`, reinstall the
frontend dependencies:
```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
rm -rf node_modules
npm ci
npm run build
```

If the AWS CLI is not installed locally, upload the contents of
`dist/portfolio-site/browser/` manually in the S3 console, then create a
CloudFront invalidation for `/*`.

## Remaining TODOs

- Protect `GET /api/contact` with authentication
- Set Spring Boot to auto-start on EC2 reboot (systemd service)
- Review npm audit findings and update frontend dependencies safely
