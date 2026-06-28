-- ============================================================
-- Portfolio Database Schema
-- Database: MySQL
-- Three features: contact messages + projects served from DB + visitor analytics
-- ============================================================

CREATE DATABASE IF NOT EXISTS portfolio_db;
USE portfolio_db;

-- ------------------------------------------------------------
-- Table 1: contact_messages
-- Stores submissions from the contact form on the website.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contact_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(100)  NOT NULL,
    last_name   VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Table 2: projects
-- Drives the Projects section. Angular fetches these via the API
-- instead of having them hardcoded in the HTML.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(150)  NOT NULL,
    kicker       VARCHAR(100),
    description  VARCHAR(1500) NOT NULL,
    metric       VARCHAR(200),
    category     VARCHAR(50)   NOT NULL,   -- used by the filter: frontend / backend / fullstack
    tags         VARCHAR(300),             -- comma-separated, e.g. "Angular,TypeScript,SQL"
    display_order INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Table 3: visitor_events
-- Stores privacy-conscious portfolio analytics.
-- The site records page/click events, browser/device data, referrer,
-- a hashed/truncated IP, and approximate GeoIP fields for public IPs.
-- It does not require browser GPS permission.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visitor_events (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id        VARCHAR(100),
    event_type        VARCHAR(50)   NOT NULL,
    event_name        VARCHAR(150),
    page_url          VARCHAR(500),
    referrer          VARCHAR(500),
    user_agent        VARCHAR(1000),
    browser           VARCHAR(100),
    operating_system  VARCHAR(100),
    device_type       VARCHAR(50),
    ip_hash           VARCHAR(128),
    ip_truncated      VARCHAR(100),
    country           VARCHAR(100),
    region            VARCHAR(100),
    city              VARCHAR(100),
    timezone          VARCHAR(100),
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Seed data: your real projects
-- ------------------------------------------------------------
INSERT INTO projects (title, kicker, description, metric, category, tags, display_order) VALUES
('New Subscription Bug Fix',
 'Production debugging - KFin',
 'Investors submitting the New Subscription form were silently redirected to an error page even when every field looked correct. I traced the request from the Angular form through the generated XML to the backend stored procedure and found the cause: a name longer than the model''s 50-character limit was breaking the call before any error message could surface. After fixing the field length and validating through dev, test, and staging, I reviewed every model class and corrected other length mismatches.',
 'Root cause found - fixed across all environments',
 'fullstack',
 'Angular,TypeScript,C#,SQL',
 1),

('KYC Validation Integration',
 'Compliance - KFin',
 'In a mutual fund purchase workflow, customers could move forward without meeting KYC requirements. I implemented a KYC validation check through an API integration so users had to satisfy compliance rules before proceeding, closing a gap that created downstream operational issues.',
 'Reduced compliance gaps in onboarding',
 'backend',
 'Java,REST APIs,Spring Boot',
 2),

('Application Performance Tuning',
 'Performance - KFin',
 'Worked on improving load behavior and responsiveness in enterprise pages by addressing how data was fetched and rendered, reducing unnecessary work on the client and making heavy screens feel faster for branch users.',
 'Faster load on data-heavy screens',
 'frontend',
 'Angular,TypeScript,SQL',
 3),

('Portfolio Website',
 'This site - 2026',
 'A full-stack portfolio built with Angular and Spring Boot, backed by MySQL and deployed on Vercel. The contact form and project list are both powered by REST APIs, making the site itself a working demonstration of the stack.',
 'Angular - Spring Boot - MySQL',
 'fullstack',
 'Angular,Spring Boot,MySQL,Vercel',
 4);
