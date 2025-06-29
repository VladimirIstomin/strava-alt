package com.stravaalt

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.sessions.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val clientId = System.getenv("STRAVA_CLIENT_ID") ?: error("STRAVA_CLIENT_ID not set")
    val clientSecret = System.getenv("STRAVA_CLIENT_SECRET") ?: error("STRAVA_CLIENT_SECRET not set")
    val redirectUri = System.getenv("STRAVA_REDIRECT_URI") ?: "http://localhost:8080/callback"

    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }

    install(Sessions) {
        cookie<UserSession>("SESSION")
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    routing {
        get("/") {
            call.respondText("StravaAlt backend running")
        }

        get("/login") {
            val url = URLBuilder("https://www.strava.com/oauth/authorize").apply {
                parameters.append("client_id", clientId)
                parameters.append("redirect_uri", redirectUri)
                parameters.append("response_type", "code")
                parameters.append("scope", "read")
                parameters.append("approval_prompt", "auto")
            }.buildString()
            call.respondRedirect(url)
        }

        get("/callback") {
            val code = call.request.queryParameters["code"]
            if (code == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing code")
                return@get
            }

            val token: TokenResponse = httpClient.post("https://www.strava.com/oauth/token") {
                parameter("client_id", clientId)
                parameter("client_secret", clientSecret)
                parameter("code", code)
                parameter("grant_type", "authorization_code")
            }.body()

            call.sessions.set(UserSession(token.access_token))
            call.respondRedirect("http://localhost:5173")
        }

        get("/api/me") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            val athlete: Athlete = httpClient.get("https://www.strava.com/api/v3/athlete") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }.body()

            call.respond(UserInfo("${athlete.firstname} ${athlete.lastname}"))
        }
    }
}

@Serializable
data class UserSession(val accessToken: String)

@Serializable
data class TokenResponse(val access_token: String, val athlete: Athlete)

@Serializable
data class Athlete(val firstname: String, val lastname: String)

@Serializable
data class UserInfo(val name: String)
