package com.travalt

import com.typesafe.config.ConfigFactory

/**
 * Application configuration loaded from [application.conf].
 */
data class AppConfig(
    val stravaClientId: String,
    val stravaClientSecret: String,
    val stravaRedirectUri: String,
    val frontendUrl: String,
    val cookieDomain: String?
) {
    companion object {
        fun load(): AppConfig {
            val config = ConfigFactory.load().getConfig("app")

            val clientId = if (config.hasPath("strava.clientId"))
                config.getString("strava.clientId")
            else error("strava.clientId not set")

            val clientSecret = if (config.hasPath("strava.clientSecret"))
                config.getString("strava.clientSecret")
            else error("strava.clientSecret not set")

            val redirectUri = if (config.hasPath("strava.redirectUri"))
                config.getString("strava.redirectUri")
            else "http://localhost:8080/api/v1/callback"

            val frontendUrl = if (config.hasPath("frontend.url"))
                config.getString("frontend.url")
            else "http://localhost:5173"

            val cookieDomain = if (config.hasPath("cookie.domain"))
                config.getString("cookie.domain")
            else null

            return AppConfig(
                stravaClientId = clientId,
                stravaClientSecret = clientSecret,
                stravaRedirectUri = redirectUri,
                frontendUrl = frontendUrl,
                cookieDomain = cookieDomain
            )
        }
    }

    fun logSanitized(log: org.slf4j.Logger) {
        log.info(
            "AppConfig: stravaClientId={}, stravaRedirectUri={}, frontendUrl={}, cookieDomain={}",
            stravaClientId,
            stravaRedirectUri,
            frontendUrl,
            cookieDomain ?: "<none>"
        )
    }
}
