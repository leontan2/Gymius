# Gymius

Gymius is a full-stack gym tracker with an Angular frontend and a Spring Boot API. Users sign in with Google OAuth 2.0, then track workouts, exercise logs, progress charts, personal records, and their Google-backed profile.

## Stack

- Frontend: Angular, TypeScript, Angular Router, Reactive Forms, Chart.js, @lucide/angular
- Backend: Spring Boot, Spring Security, OAuth2 Client, Spring Data JPA
- Database: PostgreSQL by default, H2 available with the `local` Spring profile
- Auth: Google OAuth 2.0 with only `openid`, `profile`, and `email` scopes

## Prerequisites

- Java 17 or newer, with `JAVA_HOME` pointing at the JDK
- Maven 3.9 or newer
- Node.js 20.11 or newer and npm
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
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend in a second terminal:

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200`.

## Run With H2

H2 is useful for quick local testing without PostgreSQL:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Google OAuth environment variables are still required for login. The H2 console is enabled at `http://localhost:8080/h2-console` when the `local` profile is active.

## API

Authenticated API endpoints are session-based and require the Google login cookie:

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
```

For production, point `DATABASE_URL` at PostgreSQL, set `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to your deployed frontend origin, and keep Google credentials in your host's secret manager.
