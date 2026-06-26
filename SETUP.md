# Local Setup — What We Did & What We Hit

A full record of getting this app running locally (June 2026).

---

## Environment

| Tool | Version / Location |
|------|-------------------|
| macOS | 26.5.1 (Homebrew broken on this version) |
| Java | 23.0.2 (OpenJDK) |
| Maven | 3.9.9 — installed at `/usr/local/maven` |
| MySQL | 8.x — installed at `/usr/local/mysql` (official installer) |
| Node | 20.12.1 |
| Angular CLI | 17 (latest compatible with Node 20) |

---

## Step 1 — Configure the backend safely

Real credentials live in an external file, not under `src/main/resources`. This
prevents Maven from embedding the MySQL and Gmail passwords in the JAR.

Create the local configuration:
```bash
mkdir -p portfolio-backend/config
cp portfolio-backend/src/main/resources/application.properties.example \
   portfolio-backend/config/application.properties
chmod 600 portfolio-backend/config/application.properties
```

Edit `portfolio-backend/config/application.properties` and set:
- `spring.datasource.password` — your MySQL root password
- `spring.mail.username` — your Gmail address
- `spring.mail.password` — your Gmail App Password (16 characters, not your account password)
- `app.notification-email` — email address that receives contact form submissions

Generate a Gmail App Password at: **myaccount.google.com → Security → App passwords**

The external file is excluded by `.gitignore`. Always run Spring Boot from the
`portfolio-backend/` directory so it automatically loads `config/application.properties`.

After building, verify that credentials were not packaged:
```bash
cd portfolio-backend
mvn clean package
jar tf target/portfolio-1.0.0.jar | grep application.properties
```

Expected: `BOOT-INF/classes/application.properties.example` only. The output
must not include `BOOT-INF/classes/application.properties`.

---

## Step 2 — Load the database

```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' < portfolio-db/schema.sql
```

Verify:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' -e "USE portfolio_db; SHOW TABLES;"
```

Expected: `contact_messages`, `projects`, and `visitor_events` tables.

---

## Step 3 — Start the backend

```bash
export PATH=/usr/local/maven/bin:$PATH
cd portfolio-backend
mvn spring-boot:run
```

Wait for `Started PortfolioApplication`. Verify:
```bash
curl http://localhost:8080/api/projects
```

Verify analytics insert:
```bash
curl -X POST http://localhost:8080/api/analytics/track \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"local-test","eventType":"page_view","eventName":"setup_test","pageUrl":"/","referrer":"manual"}'
```

Then check MySQL:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' \
  -e "USE portfolio_db; SELECT id, session_id, event_type, event_name, ip_truncated, created_at FROM visitor_events ORDER BY id DESC LIMIT 5;"
```

Localhost visits usually show `0:0:0:0:0:0:0:1` or `127.0.0.1` and do not produce real city/state/country values. Approximate location only works from a public IP after deployment and after a GeoIP lookup is added/enabled.

---

## Step 4 — Start the frontend

```bash
cd portfolio-site
ng serve
```

Open `http://localhost:4200`.

> **Note:** `portfolio-site/` is inside this repo at `portfolio-app/portfolio-site/`.
> The original `/Users/vinod/Projects/portfolio-site` directory has been deleted.
> Always edit and run from `portfolio-app/portfolio-site/`.

For production, `portfolio-site/src/app/services/api.service.ts` uses:
```typescript
private readonly baseUrl = '/api';
```

That relative path is intentional. It lets the same Angular build call:
- `http://localhost:4200/api/...` in local/proxy-style testing if configured
- `https://www.vinodmaneti.com/api/...` in production

Do not change it back to the old hardcoded CloudFront API URL unless you also
change the deployment/CORS design.

For local development, `api.service.ts` may special-case `localhost` to call
`http://localhost:8080/api` so `ng serve` on port 4200 reaches the Spring Boot
backend instead of returning `404` from Angular's dev server.

---

## Issues & Fixes

### MySQL: Access Denied (ERROR 1045)
Root password was unknown. Homebrew was broken so `brew services` couldn't check status.

**Fix:** Reset root password using safe mode:
```bash
sudo /usr/local/mysql/support-files/mysql.server stop
sudo /usr/local/mysql/bin/mysqld_safe --skip-grant-tables &
/usr/local/mysql/bin/mysql -u root
```
Then inside MySQL:
```sql
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'YOUR_PASSWORD';
EXIT;
```
Restart from System Settings → MySQL.

---

### Maven: command not found
Maven not installed, Homebrew broken.

**Fix:** `dlcdn.apache.org` returned 404 — use the Apache archive instead:
```bash
curl -O https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar -xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 /usr/local/maven
echo 'export PATH=/usr/local/maven/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
```

---

### npm: EACCES permission errors
npm cache had root-owned files from prior `sudo npm` runs.

**Fix:**
```bash
sudo chown -R 501:20 "/Users/vinod/.npm"
```

---

### Angular CLI: can't install latest (v22)
Angular 22 requires Node 22+. Machine has Node 20.12.1.

**Fix:** Install Angular 17 instead:
```bash
sudo npm install -g @angular/cli@17
```

---

### Angular build: `Cannot find module './bootstrap'`
`npm run build` failed with:
```text
Error: Cannot find module './bootstrap'
Require stack:
- /Users/vinod/Projects/portfolio-app/portfolio-site/node_modules/.bin/ng
```

**Cause:** `node_modules` was corrupted/incomplete. Running `npm install` alone
said "up to date" and did not repair it.

**Fix:**
```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
rm -rf node_modules
npm ci
npm run build
```

The build output is:
```text
portfolio-site/dist/portfolio-site/browser/
```

---

### Standalone components in NgModule error
`ProjectsComponent` and `ContactComponent` use `standalone: true` — cannot go in `declarations[]`.

**Fix:** Put them in `imports[]` in `AppModule`:
```typescript
imports: [BrowserModule, HttpClientModule, ProjectsComponent, ContactComponent]
```

---

### Projects section showing nothing
Cards had `class="proj-card reveal"` — the `reveal` class starts at `opacity: 0` and relies on an IntersectionObserver added in `ngAfterViewInit`. Since cards are rendered asynchronously after the API call, the observer never picks them up.

**Fix:** Removed `reveal` from dynamically rendered project cards in `projects.component.html`.

---

### portfolio-site added as git submodule
Copying `portfolio-site` into `portfolio-app` while it still had its own `.git` folder caused git to treat it as a submodule — only a reference was committed, not the files.

**Fix:**
```bash
git rm --cached portfolio-site
rm -rf portfolio-site/.git
git add portfolio-site
git commit -m "Add portfolio-site source files"
git push
```

---

## Frontend Setup (one-time)

The Angular app was created fresh and components copied in:
```bash
ng new portfolio-site --no-standalone --routing false --style css
cp -r portfolio-frontend/src/app/* portfolio-site/src/app/
```

Then `app.module.ts`, `app.component.html`, `app.component.ts`, and `styles.css` were fully rebuilt to match the portfolio design.

---

## Design iterations

| Theme | Description |
|-------|-------------|
| Original | Dark — violet/teal/black (StackHawk-inspired) |
| Apple theme | Light — white/gray canvas, near-black text |
| **Final** | **Dark — StackHawk with 8 CSS tokens (`--ink`, `--surface`, `--edge`, `--hi`, `--lo`, `--violet`, `--teal`, `--bronze`)** |

UI features added progressively:
- Hero name single line: "Vinod Kumar" white, "Maneti" animated violet→teal gradient
- LinkedIn + GitHub social pill buttons (SVG icons) below hero name
- Stats counter bar (counts up on page load)
- Glassmorphism about cards (frosted glass + gradient border)
- Vertical experience timeline with glowing dot markers
- Spinning gradient border on project cards on hover
- Contact form (`<app-contact>`) replaces static mailto link — saves to DB + sends email

---

## GitHub

Repository: **https://github.com/vimaneti-ai/portfolio-app**

`portfolio-backend/config/application.properties` is excluded via `.gitignore`,
has user-only permissions (`chmod 600`), and is not packaged into the JAR.
`application.properties.example` is committed as a setup template.

Push changes:
```bash
cd /Users/vinod/Projects/portfolio-app
git add .
git commit -m "your message"
git push
```

## Custom domain status

Current live URLs:

```text
https://www.vinodmaneti.com
https://vinodmaneti.com  → redirects to https://www.vinodmaneti.com
```

The old CloudFront URL still works as a fallback:

```text
https://d3v7l3ap9v1bme.cloudfront.net
```

Important production notes:
- ACM certificate was issued in `us-east-1` for both `vinodmaneti.com` and
  `www.vinodmaneti.com`.
- IONOS DNS has `www` as a CNAME to CloudFront.
- IONOS root forwarding redirects `vinodmaneti.com` to
  `https://www.vinodmaneti.com`.
- IONOS included SSL Starter Wildcard was activated so the root HTTPS redirect
  works without `ERR_SSL_PROTOCOL_ERROR`.
- Spring Boot CORS now allows localhost, the CloudFront URL, and both custom
  domain origins.
- Contact form was verified after the backend redeploy and returned:
  `Thanks for reaching out. I'll get back to you soon.`

---

## Next Steps

- [x] Deploy backend to AWS EC2 + RDS (done June 2026)
- [x] Deploy frontend to AWS S3 static website (done June 2026)
- [x] Set up Elastic IP so EC2 IP never changes (3.150.38.140)
- [x] Set up HTTPS via CloudFront (https://d3v7l3ap9v1bme.cloudfront.net)
- [x] Register and connect custom domain (`www.vinodmaneti.com`)
- [x] Configure root redirect (`vinodmaneti.com` → `www.vinodmaneti.com`)
- [x] Update `WebConfig.java` allowed origins with CloudFront + custom domain URLs
- [x] Update `baseUrl` in `api.service.ts` to relative `/api`
- [x] Add resume PDF — `portfolio-site/src/assets/Vinod_Resume.pdf`
- [x] Enable S3 versioning for rollback
- [ ] Protect `GET /api/contact` with authentication
- [ ] Set Spring Boot to auto-start on EC2 reboot (systemd service)
- [ ] Restrict public EC2 `8080` exposure
- [ ] Review npm audit findings and update frontend dependencies safely
- [ ] Test on mobile and cross-browser (Chrome, Safari, Firefox)
- [ ] Record video demo for final course submission (deadline August 2, 2026)

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full AWS deployment guide.
