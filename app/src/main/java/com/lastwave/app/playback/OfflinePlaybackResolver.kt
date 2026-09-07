package com.lastwave.app.playback

import com.lastwave.app.data.local.db.DownloadedTrackEntity

/**
 * Offline-first playback resolution.
 *
 * Single place that decides whether a requested track already has a valid
 * local download. When it does, Media3/ExoPlayer must play that local file
 * instead of resolving the remote URL — including when the network is
 * unavailable.
 *
 * Flow:
 * ```
 * Play requested -> Is this track downloaded locally?
 *   YES -> Play local file
 *   NO  -> Existing remote streaming/resolution logic
 * ```
 *
 * Stale records (DB row present but local file missing/unreadable) are
 * reported via [onStaleDownload] so callers can reuse their existing cleanup
 * (e.g. delete the row) and then keep looking: first the public download
 * directory on disk, and only then remote resolution.
 *
 * The public-directory probe is the second-chance path for when the Room
 * lookup misses entirely (history cleared, reinstall, sync not yet run) or
 * the stored row is stale but the file is still on disk. It reuses the
 * downloader's own naming (sanitized `"artist - title"` + known audio
 * extensions), injected as lambdas so this object stays free of Android
 * framework dependencies and unit-testable on the JVM.
 *
 * Pure Kotlin (no Android framework dependency) so the decision table is
 * unit-testable on the JVM.
 */
object OfflinePlaybackResolver {

    /**
     * Normalized download key. Must stay in sync with
     * `TrackDownloadManager.makeDownloadKey` ("${artist}_${title}",
     * trimmed + lowercased) so playback lookups hit the same rows that the
     * download manager writes (unique `trackKey` index).
     */
    fun makeDownloadKey(title: String, artist: String): String =
        "${artist.trim().lowercase()}_${title.trim().lowercase()}"

    /**
     * Raw stored location for a download: MediaStore `content://` URI when
     * present, otherwise the filesystem path. Returns null when the record
     * carries no usable location.
     */
    fun rawLocalUriString(downloaded: DownloadedTrackEntity): String? =
        downloaded.mediaStoreUri?.takeIf { it.isNotBlank() }
            ?: downloaded.filePath.takeIf { it.isNotBlank() }

    /**
     * Maps a stored download location to a playback URI string for Media3:
     * - `content://` URIs are passed through unchanged.
     * - `file://` URIs are passed through unchanged.
     * - Absolute filesystem paths (`/…`) become `file://` URIs
     *   (equivalent to `Uri.fromFile(File(path)).toString()`).
     * - Anything else (e.g. already-resolved remote URL) is returned as-is.
     */
    fun toPlaybackUriString(rawUriOrPath: String): String {
        val raw = rawUriOrPath.trim()
        if (raw.startsWith("content://")) return raw
        if (raw.startsWith("file://")) return raw
        if (raw.startsWith("/")) return "file://$raw"
        return raw
    }

    /** Playback URI string for a download record, or null if it has no location. */
    fun localPlaybackUriString(downloaded: DownloadedTrackEntity): String? =
        rawLocalUriString(downloaded)?.let(::toPlaybackUriString)

    /**
     * Sanitized `"artist - title"` base name, using the downloader's filename
     * sanitization (pass `TrackDownloadManager::sanitizeDownloadFilename`).
     */
    fun publicDownloadBaseName(
        title: String,
        artist: String,
        sanitize: (String) -> String,
    ): String = sanitize("${artist.trim()} - ${title.trim()}")

    /**
     * Second-chance probe: finds a downloaded audio file directly in the
     * public download directory, without consulting Room.
     *
     * @param musicDir the downloader's public music directory
     *   (`Music/LastWave`).
     * @param sanitize the downloader's filename sanitization.
     * @param extensions playable audio extensions in probe order.
     * @return the first non-empty match, or null when nothing valid exists.
     */
    fun findPublicDownloadFile(
        musicDir: java.io.File,
        title: String,
        artist: String,
        sanitize: (String) -> String,
        extensions: List<String>,
    ): java.io.File? {
        if (!musicDir.isDirectory) return null
        val base = publicDownloadBaseName(title, artist, sanitize)
        return extensions.asSequence()
            .map { java.io.File(musicDir, "$base.$it") }
            .firstOrNull { it.isFile && it.length() > 0 }
    }

    /**
     * Offline-first selection.
     *
     * @param findDownload DAO lookup: given the normalized [makeDownloadKey],
     *   plus trimmed title/artist for the `findByTitleAndArtist` fallback,
     *   returns the matching download record or null.
     * @param localExists returns true when the record's local file is present
     *   and readable (file `exists() && length() > 0`, or MediaStore stream
     *   opens successfully).
     * @param resolveRemote existing remote streaming/resolution logic; only
     *   invoked when no *valid* local download exists.
     * @param onStaleDownload invoked once when the DB has a record but the
     *   file is gone, so the caller can reuse its existing stale-record
     *   cleanup instead of duplicating it here.
     * @param findStorageUri second-chance probe for a valid file on disk when
     *   the Room lookup misses or is stale. Only when this also returns null
     *   is [resolveRemote] invoked.
     */
    suspend fun selectPlaybackUri(
        title: String,
        artist: String,
        findDownload: suspend (trackKey: String, title: String, artist: String) -> DownloadedTrackEntity?,
        localExists: (DownloadedTrackEntity) -> Boolean,
        resolveRemote: suspend () -> String,
        onStaleDownload: suspend (DownloadedTrackEntity) -> Unit = {},
        findStorageUri: (() -> String?)? = null,
    ): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) {
            return resolveRemote()
        }
        val downloaded = runCatching {
            findDownload(makeDownloadKey(cleanTitle, cleanArtist), cleanTitle, cleanArtist)
        }.getOrNull()

        if (downloaded != null) {
            if (runCatching { localExists(downloaded) }.getOrDefault(false)) {
                localPlaybackUriString(downloaded)?.let { return it }
            } else {
                runCatching { onStaleDownload(downloaded) }
            }
        }
        if (findStorageUri != null) {
            runCatching { findStorageUri() }.getOrNull()?.let { return it }
        }
        return resolveRemote()
    }
}
