package com.example.gymtrackerwatch.sync.receiver

import android.util.Log
import com.example.gymtrackerwatch.sync.WearPaths
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json
class WorkoutReceiverService : WearableListenerService() {
    override fun onCreate() {
        super.onCreate()
        Log.e("WorkoutReceiver", "🔥 Service CREATED")
    }

    override fun onDestroy() {
        Log.e("WorkoutReceiver", "💀 Service DESTROYED")
    }
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val item = event.dataItem

                if (item.uri.path == WearPaths.START_WORKOUT) {
                    Log.e("WorkoutReceiver", "🔥 DATA RECEIVED")

                    val map = DataMapItem.fromDataItem(item).dataMap
                    val json = map.getString("workout_json") ?: return

                    val template =
                        Json.decodeFromString<WorkoutTemplateDto>(json)

                    IncomingWorkoutStore.store(template)

                    Log.e("WorkoutReceiver", "✅ Workout stored")
                }
            }
        }
    }
}