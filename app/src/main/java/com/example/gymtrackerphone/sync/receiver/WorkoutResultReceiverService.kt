package com.example.gymtrackerphone.sync.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gymtrackerphone.GymTrackerApp
import com.example.gymtrackerphone.MainActivity
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
                        val inserted = repository.addCompletedWorkout(workout)
                        if (inserted) {
                            Log.e("WorkoutReceiver", "✅ Workout stored")
                            showWorkoutNotification(workout)
                        } else {
                            Log.e("WorkoutReceiver", "ℹ️ Duplicate workout ignored")
                        }

                        sendAckMessage()
                    } catch (e: Exception) {
                        Log.e("WorkoutReceiver", "❌ Failed to store workout", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("WorkoutReceiver", "❌ Failed", e)
            }
        }
    }

    private fun sendAckMessage() {
        Wearable.getNodeClient(this)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(this)
                        .sendMessage(
                            node.id,
                            WorkoutTransfer.COMPLETED_WORKOUT_ACK,
                            ByteArray(0)
                        )
                        .addOnFailureListener {
                            Log.e("WorkoutReceiver", "❌ ACK message failed", it)
                        }

                    val ackRequest =
                        PutDataMapRequest.create(WorkoutTransfer.COMPLETED_WORKOUT_ACK)
                            .apply {
                                dataMap.putLong(WorkoutTransfer.KEY_UPDATED_AT, System.currentTimeMillis())
                                dataMap.putString("node_id", node.id)
                            }
                            .asPutDataRequest()
                            .setUrgent()

                    Wearable.getDataClient(this)
                        .putDataItem(ackRequest)
                        .addOnFailureListener {
                            Log.e("WorkoutReceiver", "❌ ACK data item failed", it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("WorkoutReceiver", "❌ Failed to fetch nodes for ACK", it)
            }
    }

    private fun showWorkoutNotification(workout: CompletedWorkoutDto) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, GymTrackerApp.CHANNEL_WORKOUTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Workout received")
            .setContentText("Saved ${workout.name}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(workout.completedAtEpochMs.toInt(), notification)
    }
}
