package com.example.gymtrackerphone.sync.sender

import android.content.Context
import com.example.gymtrackerphone.sync.WorkoutTransfer
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutSender {

    fun sendWorkout(context: Context, template: WorkoutTemplateDto) {
        val json = Json.encodeToString(template)



        Wearable.getNodeClient(context)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        WorkoutTransfer.PATH_START_WORKOUT,
                        json.toByteArray()
                    )
                }
            }
    }
}