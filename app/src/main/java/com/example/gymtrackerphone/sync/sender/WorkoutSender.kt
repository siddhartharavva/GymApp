package com.example.gymtrackerphone.sync.sender

import android.content.Context
import android.util.Log
import com.example.gymtrackerphone.sync.WorkoutTransfer
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutSender {

    fun sendWorkout(context: Context, template: WorkoutTemplateDto) {
        val json = Json.encodeToString(template)

        val request = PutDataMapRequest.create(WorkoutTransfer.PATH_START_WORKOUT).apply {
            dataMap.putString(WorkoutTransfer.KEY_WORKOUT_JSON, json)
            dataMap.putLong("timestamp", System.currentTimeMillis()) // 🔑 REQUIRED
        }.asPutDataRequest().apply {
            setUrgent()
        }
        Wearable.getCapabilityClient(context)
            .getCapability("phone_receiver", CapabilityClient.FILTER_REACHABLE)

        Wearable.getDataClient(context)
            .putDataItem(request)
            .addOnSuccessListener {
                Log.d("WorkoutSender", "✅ Data sent")
            }
            .addOnFailureListener {
                Log.e("WorkoutSender", "❌ Send failed", it)
            }
    }
}