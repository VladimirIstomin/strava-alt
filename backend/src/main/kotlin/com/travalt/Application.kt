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
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.callloging.CallLogging
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
import io.ktor.http.Cookie
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

import com.travalt.AppConfig

private val log = LoggerFactory.getLogger("Application")

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val config = AppConfig.load().also { it.logSanitized(log) }
    val clientId = config.stravaClientId
    val clientSecret = config.stravaClientSecret
    val redirectUri = config.stravaRedirectUri
    val frontendUrl = config.frontendUrl
    val cookieDomain = config.cookieDomain

    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }


    install(CORS) {
        allowCredentials = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }

    install(CallLogging) {
        logger = log
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
                    parameters.append("scope", "read,activity:read")
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

                call.response.cookies.append(
                    Cookie(
                        name = "refresh_token",
                        value = token.refresh_token,
                        httpOnly = true,
                        path = "/",
                        domain = cookieDomain
                    )
                )

                val redirect = URLBuilder(frontendUrl).apply {
                    parameters.append("access_token", token.access_token)
                }.buildString()
                log.info("Redirecting authenticated user to {}", redirect)
                call.respondRedirect(redirect)
            }

            get("/logout") {
                call.response.cookies.append(
                    Cookie(
                        name = "refresh_token",
                        value = "",
                        maxAge = 0,
                        path = "/",
                        domain = cookieDomain
                    )
                )
                call.respondRedirect(frontendUrl)
            }

            post("/refresh") {
                val refresh = call.request.cookies["refresh_token"]
                if (refresh == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val token: TokenResponse = httpClient.post("https://www.strava.com/oauth/token") {
                    parameter("client_id", clientId)
                    parameter("client_secret", clientSecret)
                    parameter("refresh_token", refresh)
                    parameter("grant_type", "refresh_token")
                }.body()

                call.response.cookies.append(
                    Cookie(
                        name = "refresh_token",
                        value = token.refresh_token,
                        httpOnly = true,
                        path = "/",
                        domain = cookieDomain
                    )
                )

                call.respond(mapOf("access_token" to token.access_token))
            }

            get("/me") {
                val header = call.request.headers[HttpHeaders.Authorization]
                val token = header?.removePrefix("Bearer ")?.trim()
                if (token.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val athlete: Athlete = httpClient.get("https://www.strava.com/api/v3/athlete") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body()

                call.respond(
                    UserInfo(
                        name = "${athlete.firstname} ${athlete.lastname}",
                        avatar = athlete.profile
                    )
                )
            }

            get("/activities") {
                val header = call.request.headers[HttpHeaders.Authorization]
                val token = header?.removePrefix("Bearer ")?.trim()
                if (token.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val page = offset / limit + 1

                val activities: List<ActivitySummary> = httpClient.get("https://www.strava.com/api/v3/athlete/activities") {
                    header(HttpHeaders.Authorization, "Bearer $token")
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
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_at: Long,
    val athlete: Athlete? = null
)

@Serializable
data class Athlete(val firstname: String, val lastname: String, val profile: String)

@Serializable
data class UserInfo(val name: String, val avatar: String)

@Serializable
data class ActivitySummary(
    val id: Long,
    val name: String,
    @SerialName("start_date")
    val startDate: String,
    val type: String,
    @SerialName("average_heartrate")
    val averageHeartrate: Double? = null,
    @SerialName("average_speed")
    val averageSpeed: Double? = null,
    @SerialName("moving_time")
    val movingTime: Int? = null,
    val distance: Double? = null,
    @SerialName("average_cadence")
    val averageCadence: Double? = null
)
