# StravaAlt

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

The backend requires Strava API credentials. Set the following environment variables before running:

```
STRAVA_CLIENT_ID=<your_client_id>
STRAVA_CLIENT_SECRET=<your_client_secret>
STRAVA_REDIRECT_URI=http://localhost:8080/callback
```

The server will start on `http://localhost:8080`.

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

Install dependencies and start the development server:

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
