package com.example.gymtrackerwatch.sync.receiver

import android.util.Log
import com.example.gymtrackerwatch.sync.WearPaths
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class WorkoutReceiverService : WearableListenerService() {

    private val TAG = "WorkoutReceiver"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔥 Service CREATED")
    }

    override fun onDestroy() {
        Log.d(TAG, "💀 Service DESTROYED")
        super.onDestroy()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach

            val item = event.dataItem
            val path = item.uri.path

            if (path != WearPaths.START_WORKOUT) {
                Log.d(TAG, "Ignoring unrelated path: $path")
                return@forEach
            }

            Log.d(TAG, "📥 Workout DataItem received")

            val dataMap = DataMapItem.fromDataItem(item).dataMap
            val json = dataMap.getString(WearPaths.KEY_WORKOUT_JSON)

            if (json.isNullOrBlank()) {
                Log.e(TAG, "❌ Missing workout JSON")
                return@forEach
            }

            try {
                val template =
                    Json.decodeFromString<WorkoutTemplateDto>(json)

                IncomingWorkoutStore.store(template)

                Log.d(TAG, "✅ Workout stored successfully")
            } catch (e: SerializationException) {
                Log.e(TAG, "❌ Failed to deserialize workout", e)
            }
        }
    }
}