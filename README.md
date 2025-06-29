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

The server will start on `http://localhost:8080`.

## Running the frontend

Install dependencies and start the development server:

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173` by default.

## Android project

The Android module can be opened with Android Studio. It currently contains
only a minimal activity as a starting point.
