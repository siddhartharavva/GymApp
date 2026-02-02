package com.example.gymtrackerwatch.sync.sender

import android.content.Context
import android.util.Log
import com.example.gymtrackerwatch.domain.model.CompletedWorkout
import com.example.gymtrackerwatch.sync.WearPaths
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutResultSender {

    private const val TAG = "WorkoutResultSender"
    private const val PHONE_CAPABILITY = "gymtracker_phone"

    fun send(context: Context, workout: CompletedWorkout) {
        val json = Json.encodeToString(workout)

        val putDataRequest: com.google.android.gms.wearable.PutDataRequest =
            PutDataMapRequest.create(WearPaths.SEND_COMPLETED_WORKOUT)
                .apply {
                    dataMap.putString("completed_workout_json", json)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

        // 1️⃣ Debug: check connected nodes
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.e(TAG, "Connected nodes = ${nodes.size}")
                nodes.forEach { Log.e(TAG, "Node: ${it.displayName} (${it.id})") }
            }

        // 2️⃣ Find phone via capability and SEND
        Wearable.getCapabilityClient(context)
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capability ->

                if (capability.nodes.isEmpty()) {
                    Log.e(TAG, "❌ No phone with capability $PHONE_CAPABILITY reachable")
                    return@addOnSuccessListener
                }

                capability.nodes.forEach { node ->
                    Log.e(TAG, "📤 Sending workout to phone node ${node.id}")

                    Wearable.getDataClient(context)
                        .putDataItem(putDataRequest)

                        .addOnSuccessListener {
                            Log.e(TAG, "✅ Completed workout sent to phone")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "❌ Failed to send workout", it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ Capability lookup failed", it)
            }
    }
}