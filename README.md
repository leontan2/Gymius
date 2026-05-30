# Gymius

Gymius is a full-stack gym tracker with an Angular frontend and a Spring Boot API. Users sign in with Google OAuth 2.0, then track workouts, exercise logs, progress charts, personal records, and their Google-backed profile.

## Stack

- Frontend: Angular, TypeScript, Angular Router, Reactive Forms, Chart.js, @lucide/angular
- Backend: Spring Boot, Spring Security, OAuth2 Client, Spring Data JPA
- Database: PostgreSQL by default, H2 available with the `local` Spring profile
- Auth: Google OAuth 2.0 with only `openid`, `profile`, and `email` scopes
- AI: OpenAI vision analysis for the Food Calorie Scanner, with a mock provider for local development

## Prerequisites

- Java 17 or newer, with `JAVA_HOME` pointing at the JDK
- Maven 3.9 or newer, or the included Maven wrapper in `backend/`
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
npm install
npm start
```

Open `http://localhost:4200`.

## Run With H2

H2 is useful for quick local testing without PostgreSQL:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Google OAuth environment variables are still required for login. The H2 console is enabled at `http://localhost:8080/h2-console` when the `local` profile is active.

## Deploy on Vercel + Render + Neon

The easiest free deployment split is:

- Frontend: Vercel static app from the `frontend` directory
- Backend: Render Web Service from the `backend` directory using the Docker runtime
- Database: Neon PostgreSQL

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
FRONTEND_URL=https://your-vercel-app.vercel.app
CORS_ALLOWED_ORIGINS=https://your-vercel-app.vercel.app
DATABASE_URL=jdbc:postgresql://your-neon-host.neon.tech/your-db?sslmode=require
DATABASE_USERNAME=your-neon-user
DATABASE_PASSWORD=your-neon-password
SESSION_COOKIE_SAME_SITE=none
SESSION_COOKIE_SECURE=true
SPRING_MAIN_LAZY_INITIALIZATION=true
SPRING_DATA_JPA_REPOSITORIES_BOOTSTRAP_MODE=lazy
MEAL_VISION_PROVIDER=openai
OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-5-mini
OPENAI_IMAGE_DETAIL=auto
```

Render provides `PORT` automatically. The backend reads it with a local fallback to `8080`.

Create the Vercel frontend from the `frontend` directory. Set this Vercel environment variable:

```text
FRONTEND_API_URL=https://your-render-backend.onrender.com
```

The Vercel build runs `npm run build`, which writes the production Angular API URL from `FRONTEND_API_URL`.

Update your Google OAuth Web Client after both apps have URLs:

```text
Authorized JavaScript origin:
https://your-vercel-app.vercel.app

Authorized redirect URI:
https://your-render-backend.onrender.com/login/oauth2/code/google
```

Keep only the `openid`, `profile`, and `email` scopes.

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
FRONTEND_API_URL
MEAL_VISION_PROVIDER
OPENAI_API_KEY
OPENAI_MODEL
OPENAI_IMAGE_DETAIL
MEAL_IMAGE_MAX_BYTES
MEAL_IMAGE_MAX_SIZE
DEFAULT_DAILY_CALORIES
```

For production, point `DATABASE_URL` at PostgreSQL, set `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to your deployed frontend origin, set secure cross-site cookies with `SESSION_COOKIE_SAME_SITE=none` and `SESSION_COOKIE_SECURE=true`, and keep Google/OpenAI credentials in your host's secret manager. Put `OPENAI_API_KEY` only on the backend host, never in Vercel or Angular.
