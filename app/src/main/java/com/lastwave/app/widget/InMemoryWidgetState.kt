package com.lastwave.app.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * No-op, in-memory [GlanceStateDefinition].
 *
 * Glance's default state definition, `PreferencesGlanceStateDefinition`,
 * performs a Preferences DataStore file read/write on every widget session
 * operation (initial composition, each `update()`, every action callback) —
 * regardless of whether the widget's composable actually reads that state.
 * [NowPlayingWidget] already persists everything it needs via its own
 * SharedPreferences-backed [NowPlayingWidgetSnapshot], so Glance's own store
 * is pure, unused failure surface sitting in front of every render.
 *
 * Overriding `GlanceAppWidget.stateDefinition` with this object replaces
 * that file-backed store with an in-memory [DataStore] holding [Unit], so
 * the render path never touches disk for Glance's own state.
 */
object InMemoryWidgetState : GlanceStateDefinition<Unit> {

    private val store: DataStore<Unit> = object : DataStore<Unit> {
        private val flow = MutableStateFlow(Unit)

        override val data: Flow<Unit> = flow

        override suspend fun updateData(transform: suspend (Unit) -> Unit): Unit {
            transform(Unit)
            flow.value = Unit
            return Unit
        }
    }

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Unit> = store

    // Required by the interface but never read from since [store] never
    // touches disk; kept under cacheDir so nothing is ever created there.
    override fun getLocation(context: Context, fileKey: String): File =
        File(context.cacheDir, "lastwave_glance_noop_state_$fileKey")
}
