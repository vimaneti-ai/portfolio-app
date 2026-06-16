# Local Setup — What We Did & What We Hit

A full record of getting this app running locally for the first time (June 2026).

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

Edit `portfolio-backend/src/main/resources/application.properties`:
- Set `spring.datasource.password` to your MySQL root password
- Add `allowPublicKeyRetrieval=true` to the datasource URL

---

## Step 2 — Load the database

```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' < portfolio-db/schema.sql
```

Verify:
```bash
/usr/local/mysql/bin/mysql -u root -p'YOUR_PASSWORD' -e "USE portfolio_db; SHOW TABLES;"
```

Expected output: `contact_messages` and `projects`.

---

## Step 3 — Start the backend

```bash
cd portfolio-backend
mvn spring-boot:run
```

If `mvn` is not found: `export PATH=/usr/local/maven/bin:$PATH`

Verify: `curl http://localhost:8080/api/projects`

---

## Step 4 — Start the frontend

The live Angular app is at `/Users/vinod/Projects/portfolio-site` (separate from this repo).

```bash
cd /Users/vinod/Projects/portfolio-site
ng serve
```

Open `http://localhost:4200`.

---

## Issues & Fixes

### MySQL: Access Denied (ERROR 1045)
Root password was unknown. Homebrew was broken so status couldn't be checked via `brew services`.

**Fix:** MySQL is installed at `/usr/local/mysql`. Reset root password using safe mode:
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
Restart MySQL from System Settings → MySQL.

---

### Maven: command not found
Maven was not installed and Homebrew was broken.

**Fix:** Download from the Apache archive (dlcdn.apache.org returned 404):
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
`ProjectsComponent` and `ContactComponent` use `standalone: true` and cannot go in `declarations[]`.

**Fix:** Put them in `imports[]` in `AppModule`:
```typescript
imports: [BrowserModule, HttpClientModule, ProjectsComponent, ContactComponent]
```

---

## Frontend Setup (one-time)

The Angular app (`portfolio-site`) was created fresh and the components copied in:
```bash
ng new portfolio-site --no-standalone --routing false --style css
cp -r portfolio-app/portfolio-frontend/src/app/* portfolio-site/src/app/
```

The design was rebuilt to match `vinod-portfolio.html` — global styles in `styles.css`, full page layout in `app.component.html`, projects loaded dynamically from the API.
