package com.example.gymtrackerphone.sync.sender

import android.content.Context
import android.util.Log
import com.example.gymtrackerphone.sync.WorkoutTransfer
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutSender {

    private const val TAG = "WorkoutSender"

    fun sendWorkout(context: Context, template: WorkoutTemplateDto) {
        val json = Json.encodeToString(template)

        val request = PutDataMapRequest
            .create(WorkoutTransfer.PATH_START_WORKOUT)
            .apply {
                dataMap.putString(WorkoutTransfer.KEY_WORKOUT_JSON, json)
                dataMap.putLong(
                    WorkoutTransfer.KEY_UPDATED_AT,
                    System.currentTimeMillis()
                )
            }
            .asPutDataRequest()
            .setUrgent()

        Wearable.getDataClient(context)
            .putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Workout template synced to watch")
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ Failed to sync workout template", it)
            }
    }
}