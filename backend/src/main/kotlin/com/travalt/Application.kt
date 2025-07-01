package com.travalt


import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set
import io.ktor.server.sessions.clear
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.http.HttpMethod
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val clientId = System.getenv("STRAVA_CLIENT_ID") ?: error("STRAVA_CLIENT_ID not set")
    val clientSecret = System.getenv("STRAVA_CLIENT_SECRET") ?: error("STRAVA_CLIENT_SECRET not set")
    val redirectUri = System.getenv("STRAVA_REDIRECT_URI") ?: "http://localhost:8080/api/v1/callback"
    val frontendUrl = System.getenv("FRONTEND_URL") ?: "http://localhost:5173"

    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }

    install(Sessions) {
        cookie<UserSession>("SESSION")
    }

    install(CORS) {
        allowCredentials = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    routing {
        get("/") {
            call.respondText("Travalt backend running")
        }

        route("/api/v1") {
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
                call.application.environment.log.info("Redirecting authenticated user to {}", frontendUrl)
                call.respondRedirect(frontendUrl)
            }

            get("/logout") {
                call.sessions.clear<UserSession>()
                call.respondRedirect(frontendUrl)
            }

            get("/me") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val athlete: Athlete = httpClient.get("https://www.strava.com/api/v3/athlete") {
                    header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                }.body()

                call.respond(
                    UserInfo(
                        name = "${athlete.firstname} ${athlete.lastname}",
                        avatar = athlete.profile
                    )
                )
            }

            get("/activities") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val page = offset / limit + 1

                val activities: List<ActivitySummary> = httpClient.get("https://www.strava.com/api/v3/athlete/activities") {
                    header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                    parameter("page", page)
                    parameter("per_page", limit)
                }.body()

                val start = offset % limit
                val result = if (start > 0) activities.drop(start) else activities

                call.respond(result)
            }
        }
    }
}

@Serializable
data class UserSession(val accessToken: String)

@Serializable
data class TokenResponse(val access_token: String, val athlete: Athlete)

@Serializable
data class Athlete(val firstname: String, val lastname: String, val profile: String)

@Serializable
data class UserInfo(val name: String, val avatar: String)

@Serializable
data class ActivitySummary(
    val id: Long,
    val name: String,
    val start_date: String,
    val type: String
)
