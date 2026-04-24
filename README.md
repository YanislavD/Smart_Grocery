# Smart Grocery

Smart Grocery is a Spring Boot web application for managing household grocery and pantry workflows.

## What the application does now

- User registration and login
  - Web flow with Thymeleaf pages (`/register`, `/login`, `/dashboard`)
  - Session-based authentication for web UI
- API authentication endpoints
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Pantry domain basics
  - Pantry items, products, households data model
  - CRUD API for pantry items (`/api/pantry`)
- Security setup
  - Separate security chains for web and API
  - Form login for web, JWT filter for API routes
- Error handling
  - Global API error responses in JSON
  - Global web error handling with custom error pages
- UI foundation
  - Styled Thymeleaf templates and shared stylesheet
  - Basic dashboard with live counters (users/products/pantry items)

## What the application is planned to do

- Household-oriented user experience
  - Assign users to households
  - Show household-specific dashboard data
- Rich pantry features
  - Low-stock alerts
  - Expiration tracking and reminders
  - Better product catalog management
- Shopping workflows
  - Build shopping lists from pantry shortages
  - Track list completion by household members
- Better quality and operations
  - Integration tests for auth and critical flows
  - Production-ready logging and monitoring
  - CI pipeline for automated checks

## Architecture notes

- Layered structure:
  - Controller -> Service -> Repository
- Service boundary rule:
  - A service may call its own repository
  - A service may call another service
  - A service must not call another module's repository directly

## Tech stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Thymeleaf

## Local run (quick start)

1. Ensure PostgreSQL is running:
   - DB: `smart_pantry`
   - User: `postgres`
   - Password: `postgres`
2. Ensure Java 21 is installed and `JAVA_HOME` is configured.
3. Run:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

Then open:

- `http://localhost:8080/`

