package com.lastwave.app.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenVuMeterTest {
    @Test
    fun `portrait window uses rotated landscape content even when orientation request is ignored`() {
        assertTrue(vuMeterNeedsRotation(1080, 2400))
        assertTrue(vuMeterNeedsRotation(600, 900))
    }

    @Test
    fun `landscape and square windows show content without rotation`() {
        assertFalse(vuMeterNeedsRotation(2400, 1080))
        assertFalse(vuMeterNeedsRotation(900, 600))
        assertFalse(vuMeterNeedsRotation(800, 800))
    }
}
