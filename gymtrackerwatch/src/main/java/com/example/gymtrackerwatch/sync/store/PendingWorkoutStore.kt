package com.example.gymtrackerwatch.sync.store

import android.content.Context
import com.example.gymtrackerwatch.domain.model.CompletedWorkout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object PendingWorkoutStore {
    private const val PREFS = "pending_workout"
    private const val KEY_JSON = "completed_json"

    fun save(context: Context, workout: CompletedWorkout) {
        val json = Json.encodeToString(workout)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, json)
            .apply()
    }

    fun load(context: Context): CompletedWorkout? {
        val json =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_JSON, null)
                ?: return null
        return runCatching { Json.decodeFromString<CompletedWorkout>(json) }
            .getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JSON)
            .apply()
    }

    fun hasPending(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_JSON)
    }
}
