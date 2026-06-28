# AWS Deployment Guide

Full record of deploying the portfolio app to AWS (June 2026).

**Live URL:** https://www.vinodmaneti.com  
Root redirect: https://vinodmaneti.com → https://www.vinodmaneti.com  
CloudFront fallback URL: https://d3v7l3ap9v1bme.cloudfront.net

---

## Architecture

```
Browser (HTTPS)
   |
   v
CloudFront  www.vinodmaneti.com / d3v7l3ap9v1bme.cloudfront.net  (distribution E2EZ2L1KSZ1EQ8)
   |-- /* (default)   →  S3 bucket: vinod-portfolio-2026  (Angular static build)
   |-- /api/*         →  EC2 t3.micro ec2-3-150-38-140.us-east-2.compute.amazonaws.com :8080
                              |
                              v
                         RDS MySQL db.t4g.micro
```

CloudFront sits in front of everything. The browser only ever talks HTTPS to CloudFront. CloudFront fetches S3 over HTTP (S3 website endpoints don't support HTTPS) and proxies `/api/*` to EC2.

---

## AWS Account

- Provider: **AWS Educate** — login at awseducate.com with `vinodkumarmaneti@my.unt.edu`
- Region: **us-east-2 (Ohio)** for EC2/RDS/S3. Exception: the ACM certificate
  used by CloudFront must be created in **us-east-1 (N. Virginia)**.
- Credits: $100 USD, valid 185 days from June 2026
- No credit card required (Educate account)

---

## Resources Created

| Resource | Name / ID | Details |
|---|---|---|
| EC2 instance | `portfolio-backend` | t3.micro, Amazon Linux 2023 |
| EC2 Elastic IP | `3.150.38.140` | Fixed — does not change on stop/start |
| EC2 DNS | `ec2-3-150-38-140.us-east-2.compute.amazonaws.com` | Used as CloudFront origin (IPs not allowed) |
| EC2 security group | `portfolio-ec2-sg` | Port 22 (My IP), Port 8080 (Anywhere) |
| RDS instance | `portfolio-db` | db.t4g.micro, MySQL 8.4 |
| RDS endpoint | `portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com` | Port 3306 |
| RDS security group | `portfolio-rds-sg` | Port 3306 from EC2 sg + My IP |
| S3 bucket | `vinod-portfolio-2026` | Static website hosting, versioning enabled |
| S3 website URL | `vinod-portfolio-2026.s3-website.us-east-2.amazonaws.com` | HTTP only — access via CloudFront instead |
| CloudFront distribution | `E2EZ2L1KSZ1EQ8` | `d3v7l3ap9v1bme.cloudfront.net` |
| Custom domain | `vinodmaneti.com` | Registered at IONOS; root redirects to `www` |
| ACM certificate | `c40ade75-49ae-4a42-9354-d663e6048cde` | Issued in `us-east-1`; covers `vinodmaneti.com` and `www.vinodmaneti.com` |
| Key pair | `portfolio-key` | `~/.ssh/portfolio-key.pem` |

---

## Step 1 — Create RDS MySQL Database

1. AWS Console → **RDS** → **Create database**
2. Settings:
   - Creation method: **Standard create**
   - Engine: **MySQL**
   - Template: **Free tier** (db.t4g.micro)
   - DB identifier: `portfolio-db`
   - Master username: `admin`
   - Master password: (your password)
   - Public access: **Yes**
   - VPC security group: create new → `portfolio-rds-sg`
   - Initial database name: `portfolio_db`
3. Click **Create database** — takes ~5 minutes

---

## Step 2 — Launch EC2 Instance

1. AWS Console → **EC2** → **Launch instance**
2. Settings:
   - Name: `portfolio-backend`
   - AMI: **Amazon Linux 2023** (free tier eligible)
   - Instance type: **t3.micro** (free tier in us-east-2)
   - Key pair: create new → `portfolio-key` → `.pem` format → download it
   - Auto-assign public IP: **Enable**
   - Security group: create new → `portfolio-ec2-sg`
     - SSH — port 22 — My IP
     - Custom TCP — port 8080 — Anywhere (0.0.0.0/0)
3. Click **Launch instance**

---

## Step 3 — Connect EC2 to RDS (Security Groups)

1. Go to **RDS** → `portfolio-db` → click `portfolio-rds-sg`
2. **Edit inbound rules** → add two rules:
   - MySQL/Aurora — 3306 — Source: `portfolio-ec2-sg` (select by security group)
   - MySQL/Aurora — 3306 — Source: My IP (to load schema from local Mac)
3. Save rules

---

## Step 4 — Load Database Schema

From your local Mac:

```bash
/usr/local/mysql/bin/mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  < /Users/vinod/Projects/portfolio-app/portfolio-db/schema.sql
```

Verify:

```bash
/usr/local/mysql/bin/mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  -e "USE portfolio_db; SHOW TABLES; SELECT COUNT(*) FROM projects;"
```

Expected: `contact_messages`, `projects`, `visitor_events` tables and 4 project rows.

If the database already exists and only the analytics table is missing, run the
schema change from EC2. This is the stable path because RDS allows the EC2
security group permanently, while a home/public IP changes over time.

```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140

mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  -e "USE portfolio_db; CREATE TABLE IF NOT EXISTS visitor_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, session_id VARCHAR(100), event_type VARCHAR(50) NOT NULL, event_name VARCHAR(150), page_url VARCHAR(500), referrer VARCHAR(500), user_agent VARCHAR(1000), browser VARCHAR(100), operating_system VARCHAR(100), device_type VARCHAR(50), ip_hash VARCHAR(128), ip_truncated VARCHAR(100), country VARCHAR(100), region VARCHAR(100), city VARCHAR(100), timezone VARCHAR(100), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);"
```

If `mysql` is not installed on EC2:

```bash
sudo dnf install mariadb105 -y
```

---

## Step 5 — Deploy Spring Boot to EC2

### 5a — Build the JAR (on your Mac)

```bash
export PATH=/usr/local/maven/bin:$PATH
cd /Users/vinod/Projects/portfolio-app/portfolio-backend
mvn clean package -DskipTests
jar tf target/portfolio-1.0.0.jar | grep application.properties
```

Expected: only `BOOT-INF/classes/application.properties.example`. If
`BOOT-INF/classes/application.properties` appears, stop: a local secret-bearing
configuration file is still under `src/main/resources`.

### 5b — Create production application.properties (on your Mac)

```bash
umask 077
cat > /tmp/application.properties << 'EOF'
spring.application.name=portfolio
spring.datasource.url=jdbc:mysql://portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com:3306/portfolio_db?useSSL=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=YOUR_RDS_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
server.port=8080

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=vinodben594@gmail.com
spring.mail.password=YOUR_GMAIL_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.notification-email=vinodben594@gmail.com
EOF
```

### 5c — Upload to EC2

```bash
scp -i ~/.ssh/portfolio-key.pem \
  /Users/vinod/Projects/portfolio-app/portfolio-backend/target/portfolio-1.0.0.jar \
  ec2-user@3.150.38.140:~/

scp -i ~/.ssh/portfolio-key.pem \
  /tmp/application.properties \
  ec2-user@3.150.38.140:~/

rm /tmp/application.properties
```

The production configuration remains external to the JAR. On EC2, protect it
before starting the application:
```bash
chmod 600 ~/application.properties
```

### 5d — Install Java and start the app (on EC2)

```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140

sudo dnf install java-17-amazon-corretto -y

nohup java -jar portfolio-1.0.0.jar \
  --spring.config.location=application.properties \
  > app.log 2>&1 &

sleep 15 && curl http://localhost:8080/api/projects
```

Test analytics from EC2:

```bash
curl -X POST http://localhost:8080/api/analytics/track \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"ec2-test","eventType":"page_view","eventName":"backend_deploy_test","pageUrl":"/","referrer":"manual"}'
```

### Restart the app after a JAR update

```bash
pkill -f portfolio-1.0.0.jar
sleep 3
nohup java -jar portfolio-1.0.0.jar \
  --spring.config.location=application.properties \
  > app.log 2>&1 &
```

### Check logs

```bash
tail -f app.log
```

---

## Step 6 — Deploy Angular to S3

### 6a — Update CORS in WebConfig.java

In `portfolio-backend/src/main/java/com/vinod/portfolio/config/WebConfig.java`:
```java
.allowedOrigins(
    "http://localhost:4200",
    "https://d3v7l3ap9v1bme.cloudfront.net",
    "https://www.vinodmaneti.com",
    "https://vinodmaneti.com"
)
```

Rebuild and redeploy the JAR after this change (repeat Steps 5a, 5c, 5d restart).

### 6b — Update API URL in api.service.ts

In `portfolio-site/src/app/services/api.service.ts`, production traffic must resolve to:
```typescript
private readonly baseUrl = '/api';
```

Using `/api` makes the browser call the same domain currently serving the
Angular app, for example `https://www.vinodmaneti.com/api/projects`. This avoids
cross-origin/CORS problems when using the custom domain.

Local development can special-case `localhost` to call
`http://localhost:8080/api`, but the deployed build must still use `/api` for
the live domain.

### 6c — Build Angular

```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
npm run build
```

Output goes to: `dist/portfolio-site/browser/`

### 6d — Create S3 bucket

1. AWS Console → **S3** → **Create bucket**
2. Name: `vinod-portfolio-2026`, Region: `us-east-2`
3. Uncheck **Block all public access** → confirm
4. Create bucket

### 6e — Enable static website hosting

1. Click the bucket → **Properties** tab
2. Scroll to **Static website hosting** → **Edit**
3. Enable it, Index document: `index.html`, Error document: `index.html`
4. Save

### 6f — Enable versioning (for rollback)

1. **Properties** tab → **Bucket Versioning** → **Edit** → **Enable**
2. This lets you restore a previous Angular build if a new deploy breaks the site

### 6g — Upload Angular build files

1. Go to **Objects** tab → **Upload**
2. Upload ALL contents of `dist/portfolio-site/browser/` **including the `assets/` folder**
3. Set **bucket policy** under **Permissions** tab:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::vinod-portfolio-2026/*"
    }
  ]
}
```

---

## Step 7 — Set Up Elastic IP (Fix for Changing EC2 IP)

Without Elastic IP, EC2's public IP changes every time the instance is stopped and restarted.

1. AWS Console → **EC2** → **Elastic IPs** → **Allocate Elastic IP address**
2. Click **Allocate**
3. Select the new IP → **Actions** → **Associate Elastic IP address**
4. Resource type: **Instance** → select `portfolio-backend` → **Associate**

Result: EC2 is now permanently at `3.150.38.140`. SSH and SCP commands always use this IP.

---

## Step 8 — Set Up HTTPS with CloudFront

CloudFront gives HTTPS to the whole site. The browser calls CloudFront; CloudFront calls S3 over HTTP (the S3 website endpoint only supports HTTP internally) and proxies `/api/*` to EC2.

### 8a — Create the CloudFront distribution

1. AWS Console → **CloudFront** → **Create distribution**
2. **Origin domain:** paste the S3 website endpoint URL (NOT the bucket ARN):
   ```
   vinod-portfolio-2026.s3-website.us-east-2.amazonaws.com
   ```
3. **Protocol:** HTTP only (S3 website endpoints don't support HTTPS)
4. **Viewer protocol policy:** Redirect HTTP to HTTPS
5. **Cache policy:** CachingOptimized (default)
6. WAF: **No** (skip, paid feature)
7. Click **Create distribution** — takes ~5 minutes to deploy

### 8b — Add the /api/* behavior (proxy to EC2)

1. Click your distribution → **Behaviors** tab → **Create behavior**
2. **Path pattern:** `/api/*`
3. **Origin:** Create a new origin first:
   - Go to **Origins** tab → **Create origin**
   - **Origin domain:** `ec2-3-150-38-140.us-east-2.compute.amazonaws.com`
     (must be the DNS hostname — CloudFront does NOT accept raw IP addresses)
   - **Protocol:** HTTP only (EC2 runs plain HTTP on port 8080)
   - **HTTP port:** 8080
   - Save
4. Back in Behaviors → **Create behavior**:
   - Path pattern: `/api/*`
   - Origin: select the EC2 origin
   - Cache policy: **CachingDisabled** (API responses must never be cached)
   - **Allowed HTTP methods:** `GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE`
     (POST is needed for the contact form and analytics tracking)
   - **Cache HTTP methods:** keep default (`GET, HEAD`; OPTIONS may remain unchecked)
   - Save

### 8c — Test the distribution

```bash
curl https://d3v7l3ap9v1bme.cloudfront.net/api/projects
```

Should return the 4 projects JSON.

Test analytics through CloudFront:

```bash
curl -X POST https://d3v7l3ap9v1bme.cloudfront.net/api/analytics/track \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"cloudfront-test","eventType":"page_view","eventName":"cloudfront_test","pageUrl":"/","referrer":"manual"}'
```

Verify in RDS:

```bash
/usr/local/mysql/bin/mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  -e "USE portfolio_db; SELECT id, session_id, event_type, event_name, page_url, browser, operating_system, device_type, ip_truncated, country, region, city, created_at FROM visitor_events ORDER BY id DESC LIMIT 10;"
```

---

## Step 9 — Custom Domain with IONOS + ACM + CloudFront

Custom domain completed June 24/25, 2026.

### 9a — Request SSL certificate in ACM

CloudFront only sees ACM certificates created in **US East (N. Virginia) /
`us-east-1`**.

1. AWS Console → switch region to `us-east-1`
2. Certificate Manager → Request certificate → Public certificate
3. Add both names:
   ```text
   vinodmaneti.com
   www.vinodmaneti.com
   ```
4. Choose DNS validation.
5. Certificate issued:
   ```text
   arn:aws:acm:us-east-1:767398018879:certificate/c40ade75-49ae-4a42-9354-d663e6048cde
   ```

### 9b — Add ACM validation records in IONOS

IONOS DNS records used:

```text
Type: CNAME
Host name: _dfb2c5bf8d605a79c6cbb06a672108cd
Value: _c21f27b854f5511bd8ca0bd9589215f4.jkddzztszm.acm-validations.aws
Service: -
```

```text
Type: CNAME
Host name: _a64e269b9c994ff1543cd0c57cc19b72.www
Value: _b8fb2c1e21567aede34c78dbff77bc26.jkddzztszm.acm-validations.aws
Service: -
```

Important: the second record must use the second value. It was initially copied
with the first value by mistake, so `vinodmaneti.com` validated but
`www.vinodmaneti.com` stayed pending. Confirm with:

```bash
dig CNAME _a64e269b9c994ff1543cd0c57cc19b72.www.vinodmaneti.com
```

Expected answer:
```text
_b8fb2c1e21567aede34c78dbff77bc26.jkddzztszm.acm-validations.aws.
```

### 9c — Attach domain names to CloudFront

CloudFront → distribution `E2EZ2L1KSZ1EQ8` → add alternate domain names:

```text
vinodmaneti.com
www.vinodmaneti.com
```

Select the ACM certificate above. Wait for CloudFront deployment to complete.

### 9d — Point IONOS DNS to CloudFront

Add this IONOS DNS record:

```text
Type: CNAME
Host name: www
Value: d3v7l3ap9v1bme.cloudfront.net
Service: -
```

IONOS does not allow a root/apex CNAME for `@`, so the root domain is configured
as an HTTP redirect:

```text
vinodmaneti.com → https://www.vinodmaneti.com
Redirect type: HTTP redirect
```

Do not enable "Also set up for www subdomain"; `www` is already handled by the
CloudFront CNAME.

### 9e — IONOS SSL for root redirect

`https://vinodmaneti.com` initially showed:

```text
ERR_SSL_PROTOCOL_ERROR
```

Cause: IONOS forwarding existed, but IONOS SSL had not been activated for the
root redirect service.

Fix: activate the included `SSL Starter Wildcard` for `vinodmaneti.com`. After
installation, `https://vinodmaneti.com` redirects securely to
`https://www.vinodmaneti.com`.

---

## Step 10 — Backup & Rollback Setup

Three layers of rollback:

**S3 versioning** (frontend rollback):
- Already enabled in Step 6f
- To roll back: S3 → bucket → show versions → select previous version files → restore

**EC2 backup JAR** (backend rollback):
```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140
cp portfolio-1.0.0.jar portfolio-1.0.0.jar.bak
```
To roll back: `cp portfolio-1.0.0.jar.bak portfolio-1.0.0.jar` then restart.

**RDS automated snapshots** (database rollback):
- RDS → `portfolio-db` → **Maintenance & backups** → automated daily snapshots
- Retention: 7 days (free tier default)
- To restore: Actions → Restore to point in time

---

## Errors Encountered & Fixes

### Key pair not found during SCP
```
Warning: Identity file /Users/vinod/.ssh/portfolio-key.pem not accessible
```
**Cause:** Key was downloaded to `~/Downloads/` not `~/.ssh/`.
**Fix:**
```bash
mv ~/Downloads/portfolio-key.pem ~/.ssh/portfolio-key.pem
chmod 400 ~/.ssh/portfolio-key.pem
```

---

### pkill/nohup ran on Mac instead of EC2
**Cause:** Commands were pasted into the local Mac terminal instead of the SSH session.
**Fix:** Always SSH into EC2 first (`ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140`). Confirm the prompt shows `[ec2-user@ip-...]` before running EC2 commands.

---

### RDS security group rule conflict
```
You may not specify a referenced group id for an existing IPv4 CIDR rule.
```
**Cause:** Tried to add a security group reference to a slot that already had a CIDR rule.
**Fix:** Delete the conflicting rule first, then add two fresh rules — one with security group source, one with My IP.

---

### Resume button opened the site instead of PDF
**Cause:** Only the root files were uploaded to S3 — the `assets/` folder containing `Vinod_Resume.pdf` was not uploaded.
**Fix:** Re-upload via S3 → Upload → **Add folder** → select the `assets` folder from `dist/portfolio-site/browser/assets`.

---

### CloudFront 504 Gateway Timeout
```
504 ERROR: The request could not be satisfied.
```
**Cause:** CloudFront default behavior was trying HTTPS to connect to the S3 website origin (which only supports HTTP).
**Fix:** Edit origin → set **Protocol** to **HTTP only**.

---

### CloudFront origin domain cannot be an IP address
**Cause:** CloudFront does not accept raw IP addresses (`3.150.38.140`) as an origin domain.
**Fix:** Use the EC2 public DNS hostname instead:
```
ec2-3-150-38-140.us-east-2.compute.amazonaws.com
```

---

### Mixed content error (projects not loading after CloudFront)
```
Mixed Content: The page at 'https://d3v7l3ap9v1bme.cloudfront.net' was loaded over HTTPS,
but requested an insecure resource 'http://3.138.107.152:8080/api/projects'.
```
**Cause:** The Angular build sitting in S3 was an old build compiled with `baseUrl = 'http://3.138.107.152:8080/api'`. Even after updating `api.service.ts` and rebuilding, the old build was cached in CloudFront.
**Fix (two parts):**
1. Update `api.service.ts` to point to the CloudFront URL, rebuild Angular, re-upload to S3.
2. Create a **CloudFront invalidation** on path `/*` to flush the cache:
   - CloudFront → distribution → **Invalidations** tab → **Create invalidation**
   - Object paths: `/*`
   - Wait ~60 seconds

---

### "Connection is not secure" in browser (before CloudFront)
**Cause:** The S3 website URL (`http://`) serves HTTP — no SSL/TLS.
**Fix:** Set up CloudFront (Steps 8a–8c above). After the custom domain setup,
access the site through `https://www.vinodmaneti.com`; the CloudFront URL remains
as a fallback.

---

### Custom domain CORS error after switching to `www.vinodmaneti.com`
```
Access to XMLHttpRequest at 'https://d3v7l3ap9v1bme.cloudfront.net/api/projects'
from origin 'https://www.vinodmaneti.com' has been blocked by CORS policy
```
**Cause:** Angular was still compiled with the hardcoded CloudFront API URL.
The frontend page was on `www.vinodmaneti.com`, but API calls went to the old
CloudFront domain.

**Fix:** Change `portfolio-site/src/app/services/api.service.ts`:
```typescript
private readonly baseUrl = '/api';
```
Then rebuild, upload the build output to S3, and create CloudFront invalidation
for `/*`.

---

### Angular build failed with `Cannot find module './bootstrap'`
```
Error: Cannot find module './bootstrap'
Require stack:
- .../portfolio-site/node_modules/.bin/ng
```
**Cause:** Local `node_modules` was damaged/incomplete. `npm install` alone
reported "up to date" and did not repair it.

**Fix:**
```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
rm -rf node_modules
npm ci
npm run build
```

---

### AWS CLI missing locally
```
zsh: command not found: aws
```
**Cause:** AWS CLI was not installed on the Mac.

**Fix used:** manually upload the contents of
`portfolio-site/dist/portfolio-site/browser/` to the S3 bucket
`vinod-portfolio-2026`, then create a CloudFront invalidation for `/*` in the
AWS console.

---

### Contact form returned `403 Forbidden`
```
POST https://www.vinodmaneti.com/api/contact 403 Forbidden
```
**Causes checked/fixed:**
1. CloudFront `/api/*` behavior must allow all HTTP methods, including `POST`.
2. Spring Boot CORS originally allowed only localhost and the CloudFront domain.

**Fix:** update `WebConfig.java` to include:
```java
"https://www.vinodmaneti.com",
"https://vinodmaneti.com"
```
Then rebuild and redeploy the backend JAR to EC2.

Verification result from the site:
```text
Thanks for reaching out. I'll get back to you soon.
```

---

### Protected contact-message admin endpoint

`GET /api/contact` is protected with Spring Security HTTP Basic Auth.

Public endpoint remains open:
```bash
curl https://www.vinodmaneti.com/api/projects
```

Contact form submissions remain open:
```bash
curl -X POST https://www.vinodmaneti.com/api/contact \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","message":"Hello"}'
```

Reading all contact messages requires admin credentials:
```bash
curl -u admin:YOUR_ADMIN_PASSWORD https://www.vinodmaneti.com/api/contact
```

Without credentials:
```bash
curl https://www.vinodmaneti.com/api/contact
# Expected: 401 Unauthorized
```

Important CloudFront note: if Basic Auth works directly against EC2 but fails
through `www.vinodmaneti.com`, confirm the `/api/*` behavior forwards the
`Authorization` header to the EC2 origin.

The admin credentials live only in external config:
```text
portfolio-backend/config/application.properties
EC2: ~/application.properties
```

Do not commit real admin credentials.

---

### SCP/SSH timed out to EC2
```
ssh: connect to host 3.150.38.140 port 22: Operation timed out
```
**Cause:** EC2 security group allowed SSH only from an old home IP:
`70.120.128.206/32`.

**Fix:** EC2 → `portfolio-ec2-sg` → inbound rules → update SSH port `22`
source to **My IP**, then retry `scp`/`ssh`.

---

### RDS MySQL timed out from local Mac
```
ERROR 2003 (HY000): Can't connect to MySQL server on
'portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com:3306' (60)
```

**Cause:** RDS security group had an old home/public IP rule:
```text
70.120.128.206/32
```
The current Mac public IP had changed, so direct MySQL access from the Mac timed
out.

**Permanent fix:** do not depend on direct Mac → RDS access. Keep the RDS inbound
rule that allows the EC2 security group:
```text
portfolio-rds-sg inbound MySQL/Aurora 3306 from portfolio-ec2-sg
```
Then run database commands from EC2:
```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140

mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  -e "USE portfolio_db; SHOW TABLES;"
```

This was verified after creating the analytics table:
```text
contact_messages
projects
visitor_events
```

---

### RDS recommendations: Enhanced Monitoring and Multi-AZ

AWS showed two informational recommendations:
```text
portfolio-db doesn't have Enhanced Monitoring enabled
portfolio-db is not a Multi-AZ instance
```

**Decision:** ignore/dismiss for now. For a personal portfolio, basic CloudWatch
metrics and a single-AZ free-tier style RDS setup are acceptable. Multi-AZ and
Enhanced Monitoring improve reliability/observability but can add cost and are
not required for this deployment.

---

### Local Angular called port 4200 API and returned 404
```
POST http://localhost:4200/api/analytics/track 404 (Not Found)
POST http://localhost:4200/api/contact 404 (Not Found)
```

**Cause:** during `ng serve`, relative `/api` points at Angular's dev server on
port 4200, not Spring Boot on port 8080.

**Fix:** `portfolio-site/src/app/services/api.service.ts` now uses localhost
detection:
```typescript
private readonly baseUrl =
  ['localhost', '127.0.0.1'].includes(window.location.hostname)
    ? 'http://localhost:8080/api'
    : '/api';
```

Production still uses `/api`, which CloudFront routes to EC2.

---

### Local dev servers did not stop with Control+C

**Cause:** Control+C only stops the process attached to the focused terminal.
If the server was launched in another tab/background process, it may keep
running.

**Fix:** find and stop the process by port:
```bash
lsof -iTCP:8080 -sTCP:LISTEN -n -P
lsof -iTCP:4200 -sTCP:LISTEN -n -P

kill <BACKEND_PID>
kill <FRONTEND_PID>
```

Force only if needed:
```bash
kill -9 <PID>
```

---

### Tomcat `Invalid character found in method name`
```
Invalid character found in method name [MGLNDD_3.150.38.140_8080...]
```
**Cause:** random internet scanner/bot traffic hit the public EC2 `8080` port
with non-HTTP data.

**Fix:** no app fix required for that log entry. Longer-term security
improvement: restrict EC2 `8080` so only CloudFront can reach it, or place the
backend behind a load balancer/API Gateway.

---

## Redeployment Checklist

When you make code changes and need to redeploy:

**Database/schema changes:**
```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140

mysql \
  -h portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com \
  -u admin -p \
  -e "USE portfolio_db; CREATE TABLE IF NOT EXISTS visitor_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, session_id VARCHAR(100), event_type VARCHAR(50) NOT NULL, event_name VARCHAR(150), page_url VARCHAR(500), referrer VARCHAR(500), user_agent VARCHAR(1000), browser VARCHAR(100), operating_system VARCHAR(100), device_type VARCHAR(50), ip_hash VARCHAR(128), ip_truncated VARCHAR(100), country VARCHAR(100), region VARCHAR(100), city VARCHAR(100), timezone VARCHAR(100), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);"
```

Use the full `portfolio-db/schema.sql` file only for a fresh database. It also
contains project seed inserts, so rerunning the whole file on an existing RDS
database can duplicate project rows.

**Backend changes:**
```bash
# Mac — build and upload
cd /Users/vinod/Projects/portfolio-app/portfolio-backend
export PATH=/usr/local/maven/bin:$PATH
mvn clean package -DskipTests
jar tf target/portfolio-1.0.0.jar | grep application.properties
scp -i ~/.ssh/portfolio-key.pem target/portfolio-1.0.0.jar ec2-user@3.150.38.140:~/

# EC2 — restart
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.150.38.140
pkill -f portfolio-1.0.0.jar
sleep 3
nohup java -jar portfolio-1.0.0.jar --spring.config.location=application.properties > app.log 2>&1 &
tail -80 app.log
```

**Frontend changes:**
```bash
# Mac — build and upload
cd /Users/vinod/Projects/portfolio-app/portfolio-site
npm run build
# Then re-upload all contents of dist/portfolio-site/browser/ to S3 bucket

# Invalidate CloudFront cache (REQUIRED — otherwise visitors see the old build)
# CloudFront → distribution E2EZ2L1KSZ1EQ8 → Invalidations → Create → /*
```

---

## Next Steps

- [x] Set up Elastic IP (3.150.38.140) — EC2 IP no longer changes
- [x] Add HTTPS via CloudFront (d3v7l3ap9v1bme.cloudfront.net)
- [x] Register and connect custom domain (`www.vinodmaneti.com`)
- [x] Configure root redirect (`vinodmaneti.com` → `www.vinodmaneti.com`)
- [x] Fix custom-domain API/CORS issue with relative `/api` and backend CORS
- [x] Enable S3 versioning for frontend rollback
- [x] Create EC2 backup JAR for backend rollback
- [x] Add `visitor_events` analytics table to production RDS
- [x] Add visitor analytics tracking endpoint and frontend page/click tracking
- [ ] Set Spring Boot to auto-start on EC2 reboot (systemd service)
- [x] Protect `GET /api/contact` with Spring Security HTTP Basic Auth
- [ ] Restrict public EC2 `8080` exposure
- [ ] Set up GitHub Actions CI/CD for automated deploys (concerns: SSH port 22 from GH runners, IAM credentials, application.properties handling)
- [ ] Record video demo for final course submission (deadline August 2, 2026)

---

## Future Improvements

### codebase-memory-mcp
A high-performance MCP server that indexes the codebase into a persistent knowledge graph for faster AI-assisted development.

- **Repo:** https://github.com/DeusData/codebase-memory-mcp
- **Why:** Reduces token usage by ~99% and enables sub-millisecond code lookups — useful when the project grows larger
- **Requirements:** Python, pip, Claude Code MCP configured
- **When to implement:** When the codebase grows significantly (multiple microservices, larger team)
- **Installation (when ready):**
  ```bash
  pip install codebase-memory-mcp
  codebase-memory-mcp install   # auto-detects Claude Code and registers itself
  ```
- **Note:** Not needed at current project size (~20 files) — revisit if codebase expands beyond 100+ files
