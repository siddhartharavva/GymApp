package com.example.gymtrackerwatch.util

object AppVisibilityStore {
    @Volatile
    private var visible: Boolean = false

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    fun isVisible(): Boolean {
        return visible
    }
}
