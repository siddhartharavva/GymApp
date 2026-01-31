package com.example.gymtrackerphone.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
fun rememberKeyboardOpen(): Boolean {
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    return imeInsets.getBottom(density) > 0
}