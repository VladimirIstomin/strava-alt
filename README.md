# Travalt

This repository contains a multi-module project that aims to provide an alternative interface to Strava.
The backend is written in Kotlin using Ktor, the frontend is a React + TypeScript app, and
there is a placeholder for a future Android application also written in Kotlin.

## Project structure

- `backend` - Ktor server exposing the API and handling Strava integration.
- `frontend` - React + TypeScript single page application.
- `android` - Placeholder for the future Android client.

## Running the backend

Use Gradle to run the server:

```bash
cd backend
./gradlew run
```

JDK 17 must be installed manually before running the backend. Gradle will not
download the JDK automatically, so ensure it is available on your `PATH`.

The backend requires Strava API credentials. Set the following environment variables before running:

```
STRAVA_CLIENT_ID=<your_client_id>
STRAVA_CLIENT_SECRET=<your_client_secret>
STRAVA_REDIRECT_URI=http://localhost:8080/api/v1/callback
FRONTEND_URL=http://localhost:5173
```

`FRONTEND_URL` controls where the backend redirects users after authentication.

The server will start on `http://localhost:8080`.
All API endpoints are available under the `/api/v1` path.

### Auto-reloading both projects

From the repository root you can launch both the backend and frontend in watch
mode:

```bash
npm install
npm run dev
```

This command starts the backend with `nodemon` and the frontend with Vite. Both
servers restart automatically whenever code changes.

## Running the frontend

Install dependencies and start the development server. The frontend expects the backend URL in the `BACKEND_URL` environment variable, which defaults to `http://localhost:8080`:

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173` by default.

Open the frontend in your browser and click "Login with Strava" to authorize and see your name in the top right corner.

## Android project

The Android module can be opened with Android Studio. It currently contains
only a minimal activity as a starting point.
