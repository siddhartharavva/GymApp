package com.example.gymtrackerwatch.sync.sender

import android.content.Context
import android.util.Log
import com.example.gymtrackerwatch.domain.model.CompletedWorkout
import com.example.gymtrackerwatch.sync.WearPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutResultSender {

    fun send(context: Context, workout: CompletedWorkout) {
        val json = Json.encodeToString(workout)

        val request = PutDataMapRequest.create(WearPaths.SEND_COMPLETED_WORKOUT)
            .apply {
                dataMap.putString("completed_workout_json", json)
                dataMap.putLong("timestamp", System.currentTimeMillis()) // forces update
            }
            .asPutDataRequest()
            .setUrgent()

        Wearable.getDataClient(context)
            .putDataItem(request)
            .addOnSuccessListener {
                Log.e("WorkoutResultSender", "✅ Completed workout sent to phone")
            }
            .addOnFailureListener {
                Log.e("WorkoutResultSender", "❌ Failed to send workout", it)
            }
    }
}