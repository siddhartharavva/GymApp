package com.example.gymtrackerphone.sync.receiver

import android.util.Log
import com.example.gymtrackerphone.sync.WorkoutTransfer
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json

class WorkoutResultReceiverService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach

            val item = event.dataItem
            if (item.uri.path != WorkoutTransfer.SEND_COMPLETED_WORKOUT) return@forEach

            try {
                val map = DataMapItem.fromDataItem(item).dataMap
                val json = map.getString("completed_workout_json") ?: return@forEach

                val workout =
                    Json.decodeFromString<CompletedWorkoutDto>(json)

                // ✅ Save to DB here
                Log.e("WorkoutReceiver", "✅ Workout stored")

                // ✅ SEND ACK BACK AS DATA
                val ack = PutDataMapRequest.create(WorkoutTransfer.COMPLETED_WORKOUT_ACK)
                    .apply {
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }
                    .asPutDataRequest()
                    .setUrgent()

                Wearable.getDataClient(this).putDataItem(ack)

            } catch (e: Exception) {
                Log.e("WorkoutReceiver", "❌ Failed", e)
            }
        }
    }
}