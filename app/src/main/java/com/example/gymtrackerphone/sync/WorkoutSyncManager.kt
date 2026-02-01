package com.example.gymtrackerphone.sync

import android.content.Context
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerphone.sync.sender.WorkoutSender

class WorkoutSyncManager(
    private val context: Context
) {
    fun sendWorkout(dto: WorkoutTemplateDto) {
        WorkoutSender.sendWorkout(context, dto)
    }
}