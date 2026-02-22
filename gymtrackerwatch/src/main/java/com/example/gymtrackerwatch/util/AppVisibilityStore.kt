package com.example.gymtrackerwatch.util

import android.content.Context

object AppVisibilityStore {
    private const val PREFS = "app_visibility"
    private const val KEY_VISIBLE = "visible"

    fun setVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VISIBLE, visible)
            .apply()
    }

    fun isVisible(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VISIBLE, false)
    }
}
