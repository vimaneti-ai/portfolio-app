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

## Step 1 — Configure the backend

Copy the example file and fill in your MySQL password:
```bash
cp portfolio-backend/src/main/resources/application.properties.example \
   portfolio-backend/src/main/resources/application.properties
```

Edit the file and set:
- `spring.datasource.password` — your MySQL root password
- `allowPublicKeyRetrieval=true` is already in the example (required for this MySQL setup)

---

## Step 2 — Load the database

```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' < portfolio-db/schema.sql
```

Verify:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' -e "USE portfolio_db; SHOW TABLES;"
```

Expected: `contact_messages` and `projects` tables.

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
- Animated gradient name in hero
- Stats counter bar (counts up on page load)
- Glassmorphism about cards (frosted glass + gradient border)
- Vertical experience timeline with glowing dot markers
- Spinning gradient border on project cards on hover

---

## GitHub

Repository: **https://github.com/vimaneti-ai/portfolio-app**

`application.properties` is excluded via `.gitignore` — credentials are never pushed.
`application.properties.example` is committed as a setup template.

Push changes:
```bash
cd /Users/vinod/Projects/portfolio-app
git add .
git commit -m "your message"
git push
```

---

## Next Steps (before deploying)

- [ ] Test on mobile and cross-browser (Chrome, Safari, Firefox)
- [ ] Protect `GET /api/contact` with authentication
- [ ] Deploy backend to Railway or AWS Elastic Beanstalk + RDS
- [ ] Deploy frontend to Vercel
- [ ] Update `WebConfig.java` allowed origins with Vercel URL
- [ ] Update `baseUrl` in `api.service.ts` with deployed backend URL
- [ ] Add resume PDF to `portfolio-site/src/assets/vinod-maneti-resume.pdf`
- [ ] Record video demo for final course submission
