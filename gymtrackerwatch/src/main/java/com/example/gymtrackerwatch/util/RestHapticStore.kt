package com.example.gymtrackerwatch.util

import android.content.Context

object RestHapticStore {
    private const val PREFS = "rest_haptic"
    private const val KEY_TOKEN = "last_token"

    fun wasFired(context: Context, token: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null) == token
    }

    fun markFired(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }
}
