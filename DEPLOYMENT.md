# AWS Deployment Guide

Full record of deploying the portfolio app to AWS (June 2026).

**Live URL:** http://vinod-portfolio-2026.s3-website.us-east-2.amazonaws.com

---

## Architecture

```
Browser
   |
   v
S3 Static Website (Angular build)
   |  REST API calls
   v
EC2 t3.micro — Spring Boot :8080  (3.138.107.152)
   |
   v
RDS MySQL db.t4g.micro — portfolio_db
```

---

## AWS Account

- Provider: **AWS Educate** — login at awseducate.com with `vinodkumarmaneti@my.unt.edu`
- Region: **us-east-2 (Ohio)** — all resources must be in the same region
- Credits: $100 USD, valid 185 days from June 2026
- No credit card required (Educate account)

---

## Resources Created

| Resource | Name / ID | Details |
|---|---|---|
| EC2 instance | `portfolio-backend` | t3.micro, Amazon Linux 2023 |
| EC2 public IP | `3.138.107.152` | Changes on stop/start — see note below |
| EC2 security group | `portfolio-ec2-sg` | Port 22 (My IP), Port 8080 (Anywhere) |
| RDS instance | `portfolio-db` | db.t4g.micro, MySQL 8.4 |
| RDS endpoint | `portfolio-db.cduecko8i86c.us-east-2.rds.amazonaws.com` | Port 3306 |
| RDS security group | `portfolio-rds-sg` | Port 3306 from EC2 + My IP |
| S3 bucket | `vinod-portfolio-2026` | Static website hosting enabled |
| Key pair | `portfolio-key` | Stored at `~/.ssh/portfolio-key.pem` |

> **Important:** EC2 public IP changes every time the instance is stopped and restarted.
> If the IP changes, update `api.service.ts`, rebuild Angular, and re-upload to S3.

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

Expected: `contact_messages`, `projects` tables and 4 project rows.

---

## Step 5 — Deploy Spring Boot to EC2

### 5a — Build the JAR (on your Mac)

```bash
export PATH=/usr/local/maven/bin:$PATH
cd /Users/vinod/Projects/portfolio-app/portfolio-backend
mvn package -DskipTests
```

### 5b — Create production application.properties (on your Mac)

```bash
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
  ec2-user@3.138.107.152:~/

scp -i ~/.ssh/portfolio-key.pem \
  /tmp/application.properties \
  ec2-user@3.138.107.152:~/
```

### 5d — Install Java and start the app (on EC2)

```bash
ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.138.107.152

sudo dnf install java-17-amazon-corretto -y

nohup java -jar portfolio-1.0.0.jar \
  --spring.config.location=application.properties \
  > app.log 2>&1 &

sleep 15 && curl http://localhost:8080/api/projects
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

### 6a — Update API URL (on your Mac)

In `portfolio-site/src/app/services/api.service.ts`:
```typescript
private readonly baseUrl = 'http://3.138.107.152:8080/api';
```

### 6b — Update CORS in WebConfig.java

In `portfolio-backend/src/main/java/com/vinod/portfolio/config/WebConfig.java`:
```java
.allowedOrigins(
    "http://localhost:4200",
    "http://vinod-portfolio-2026.s3-website.us-east-2.amazonaws.com"
)
```

Rebuild and redeploy the JAR after this change (repeat Step 5a, 5c, 5d restart).

### 6c — Build Angular

```bash
cd /Users/vinod/Projects/portfolio-app/portfolio-site
ng build --configuration production
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
4. Save — note the **Website endpoint URL**

### 6f — Upload Angular build files

1. Go to **Objects** tab → **Upload**
2. Upload ALL contents of `dist/portfolio-site/browser/` including the `assets/` folder
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
**Fix:** Always SSH into EC2 first (`ssh -i ~/.ssh/portfolio-key.pem ec2-user@3.138.107.152`), confirm the prompt shows `[ec2-user@ip-...]` before running EC2 commands.

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

## Redeployment Checklist

When you make code changes and need to redeploy:

**Backend changes:**
```bash
# Mac
mvn package -DskipTests
scp -i ~/.ssh/portfolio-key.pem target/portfolio-1.0.0.jar ec2-user@3.138.107.152:~/

# EC2
pkill -f portfolio-1.0.0.jar
sleep 3
nohup java -jar portfolio-1.0.0.jar --spring.config.location=application.properties > app.log 2>&1 &
```

**Frontend changes:**
```bash
# Mac
ng build --configuration production
# Then re-upload dist/portfolio-site/browser/ contents to S3
```

---

## Next Steps

- [ ] Set up Elastic IP on EC2 so the public IP doesn't change on restart
- [ ] Add HTTPS using AWS Certificate Manager + CloudFront in front of S3
- [ ] Set Spring Boot to auto-start on EC2 reboot (systemd service)
- [ ] Protect `GET /api/contact` with authentication
- [ ] Record video demo for final course submission

---

## Future Improvements

### codebase-memory-mcp
A high-performance MCP server that indexes the codebase into a persistent knowledge graph for faster AI-assisted development.

- **Repo:** https://github.com/DeusData/codebase-memory-mcp
- **Why:** Reduces token usage by ~99% and enables sub-millisecond code lookups — useful when the project grows larger
- **When to implement:** When the codebase grows significantly (multiple microservices, larger team)
- **Installation (when ready):**
  ```bash
  pip install codebase-memory-mcp
  codebase-memory-mcp install   # auto-detects Claude Code and registers itself
  ```
- **Note:** Not needed at current project size (~20 files) — revisit if codebase expands beyond 100+ files
