package com.lastwave.app.playback

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.local.db.DownloadedTrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflinePlaybackResolverTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `download keys trim and lowercase artist before title`() {
        assertThat(OfflinePlaybackResolver.makeDownloadKey(" Song ", " ARTIST "))
            .isEqualTo("artist_song")
    }

    @Test
    fun `playback URIs preserve content and file schemes and normalize absolute paths`() {
        listOf(
            " content://media/external/audio/media/7 " to "content://media/external/audio/media/7",
            "file:///storage/Music/song.flac" to "file:///storage/Music/song.flac",
            "/storage/Music/song.flac" to "file:///storage/Music/song.flac",
        ).forEach { (raw, expected) ->
            assertThat(OfflinePlaybackResolver.toPlaybackUriString(raw)).isEqualTo(expected)
        }
    }

    @Test
    fun `valid database download never resolves remotely`() = runBlocking {
        val downloaded = DownloadedTrackEntity(
            title = "Song", artist = "Artist", filePath = "/storage/Music/song.flac",
            mediaStoreUri = "content://media/external/audio/media/7",
        )
        val uri = OfflinePlaybackResolver.selectPlaybackUri(
            title = " Song ", artist = " Artist ",
            findDownload = { key, title, artist ->
                assertThat(key).isEqualTo("artist_song")
                assertThat(title).isEqualTo("Song")
                assertThat(artist).isEqualTo("Artist")
                downloaded
            },
            localExists = { true },
            resolveRemote = { error("Network must not be needed for a local download") },
        )
        assertThat(uri).isEqualTo(downloaded.mediaStoreUri)
    }

    @Test
    fun `stale row is cleaned up before orphan storage fallback without network`() = runBlocking {
        val downloaded = DownloadedTrackEntity(title = "Song", artist = "Artist", filePath = "/gone.flac")
        var cleaned = false
        val uri = OfflinePlaybackResolver.selectPlaybackUri(
            title = "Song", artist = "Artist",
            findDownload = { _, _, _ -> downloaded },
            localExists = { false },
            onStaleDownload = { cleaned = true },
            findStorageUri = {
                assertThat(cleaned).isTrue()
                "file:///storage/Music/orphan.flac"
            },
            resolveRemote = { error("Orphan download must play offline") },
        )
        assertThat(uri).isEqualTo("file:///storage/Music/orphan.flac")
    }

    @Test
    fun `public naming probe skips empty downloads and finds next playable extension`() {
        temporaryFolder.newFile("Artist - Song.flac")
        val playable = temporaryFolder.newFile("Artist - Song.opus").apply { writeBytes(byteArrayOf(1)) }
        assertThat(OfflinePlaybackResolver.findPublicDownloadFile(
            musicDir = temporaryFolder.root,
            title = " Song ", artist = " Artist ",
            sanitize = { it }, extensions = listOf("flac", "opus"),
        )).isEqualTo(playable)
    }
}
