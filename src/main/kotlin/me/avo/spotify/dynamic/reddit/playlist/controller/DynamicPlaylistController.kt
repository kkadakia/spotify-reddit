package me.avo.spotify.dynamic.reddit.playlist.controller

import com.wrapper.spotify.model_objects.specification.Track
import me.avo.spotify.dynamic.reddit.playlist.config.kodein
import me.avo.spotify.dynamic.reddit.playlist.model.Playlist
import me.avo.spotify.dynamic.reddit.playlist.server.prepareServer
import me.avo.spotify.dynamic.reddit.playlist.service.RedditService
import me.avo.spotify.dynamic.reddit.playlist.service.SpotifyAuthService
import me.avo.spotify.dynamic.reddit.playlist.service.SpotifyService
import me.avo.spotify.dynamic.reddit.playlist.util.YamlConfigReader
import me.avo.spotify.dynamic.reddit.playlist.util.openUrlInBrowser
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class DynamicPlaylistController(
    private val spotifyAuthService: SpotifyAuthService,
    private val redditServiceFactory: (Playlist) -> RedditService
) {

    private val spotifyService = SpotifyService(spotifyAuthService)

    fun updatePlaylists(configPath: String) {
        val configuration = YamlConfigReader.read(File(configPath).readText())

        val uri = spotifyAuthService.getRedirectUri().toString().also(::println)

        val server = prepareServer(kodein).start(wait = false)

        try {
            openUrlInBrowser(uri)
            //runClient(uri, server)
        } finally {
            Thread.sleep(5000)
            println("Shutting down server in $timeout seconds")
            server.stop(timeout, timeout, TimeUnit.SECONDS)
        }

        configuration.playlists.forEach(::processPlaylist)
    }

    private fun processPlaylist(playlist: Playlist) {
        logger.info("Processing playlist ${playlist.id}")
        //val postFilter = playlist.postFilters.first()
        val redditService = redditServiceFactory(playlist)

        val foundTracks = mutableListOf<Track>()

        val tracks = redditService.getTracks(playlist)
        spotifyService
            .findTracks(tracks)
            .mapTo(foundTracks) { it }
        /*
        when (redditService) {
            is RedditServiceAccurateImpl -> while (foundTracks.size < playlist.maxSize && !redditService.done) {
                val tracks = redditService.getTracks(playlist).onEach(::println)
                logger.info("Pre lookup: ${tracks.size} tracks")
                //tracks.mapNotNull(RedditTrack::flair).distinct().let(::println)
                spotifyService
                    .findTracks(tracks)
                    .mapTo(foundTracks) { it }
                    .also { redditService.update(it.size) }
            }
            else -> {

            }
        }
*/
        spotifyService.updatePlaylist(foundTracks, playlist.userId, playlist.id)
    }

    private val timeout = 3L
    private val logger = LoggerFactory.getLogger(this::class.java)

}