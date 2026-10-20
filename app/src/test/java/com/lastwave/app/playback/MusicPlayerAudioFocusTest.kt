package com.lastwave.app.playback

import android.content.Context
import android.content.ContextWrapper
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24, 28])
class MusicPlayerAudioFocusTest {
    @Suppress("DEPRECATION")
    @Test
    fun `audio focus request and release use the supported platform API`() {
        val manager = mockk<AudioManager>(relaxed = true)
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getApplicationContext(): Context = this
            override fun getSystemService(name: String): Any? =
                if (name == Context.AUDIO_SERVICE) manager else super.getSystemService(name)
        }
        // Keep background observers and the playback ticker out of this focus-only test.
        val scope = CoroutineScope(Job().apply { cancel() })
        val player = MusicPlayer(
            context = context,
            innerTube = mockk(relaxed = true),
            relatedGraph = mockk(relaxed = true),
            losslessMusicApi = mockk(relaxed = true),
            moduleDrmFactory = mockk(relaxed = true),
            moduleManager = mockk(relaxed = true),
            offlineLicense = mockk(relaxed = true),
            segBridge = mockk(relaxed = true),
            settingsPreferences = mockk(relaxed = true),
            equalizerPreferences = mockk(relaxed = true),
            discoverRepository = mockk(relaxed = true),
            playlistRepository = mockk(relaxed = true),
            nativeAudioEngine = mockk(relaxed = true),
            audioEffectsEngine = mockk(relaxed = true),
            applicationScope = scope,
            downloadedTrackDao = mockk(relaxed = true),
            usbDacMonitor = mockk(relaxed = true),
            exclusiveUsbOutput = mockk(relaxed = true),
            songPlayStatsRepository = mockk(relaxed = true),
            internalDirectOutput = mockk(relaxed = true),
            podcastProgressStore = mockk(relaxed = true),
            autoEqController = mockk(relaxed = true),
        )
        listOf("requestMediaAudioFocus", "abandonMediaAudioFocus").forEach { name ->
            MusicPlayer::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(player)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify(exactly = 1) { manager.requestAudioFocus(any<AudioFocusRequest>()) }
            verify(exactly = 1) { manager.abandonAudioFocusRequest(any()) }
        } else {
            verify(exactly = 1) {
                manager.requestAudioFocus(any<AudioManager.OnAudioFocusChangeListener>(), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
            verify(exactly = 1) { manager.abandonAudioFocus(any()) }
        }
    }
}
