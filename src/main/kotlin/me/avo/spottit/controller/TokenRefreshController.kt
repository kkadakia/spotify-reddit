package me.avo.spottit.controller

import me.avo.spottit.service.SpotifyAuthService
import me.avo.spottit.util.TokenUtil
import org.slf4j.LoggerFactory

class TokenRefreshController(
    private val spotifyAuthService: SpotifyAuthService
) {

    fun refresh(): String {
        logger.info("Refreshing Spotify access token")
        val refreshToken = TokenUtil.refreshTokenFile.readText()
        spotifyAuthService.refreshToken = refreshToken

        val newAccessToken = spotifyAuthService.refresh()
        spotifyAuthService.accessToken = newAccessToken
        TokenUtil.accessTokenFile.writeText(newAccessToken)
        logger.info("The access token has been refreshed")
        return newAccessToken
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

}