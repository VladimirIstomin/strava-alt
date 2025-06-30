FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/strava-alt-backend/ /app/
CMD ["./bin/strava-alt-backend"]
