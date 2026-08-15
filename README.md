# Gymius

Gymius is a full-stack gym tracker with an Angular frontend and a Spring Boot API. Users sign in with Google OAuth 2.0, then track workouts, exercise logs, progress charts, personal records, and their Google-backed profile.

## Stack

- Frontend: Angular 22, TypeScript, Angular Router, Reactive Forms, Chart.js, @lucide/angular
- Backend: Spring Boot 3.5, Spring Security, OAuth2 Client, Spring Data JPA
- Database: PostgreSQL with Flyway migrations; H2 is available with the `local` Spring profile
- Auth: Google OAuth 2.0 with only `openid`, `profile`, and `email` scopes
- AI: OpenAI vision analysis for the Food Calorie Scanner, with a mock provider for local development

## Prerequisites

- Java 17 or newer, with `JAVA_HOME` pointing at the JDK
- Maven 3.9 or newer, or the included Maven wrapper in `backend/`
- Node.js 22.22.3+, 24.15+, or 26+ and npm
- Docker, if you want the provided PostgreSQL container

## Project Structure

```text
backend/
  src/main/java/com/gymius
    config/        OAuth, CORS, API errors
    controller/    REST endpoints
    domain/        UserAccount, Workout, ExerciseLog entities
    dto/           Request and response DTOs
    mapper/        Entity to DTO mapping
    repository/    Spring Data JPA repositories
    service/       Workout, user, and analytics logic
frontend/
  src/app
    core/          API, auth guard, theme, models
    features/      Dashboard, workouts, progress, records, profile, login
    layout/        Authenticated app shell
```

## Google OAuth Setup

1. Open Google Cloud Console and create or select a project.
2. Configure the OAuth consent screen.
3. Create an OAuth Client ID with application type `Web application`.
4. Add this authorized redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

5. Copy the client ID and client secret into environment variables.

Gymius only requests `openid`, `profile`, and `email`. Do not add Gmail API scopes unless you intentionally build an inbox-related feature.

## Run Locally With PostgreSQL

Start Postgres:

```bash
docker compose up -d postgres
```

Set backend environment variables. In PowerShell:

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:FRONTEND_URL="http://localhost:4200"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/gymius"
$env:DATABASE_USERNAME="gymius"
$env:DATABASE_PASSWORD="gymius"
$env:MEAL_VISION_PROVIDER="mock"
```

Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run`.

Run the frontend in a second terminal:

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`.

## Run With H2

H2 is useful for quick local testing without PostgreSQL:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.useTestClasspath=true
```

On Windows PowerShell, quote Maven properties containing dots and hyphens:

```powershell
cd backend
$env:JAVA_HOME="C:\\Program Files\\Java\\jdk-21"
.\\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.useTestClasspath=true"
```

The local profile binds to `127.0.0.1`, uses an in-memory database, and enables the development user by default. Set `DEV_AUTH_BYPASS_ENABLED=false` to test Google OAuth. The H2 console is available at `http://localhost:8080/h2-console` only while this profile is active.

## Deploy on Vercel + Render + Neon

The easiest free deployment split is:

- Frontend: Vercel static app from the `frontend` directory
- Backend: Render Web Service from the `backend` directory using the Docker runtime
- Database: Neon PostgreSQL

For reliable session authentication, put the frontend and API on the same registrable domain, such as
`app.example.com` and `api.example.com`, or proxy the API through the frontend origin. A bare
`your-app.vercel.app` + `your-api.onrender.com` pairing is cross-site; browsers that block third-party
cookies can reject its session cookie even when it is `Secure` and `SameSite=None`.

Create a Neon database first. Use Neon's PostgreSQL connection details for the Render backend. The Spring Boot app expects a JDBC URL, so it should look like this:

```text
jdbc:postgresql://your-neon-host.neon.tech/your-db?sslmode=require
```

Create the Render backend as a Web Service:

```text
Runtime: Docker
Root Directory: backend
Instance Type: Free
```

Set these Render environment variables:

```text
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
FRONTEND_URL=https://app.example.com
CORS_ALLOWED_ORIGINS=https://app.example.com
DATABASE_URL=jdbc:postgresql://your-neon-host.neon.tech/your-db?sslmode=require
DATABASE_USERNAME=your-neon-user
DATABASE_PASSWORD=your-neon-password
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SESSION_COOKIE_SAME_SITE=lax
SESSION_COOKIE_SECURE=true
MEAL_VISION_PROVIDER=openai
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-5-mini
OPENAI_IMAGE_DETAIL=auto
OPENAI_CONNECT_TIMEOUT=10s
OPENAI_READ_TIMEOUT=45s
OPENAI_MAX_CONCURRENT_ANALYSES=4
```

Render provides `PORT` automatically. The backend reads it with a local fallback to `8080`.
Set Render's health-check path to `/health`; it now verifies database connectivity before reporting ready.

Create the Vercel frontend from the `frontend` directory. Set this Vercel environment variable:

```text
FRONTEND_API_URL=https://api.example.com
```

The Vercel build runs `npm run build`, which writes the production Angular API URL from `FRONTEND_API_URL`.

Update your Google OAuth Web Client after both apps have URLs:

```text
Authorized JavaScript origin:
https://app.example.com

Authorized redirect URI:
https://api.example.com/login/oauth2/code/google
```

Keep only the `openid`, `profile`, and `email` scopes.

## API

Authenticated API endpoints are session-based and require the Google login cookie. The frontend first fetches `GET /api/csrf` and sends its token on every state-changing request.

- `GET /api/csrf`
- `GET /api/me`
- `GET /api/dashboard`
- `GET /api/workouts`
- `POST /api/workouts`
- `GET /api/workouts/{id}`
- `PUT /api/workouts/{id}`
- `DELETE /api/workouts/{id}`
- `POST /api/workouts/{workoutId}/exercises`
- `PUT /api/workouts/{workoutId}/exercises/{exerciseId}`
- `DELETE /api/workouts/{workoutId}/exercises/{exerciseId}`
- `GET /api/progress`
- `GET /api/personal-records`
- `GET /api/nutrition/today`
- `POST /api/nutrition/analyze-image`
- `POST /api/nutrition/entries`
- `PUT /api/nutrition/goals`
- `POST /api/logout`

## Configuration

Backend configuration lives in `backend/src/main/resources/application.yml`. The important environment variables are:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
FRONTEND_URL
CORS_ALLOWED_ORIGINS
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
SERVER_PORT
PORT
SESSION_COOKIE_SAME_SITE
SESSION_COOKIE_SECURE
SPRING_MAIN_LAZY_INITIALIZATION
SPRING_DATA_JPA_REPOSITORIES_BOOTSTRAP_MODE
SPRING_JPA_HIBERNATE_DDL_AUTO
SPRING_FLYWAY_BASELINE_ON_MIGRATE
FRONTEND_API_URL
MEAL_VISION_PROVIDER
OPENAI_API_KEY
OPENAI_MODEL
OPENAI_IMAGE_DETAIL
OPENAI_CONNECT_TIMEOUT
OPENAI_READ_TIMEOUT
OPENAI_MAX_CONCURRENT_ANALYSES
MEAL_IMAGE_MAX_BYTES
MEAL_IMAGE_MAX_SIZE
DEFAULT_DAILY_CALORIES
```

For production, point `DATABASE_URL` at PostgreSQL, set `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to your deployed frontend origin, and use `SESSION_COOKIE_SAME_SITE=lax` with `SESSION_COOKIE_SECURE=true` when the app and API share a site. Use `SameSite=None` only for an intentionally cross-site deployment, understanding that browser third-party-cookie policies can still block it. Keep Google/OpenAI credentials in your host's secret manager. Put `OPENAI_API_KEY` only on the backend host, never in Vercel or Angular.

## Database Migrations

Flyway applies versioned migrations before Hibernate validates the schema. Fresh databases need no special setup. For a database previously created by Hibernate, take a backup, verify it matches the current schema, set `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` for the first deployment only, then remove the variable (or set it back to `false`). This deliberately fails closed instead of silently changing an unknown production schema.

## Quality Checks

Run the same checks used by CI before shipping:

```bash
cd frontend
npm run lint
npm run typecheck
npm run test:ci
FRONTEND_API_URL=https://api.example.invalid npm run build

cd ../backend
./mvnw clean verify
```

The production frontend build intentionally fails when `FRONTEND_API_URL` is missing or unsafe.
