# BFH Qualifier 1 — JAVA (Spring Boot)

This repo auto-runs on startup:

1. POSTs to **/hiring/generateWebhook/JAVA** with your name/regNo/email
2. Receives `webhook` and `accessToken` (JWT)
3. Submits your **final SQL query** to the webhook with `Authorization` header

## Quick start (no controllers needed)

- Edit `src/main/resources/application.properties` and set:
  - `bfh.name` — your name
  - `bfh.regNo` — your registration number (last two digits choose Q1 vs Q2)
  - `bfh.email` — your email
  - `bfh.finalQuery` — paste your final SQL query

### Build & run

```bash
mvn clean package
java -jar target/bfh-challenge-0.0.1-SNAPSHOT.jar
```

The app will print the submission status and response.

### Notes
- Choose the right SQL question (odd last-two-digits -> Q1, even -> Q2).
- Headers used:
  - `Content-Type: application/json`
  - `Authorization: <accessToken>` (value returned by generateWebhook)
- No web endpoints/controllers are used. The flow is triggered on startup via `CommandLineRunner`.
