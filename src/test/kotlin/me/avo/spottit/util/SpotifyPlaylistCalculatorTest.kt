package me.avo.spottit.util

import me.avo.spottit.makeTracks
import me.avo.spottit.track
import org.amshove.kluent.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SpotifyPlaylistCalculatorTest {

    @Nested inner class CreateIndexLookup {

        @Test fun `should return sorted indices per id`() {
            val tracks = makeTracks(3) + track("0")
            val result = SpotifyPlaylistCalculator.createIndexLookup(tracks)

            result["0"].shouldNotBeNull().let {
                it.pop() shouldEqualTo 3 // last index of track with id 0
                it.pop() shouldEqualTo 0 // next index of track with id 0 after removing popping the previous
            }

        }

    }

    @Nested inner class CalculateTracksToRemoveAndAdd {

        @Test fun `should add tracks that are not in playlist yet`() {
            val maxSize = 10
            val tracksToAdd = makeTracks(10)
            val tracksInPlaylist = makeTracks(5)
            val (remove, add) = SpotifyPlaylistCalculator.calculateTracksToRemoveAndAdd(
                tracksToAdd,
                maxSize,
                tracksInPlaylist
            )

            remove.shouldBeEmpty()
            add.shouldContainAll(tracksToAdd.takeLast(5))
        }

        @Test fun `should remove duplicates`() {
            val maxSize = 10
            val tracksToAdd = makeTracks(5)
            val duplicates = makeTracks(2)
            val tracksInPlaylist = makeTracks(5) + duplicates

            val (remove, add) = SpotifyPlaylistCalculator.calculateTracksToRemoveAndAdd(
                tracksToAdd,
                maxSize,
                tracksInPlaylist
            )

            remove.size shouldEqualTo 2
            add.shouldBeEmpty()
        }

    }

    @Nested inner class CalculateTracksToAdd {

        @Test fun `should add no more than max`() {
            val tracksInPlaylist = listOf(
                track("1")
            )

            val tracksToAdd = listOf(
                track { setId("1") }, // will be removed
                track { setId("2") },
                track { setId("3") },
                track { setId("4") }
            )

            val willBeAddedAgain = listOf(
                track("1")
            )

            val maxSize = 3

            val actual =
                SpotifyPlaylistCalculator.calculateTracksToAdd(
                    tracksInPlaylist.size,
                    0,
                    tracksToAdd,
                    willBeAddedAgain,
                    maxSize
                )

            actual.size shouldEqualTo maxSize - tracksInPlaylist.size

            actual.map { it.id } shouldContainAll listOf("2", "3")
        }

        @Test fun `should return empty list when amountInPlaylist is biggerEqual than maxSize`() {
            SpotifyPlaylistCalculator.calculateTracksToAdd(10, 0, listOf(track { }), listOf(), 5).shouldBeEmpty()
        }

        @Test fun `should account for removed tracks`() {
            val result = SpotifyPlaylistCalculator.calculateTracksToAdd(5, 2, makeTracks(2), listOf(track("0")), 5)
            result.size shouldEqualTo 1
            result.first().id shouldBeEqualTo "1"
        }

    }

    @Nested inner class AddMaxSizeTracks {

        @Test fun `should never return more than maxSize`() {
            val maxSize = 20
            val tracksToAdd = makeTracks(40)
            val result = SpotifyPlaylistCalculator.addMaxSizeTracks(tracksToAdd, maxSize)
            result.size shouldBeLessOrEqualTo maxSize
        }

        @Test fun `should return tracksToAdd if there are less than maxSize`() {
            val maxSize = 20
            val tracksToAdd = makeTracks(10)
            val result = SpotifyPlaylistCalculator.addMaxSizeTracks(tracksToAdd, maxSize)
            result.size shouldEqualTo tracksToAdd.size
        }
    }

    @Nested inner class CalculateTracksToRemove {

        @Test fun `when sizeAfterRemoval is less than maxSize`() {
            val currentSize = 20
            val willBeAddedAgain = makeTracks(5)
            val willBeRemoved = makeTracks(6, 5)
            val maxSize = 25

            val (tracksToRemove, filteredAddedAgain) =
                SpotifyPlaylistCalculator.calculateTracksToRemove(
                    currentSize,
                    willBeAddedAgain,
                    willBeRemoved,
                    maxSize
                )

            tracksToRemove.size shouldEqualTo willBeRemoved.size
            tracksToRemove shouldContainAll willBeRemoved
            filteredAddedAgain shouldEqual willBeAddedAgain
        }

        @Test fun `when sizeAfterRemoval is greater than maxSize`() {
            val currentSize = 20
            val willBeAddedAgain = makeTracks(5)
            val willBeRemoved = makeTracks(1, 5)
            val maxSize = 15

            val (tracksToRemove, filteredAddedAgain) =
                SpotifyPlaylistCalculator.calculateTracksToRemove(
                    currentSize,
                    willBeAddedAgain,
                    willBeRemoved,
                    maxSize
                )

            tracksToRemove.size shouldEqualTo currentSize - maxSize
            tracksToRemove shouldContainAll willBeRemoved
            tracksToRemove shouldContainSome willBeAddedAgain
            filteredAddedAgain.size shouldEqualTo 1
            filteredAddedAgain shouldEqual willBeAddedAgain.take(1)
        }
    }

    @Nested inner class CalculateToRemoveAndAddAgain {

        @Test fun `should remove duplicates`() {
            val duplicateId = "1"
            val tracksInPlaylist = makeTracks(5) + track { setId(duplicateId) }
            val idsToAdd = listOf(duplicateId)
            val maxSize = 5

            val (tracksToRemove, willBeAddedAgain) = SpotifyPlaylistCalculator.calculateToRemoveAndAddAgain(
                tracksInPlaylist,
                idsToAdd,
                maxSize
            )

            tracksToRemove.size shouldEqualTo 5
            tracksToRemove.map { it.track.id } shouldContain duplicateId

            willBeAddedAgain.map { it.id }
            willBeAddedAgain.first().id shouldBeEqualTo duplicateId
            willBeAddedAgain.size shouldEqualTo 1
        }
    }
}
