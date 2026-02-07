package com.example.gymtrackerphone.sync.receiver

import android.util.Log
import com.example.gymtrackerphone.GymTrackerApp
import com.example.gymtrackerphone.data.repository.WorkoutRepository
import com.example.gymtrackerphone.sync.WorkoutTransfer
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem

import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WorkoutResultReceiverService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy {
        val app = applicationContext as GymTrackerApp
        WorkoutRepository(app.database)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

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

                scope.launch {
                    try {
                        repository.addCompletedWorkout(workout)
                        Log.e("WorkoutReceiver", "✅ Workout stored")

                        // ✅ SEND ACK BACK AS DATA (only after DB success)
                        val ack =
                            PutDataMapRequest.create(WorkoutTransfer.COMPLETED_WORKOUT_ACK)
                                .apply {
                                    dataMap.putLong("timestamp", System.currentTimeMillis())
                                }
                                .asPutDataRequest()
                                .setUrgent()

                        Wearable.getDataClient(this@WorkoutResultReceiverService)
                            .putDataItem(ack)
                    } catch (e: Exception) {
                        Log.e("WorkoutReceiver", "❌ Failed to store workout", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("WorkoutReceiver", "❌ Failed", e)
            }
        }
    }
}
