package com.example.gymtrackerwatch.sync.store

import android.content.Context
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object IncomingWorkoutStore {
    private const val PREFS = "incoming_workout"
    private const val KEY_JSON = "template_json"

    private val _hasWorkout = MutableStateFlow(false)
    val hasWorkout: StateFlow<Boolean> = _hasWorkout

    private var workout: WorkoutTemplateDto? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (workout != null) return

        val json = appContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getString(KEY_JSON, null)
            ?: return

        workout = runCatching { Json.decodeFromString<WorkoutTemplateDto>(json) }
            .getOrNull()
        _hasWorkout.value = workout != null
    }

    fun store(context: Context, template: WorkoutTemplateDto) {
        appContext = context.applicationContext
        workout = template
        _hasWorkout.value = true

        val json = Json.encodeToString(template)
        appContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_JSON, json)
            ?.apply()
    }

    fun consume(): WorkoutTemplateDto? {
        if (workout == null) {
            init(appContext ?: return null)
        }

        val w = workout
        workout = null
        _hasWorkout.value = false

        appContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_JSON)
            ?.apply()

        return w
    }
}
