# Millennial Task Management System — Worker Service

A **Spring Boot 3** background worker service responsible for async email delivery, SSE push notifications, and queue-based job processing for the Millennial Task Management System.

---

## Tech Stack

| Layer         | Technology                           |
|---------------|--------------------------------------|
| Framework     | Spring Boot 3 (Web, Mail)            |
| Language      | Java 17                              |
| Queue         | Redis (list-based job queue)         |
| Email         | Spring Mail (SMTP / Gmail)           |
| Notifications | Server-Sent Events (SSE via emitters)|
| Build         | Maven (wrapper included)             |

---

## Responsibilities

- **Email Notifications** — Sends HTML emails when users are assigned to projects/tasks, or when a reply is posted on a work log.
- **SSE Push** — Manages long-lived `SseEmitter` connections per user; broadcasts notifications without polling.
- **Background Job Queue** — Polls a Redis list for async tasks submitted by the core API (email payloads, notification payloads).

---

## Setup Instructions

### Prerequisites
- Java 17+
- Redis running locally (default port 6379)
- SMTP credentials (Gmail or any SMTP server)

### Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd millennial-plance-worker
   ```

2. **Create environment file**
   ```bash
   cp .env.template .env
   ```
   Fill in the values:

   | Variable       | Description                          | Example                          |
   |----------------|--------------------------------------|----------------------------------|
   | `MYSQL_HOST`   | MySQL host (shared DB with backend)  | `localhost`                      |
   | `MYSQL_PORT`   | MySQL port                           | `3307`                           |
   | `MYSQL_DATABASE` | Database name                      | `millennial_db`                  |
   | `MYSQL_USER`   | Database user                        | `millennial`                     |
   | `MYSQL_PASSWORD` | Database password                  | `your_db_password`               |
   | `REDIS_HOST`   | Redis host                           | `localhost`                      |
   | `REDIS_PORT`   | Redis port                           | `6379`                           |
   | `REDIS_PASSWORD` | Redis password (leave empty if none) | *(empty)*                      |
   | `SMTP_HOST`    | SMTP server host                     | `smtp.gmail.com`                 |
   | `SMTP_PORT`    | SMTP port                            | `587`                            |
   | `SMTP_USER`    | SMTP username / email                | `your_email@gmail.com`           |
   | `SMTP_PASS`    | SMTP password or app password        | `your_app_password`              |
   | `SMTP_FROM`    | Sender address shown in emails       | `no-reply@millennial.com`        |

3. **Run the worker**
   ```bash
   ..\millennial-backend\mvnw.cmd spring-boot:run
   ```
   The worker listens on **http://localhost:8081** (SSE endpoint).

---

## Architecture Decisions

- **Redis Queue** — Core API pushes JSON payloads onto a Redis list (`LPUSH`); the worker polls with `BRPOP` to process them reliably.
- **SSE per User** — Each authenticated user opens a persistent `GET /sse/subscribe/{userId}` connection; the worker broadcasts events without HTTP polling.
- **Transactional Email** — HTML templates are rendered server-side; SMTP connection pool reuse is handled by Spring Mail's `JavaMailSender`.
- **Graceful Emitter Cleanup** — `SseManager` removes stale/timed-out emitters on `onCompletion` and `onTimeout` callbacks to prevent memory leaks.

---

## Assumptions

- The worker shares the same MySQL database as the backend (read-only for user/task lookups).
- Redis is used purely as a message broker; no persistence is required for the queue.
- Gmail SMTP requires an **App Password** when 2FA is enabled (not the account password).

---

## Available Commands

| Command                                   | Description               |
|-------------------------------------------|---------------------------|
| `..\millennial-backend\mvnw.cmd spring-boot:run` | Run in development mode |
| `./mvnw clean package`                    | Build a fat JAR           |
| `./mvnw test`                             | Run unit tests            |

---

## Docker (optional)

A `docker-compose.yml` is included for spinning up MySQL and Redis locally:

```bash
docker-compose up -d
```
