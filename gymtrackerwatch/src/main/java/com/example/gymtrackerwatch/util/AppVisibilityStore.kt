package com.example.gymtrackerwatch.util

import android.content.Context

object AppVisibilityStore {
    @Volatile
    private var visible: Boolean = false

    fun setVisible(context: Context, visible: Boolean) {
        this.visible = visible
    }

    fun isVisible(context: Context): Boolean {
        return visible
    }
}
