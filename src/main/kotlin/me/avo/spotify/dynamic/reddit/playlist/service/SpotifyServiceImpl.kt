package me.avo.spotify.dynamic.reddit.playlist.service

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.Track
import me.avo.spotify.dynamic.reddit.playlist.model.RedditTrack
import org.slf4j.LoggerFactory

class SpotifyServiceImpl(
    private val authService: SpotifyAuthService
) : SpotifyService {

    override fun updatePlaylist(tracks: List<Track>, userId: String, playlistId: String) {
        logger.info("Updating Playlist")
        logger.info("Found ${tracks.size} tracks")

        val api = authService.getSpotifyApi()

        api.clearPlaylist(tracks, userId, playlistId)
            .also { logger.info("Adding ${it.size} tracks to the playlist") }
            .let { api.addTracks(it, userId, playlistId) }
    }

    override fun findTracks(tracks: List<RedditTrack>): List<Track> = authService.getSpotifyApi()
        .searchForTracks(tracks)
        .mapNotNull { (_, results) -> results.firstOrNull() } // TODO how to select from multiple results?

    private fun SpotifyApi.searchForTracks(tracks: List<RedditTrack>): List<Pair<RedditTrack, Array<Track>>> = tracks
        .map { findTrack(it) }
        .onEach { Thread.sleep(250) }

    private fun SpotifyApi.findTrack(track: RedditTrack): Pair<RedditTrack, Array<Track>> {
        val query = listOf(track.artist, track.title, track.mix ?: "").joinToString(" ")
        logger.debug("Searching for $query")
        val results = searchTracks(query).limit(10).offset(0).build().execute()
        val items = results.items
        items.firstOrNull()
            .let { "Found ${results.total} results. " + if (it != null) "Top 1: ${it.artists.joinToString { it.name }} ${it.name}" else "" }
            .let { if (results.total == 0) logger.debug(it) else logger.info(it) }
        return track to items
    }

    private fun SpotifyApi.clearPlaylist(tracksToAdd: List<Track>, userId: String, playlistId: String): List<Track> {
        logger.info("Clearing Playlist")
        val idsToAdd = tracksToAdd.map { it.id }
        val tracksInPlaylist = getPlaylistsTracks(userId, playlistId).build().execute().items.map { it.track }

        return when {
            tracksInPlaylist.isEmpty() -> {
                logger.info("The playlist is currently empty, all found tracks will be added")
                tracksToAdd
            }
            else -> {
                val (willBeAddedAgain, willBeRemoved) = tracksInPlaylist.partition { idsToAdd.contains(it.id) }
                logger.info("${willBeAddedAgain.size} tracks are already in the playlist, ${willBeRemoved.size} tracks will be removed")
                val jsonTracks = willBeRemoved.map { jsonObject("uri" to it.uri) }.toJsonArray()
                removeTracksFromPlaylist(userId, playlistId, jsonTracks).build().execute()
                val againIds = willBeAddedAgain.map { it.id }
                tracksToAdd.filterNot { againIds.contains(it.id) }
            }
        }
    }

    private fun SpotifyApi.addTracks(tracks: Collection<Track>, userId: String, playlistId: String) {
        if (tracks.isEmpty()) {
            logger.warn("Did not find any tracks to add")
            return
        }
        addTracksToPlaylist(userId, playlistId, tracks.map { it.uri }.toTypedArray()).build().execute()
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

}