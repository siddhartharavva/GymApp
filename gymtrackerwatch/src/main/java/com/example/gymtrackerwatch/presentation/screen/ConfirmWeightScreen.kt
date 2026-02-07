package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
import com.example.gymtrackerwatch.presentation.util.formatHistorySets
import com.example.gymtrackerwatch.presentation.util.formatWeight


@Composable
fun ConfirmWeightScreen(
    vm: ActiveWorkoutViewModel
) {
    val step = 2.5f
    val weight = vm.pendingWeight
    val lastHistory = vm.currentExercise().history.firstOrNull()
    val focusRequester = remember { FocusRequester() }
    var rotaryAccum by remember { mutableStateOf(0f) }
    // Galaxy Watch bezel events are tiny (~3 degrees), so use a small threshold.
    val rotaryStepPx = 1f
    val view = LocalView.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        view.requestFocus()
    }

    DisposableEffect(view) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        val listener = View.OnGenericMotionListener { _, event ->
            val isRotary =
                event.action == MotionEvent.ACTION_SCROLL &&
                    event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
            if (!isRotary) return@OnGenericMotionListener false

            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
            rotaryAccum += delta
            val steps = (rotaryAccum / rotaryStepPx).toInt()
            Log.d("RotaryWeight", "delta=$delta accum=$rotaryAccum steps=$steps")
            if (steps != 0) {
                rotaryAccum -= steps * rotaryStepPx
                vm.updatePendingWeight(vm.pendingWeight + (steps * step))
                Log.d("RotaryWeight", "weight=${vm.pendingWeight}")
            }
            true
        }
        view.setOnGenericMotionListener(listener)
        onDispose {
            view.setOnGenericMotionListener(null)
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "WEIGHT",
                    style = MaterialTheme.typography.caption1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.updatePendingWeight(weight - step) },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("-")
                    }

                    Text(
                        text = "${formatWeight(weight)} kg",
                        style = MaterialTheme.typography.display1
                    )

                    Button(
                        onClick = { vm.updatePendingWeight(weight + step) },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("+")
                    }
                }

                if (lastHistory != null) {
                    Text(
                        text = "Last: ${formatHistorySets(lastHistory.sets)}",
                        style = MaterialTheme.typography.caption1
                    )
                }

                Button(
                    onClick = {
                        vm.confirmSet(vm.pendingReps, weight)
                        vm.goToRest() // 🔥 THIS is the missing piece
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("CONFIRM")
                }
            }
        }
    }
}
