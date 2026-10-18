package com.lastwave.app.ui.player

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.playback.AudioLevelMonitor
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.ui.common.AnalogVuMeter
import com.lastwave.app.ui.common.LocalOpenVuMeter
import kotlinx.coroutines.delay

/** A separate window keeps this above navigation sheets without dismissing the previous view. */
@Composable
internal fun FullscreenVuMeter(player: MusicPlayer, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
    }
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            previousOrientation?.let { activity.requestedOrientation = it }
        }
    }
    val state by player.chromeState.collectAsStateWithLifecycle()
    val levels by AudioLevelMonitor.levels.collectAsStateWithLifecycle()
    var fresh by remember { mutableStateOf(false) }
    LaunchedEffect(levels.updatedMs) {
        val age = SystemClock.elapsedRealtime() - levels.updatedMs
        fresh = levels.isFresh(SystemClock.elapsedRealtime())
        if (fresh) delay((400L - age).coerceIn(1L, 400L))
        fresh = false
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            // Only change the temporary window; the underlying view keeps its system-bar state.
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            dialogWindow?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { }
        }
        CompositionLocalProvider(LocalOpenVuMeter provides null) {
            Box(
                Modifier.fillMaxSize().background(Color.Black)
                    .windowInsetsPadding(WindowInsets.displayCutout).vuMeterLandscapeLayout(),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(stringResource(R.string.vu_left), color = Color.White,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(stringResource(R.string.vu_right), color = Color.White,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val height = maxHeight
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            AnalogVuMeter(state.isPlaying, Modifier.weight(1f),
                                level = if (state.isPlaying && fresh) levels.left else 0f, meterHeight = height)
                            AnalogVuMeter(state.isPlaying, Modifier.weight(1f),
                                level = if (state.isPlaying && fresh) levels.right else 0f, meterHeight = height)
                        }
                    }
                    Text(
                        state.current?.title ?: stringResource(R.string.vu_nothing_playing),
                        Modifier.padding(top = 8.dp), color = Color.White,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconButton(onClick = player::previous, enabled = state.current != null) {
                            Icon(Icons.Filled.SkipPrevious, stringResource(R.string.desc_previous), tint = Color.White)
                        }
                        IconButton(onClick = player::togglePlayPause, enabled = state.current != null) {
                            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                stringResource(if (state.isPlaying) R.string.vu_pause else R.string.vu_play), tint = Color.White)
                        }
                        IconButton(onClick = player::next, enabled = state.current != null) {
                            Icon(Icons.Filled.SkipNext, stringResource(R.string.desc_next), tint = Color.White)
                        }
                    }
                    SeekBar(
                        progressState = player.progressState, isPlaying = state.isPlaying,
                        trackKey = state.current?.let { it.videoId ?: "${it.artist}|${it.title}" },
                        wavyEnabled = false, onSeek = player::seekTo, isTranslucent = true,
                        fallbackDurationMs = state.current?.durationMs ?: 0L,
                        primaryColor = Color(0xFFFFC64D),
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, stringResource(R.string.vu_close), tint = Color.White)
                }
            }
        }
    }
}

internal fun vuMeterNeedsRotation(width: Int, height: Int): Boolean = height > width

// Window bounds, not Dialog configuration, also cover ignored orientation requests in multi-window.
private fun Modifier.vuMeterLandscapeLayout() = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val rotate = vuMeterNeedsRotation(width, height)
    val child = measurable.measure(if (rotate) Constraints.fixed(height, width) else constraints)
    layout(width, height) {
        child.placeWithLayer(0, 0) {
            transformOrigin = TransformOrigin(0f, 0f)
            rotationZ = if (rotate) 90f else 0f
            translationX = if (rotate) width.toFloat() else 0f
        }
    }
}
