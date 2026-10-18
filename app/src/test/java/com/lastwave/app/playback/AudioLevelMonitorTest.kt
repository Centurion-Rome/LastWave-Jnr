package com.lastwave.app.playback

import android.os.SystemClock
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class AudioLevelMonitorTest {
    @Before fun setUp() = AudioLevelMonitor.reset()
    @After fun tearDown() = AudioLevelMonitor.reset()

    @Test fun `left-only PCM leaves the right meter at rest`() {
        val levels = feed(0.5f, 0f)
        assertThat(levels.left).isGreaterThan(0f)
        assertThat(levels.right).isEqualTo(0f)
        assertThat(levels.bass).isGreaterThan(0f)
    }

    @Test fun `right-only PCM leaves the left meter at rest`() {
        val levels = feed(0f, 0.5f)
        assertThat(levels.left).isEqualTo(0f)
        assertThat(levels.right).isGreaterThan(0f)
    }

    @Test fun `mono feeds both meters equally`() {
        val levels = feed(0.5f, channels = 1)
        assertThat(levels.left).isGreaterThan(0f)
        assertThat(levels.left).isEqualTo(levels.right)
        assertThat(levels.bass).isWithin(0.00001f).of(levels.left)
    }

    @Test fun `shared reference preserves unequal channel amplitudes`() {
        val levels = feed(0.8f, 0.2f)
        assertThat(levels.right / levels.left).isWithin(0.0001f).of(0.25f)
    }

    @Test fun `opposite phase channels do not cancel stereo meters`() {
        val levels = feed(0.5f, -0.5f)
        assertThat(levels.left).isGreaterThan(0f)
        assertThat(levels.left).isEqualTo(levels.right)
        // The existing mono feed still uses its original summed signal.
        assertThat(levels.bass).isEqualTo(0f)
    }

    @Test fun `freshness expires at 400ms and reset clears published and accumulated levels`() {
        val levels = feed(0.8f, 0.2f)
        assertThat(levels.isFresh(levels.updatedMs)).isTrue()
        assertThat(levels.isFresh(levels.updatedMs + 399)).isTrue()
        assertThat(levels.isFresh(levels.updatedMs + 400)).isFalse()
        assertThat(levels.isFresh(levels.updatedMs - 1)).isFalse()
        AudioLevelMonitor.reset()
        assertThat(AudioLevelMonitor.levels.value).isEqualTo(AudioLevelMonitor.Levels())
        assertThat(AudioLevelMonitor.levels.value.isFresh(SystemClock.elapsedRealtime())).isFalse()
        val silence = feed(0f, 0f)
        assertThat(silence.left).isEqualTo(0f)
        assertThat(silence.right).isEqualTo(0f)
        assertThat(silence.bass).isEqualTo(0f)
    }

    @Test fun `invalid dimensions and truncated buffers are ignored`() {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
        AudioLevelMonitor.onProcessedPcm(buffer, Int.MAX_VALUE, 2, 48_000)
        AudioLevelMonitor.onProcessedPcm(buffer, 1, 0, 48_000)
        AudioLevelMonitor.onProcessedPcm(buffer, 1, 1, 0)
        assertThat(AudioLevelMonitor.levels.value).isEqualTo(AudioLevelMonitor.Levels())
    }

    private fun feed(left: Float, right: Float = left, channels: Int = 2): AudioLevelMonitor.Levels {
        val frames = 480
        val buffer = ByteBuffer.allocate(frames * channels * 4).order(ByteOrder.nativeOrder())
        repeat(frames) {
            buffer.putFloat(left)
            if (channels == 2) buffer.putFloat(right)
        }
        buffer.flip()
        SystemClock.sleep(50)
        AudioLevelMonitor.onProcessedPcm(buffer, frames, channels, 48_000)
        assertThat(buffer.position()).isEqualTo(0)
        assertThat(buffer.limit()).isEqualTo(frames * channels * 4)
        return AudioLevelMonitor.levels.value
    }
}
