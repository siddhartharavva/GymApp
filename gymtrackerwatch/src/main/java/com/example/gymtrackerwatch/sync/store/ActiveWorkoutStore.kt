package com.example.gymtrackerwatch.sync.store

import android.content.Context
import com.example.gymtrackerwatch.domain.model.ActiveWorkout
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ActiveWorkoutState(
    val workout: ActiveWorkout,
    val uiState: String,
    val restEndElapsedMs: Long,
    val started: Boolean
)

object ActiveWorkoutStore {
    private const val PREFS = "active_workout"
    private const val KEY_JSON = "active_state_json"

    private val json = Json { encodeDefaults = true }

    fun save(context: Context, state: ActiveWorkoutState) {
        val payload = json.encodeToString(state)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, payload)
            .apply()
    }

    fun load(context: Context): ActiveWorkoutState? {
        val payload =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_JSON, null)
                ?: return null
        return runCatching { json.decodeFromString<ActiveWorkoutState>(payload) }
            .getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JSON)
            .apply()
    }
}
