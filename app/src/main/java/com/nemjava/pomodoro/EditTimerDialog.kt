package com.nemjava.pomodoro

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

data class TimerPlanSettings(
    val pomodoroMinutes: Long,
    val breakMinutes: Long,
    val longBreakMinutes: Long,
    val sessions: Long
)

object EditTimerDialog {
    private const val MIN_POMODORO_MINUTES = 1L
    private const val MAX_POMODORO_MINUTES = 60L
    private const val STEP_POMODORO_MINUTES = 5L

    private const val MIN_BREAK_MINUTES = 1L
    private const val MAX_BREAK_MINUTES = 15L
    private const val STEP_BREAK_MINUTES = 3L

    private const val MIN_LONG_BREAK_MINUTES = 1L
    private const val MAX_LONG_BREAK_MINUTES = 30L
    private const val STEP_LONG_BREAK_MINUTES = 5L

    private const val MIN_SESSIONS = 2L
    private const val MAX_SESSIONS = 10L
    private const val STEP_SESSIONS = 1L

    fun show(
        activity: AppCompatActivity,
        initialSettings: TimerPlanSettings,
        onSave: (TimerPlanSettings) -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_timer, null)
        var pomodoroMinutes = initialSettings.pomodoroMinutes
        var breakMinutes = initialSettings.breakMinutes
        var longBreakMinutes = initialSettings.longBreakMinutes
        var sessions = initialSettings.sessions

        bindStepper(
            root = dialogView,
            minusButtonId = R.id.focusMinusButton,
            plusButtonId = R.id.focusPlusButton,
            valueTextId = R.id.focusValue,
            initialValue = pomodoroMinutes,
            minValue = MIN_POMODORO_MINUTES,
            maxValue = MAX_POMODORO_MINUTES,
            step = STEP_POMODORO_MINUTES
        ) { pomodoroMinutes = it }

        bindStepper(
            root = dialogView,
            minusButtonId = R.id.breakMinusButton,
            plusButtonId = R.id.breakPlusButton,
            valueTextId = R.id.breakValue,
            initialValue = breakMinutes,
            minValue = MIN_BREAK_MINUTES,
            maxValue = MAX_BREAK_MINUTES,
            step = STEP_BREAK_MINUTES
        ) { breakMinutes = it }

        bindStepper(
            root = dialogView,
            minusButtonId = R.id.longBreakMinusButton,
            plusButtonId = R.id.longBreakPlusButton,
            valueTextId = R.id.longBreakValue,
            initialValue = longBreakMinutes,
            minValue = MIN_LONG_BREAK_MINUTES,
            maxValue = MAX_LONG_BREAK_MINUTES,
            step = STEP_LONG_BREAK_MINUTES
        ) { longBreakMinutes = it }

        bindStepper(
            root = dialogView,
            minusButtonId = R.id.sessionsMinusButton,
            plusButtonId = R.id.sessionsPlusButton,
            valueTextId = R.id.sessionsValue,
            initialValue = sessions,
            minValue = MIN_SESSIONS,
            maxValue = MAX_SESSIONS,
            step = STEP_SESSIONS
        ) { sessions = it }

        val dialog = AlertDialog.Builder(activity).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            onSave(
                TimerPlanSettings(
                    pomodoroMinutes = pomodoroMinutes,
                    breakMinutes = breakMinutes,
                    longBreakMinutes = longBreakMinutes,
                    sessions = sessions
                )
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun bindStepper(
        root: View,
        minusButtonId: Int,
        plusButtonId: Int,
        valueTextId: Int,
        initialValue: Long,
        minValue: Long,
        maxValue: Long,
        step: Long,
        onValueChanged: (Long) -> Unit
    ) {
        val minusButton = root.findViewById<Button>(minusButtonId)
        val plusButton = root.findViewById<Button>(plusButtonId)
        val valueText = root.findViewById<TextView>(valueTextId)

        var value = initialValue.coerceIn(minValue, maxValue)

        fun render() {
            valueText.text = value.toString()
            val canDecrease = value > minValue
            val canIncrease = value < maxValue
            minusButton.isEnabled = canDecrease
            plusButton.isEnabled = canIncrease
            minusButton.alpha = if (canDecrease) 1f else 0.4f
            plusButton.alpha = if (canIncrease) 1f else 0.4f
        }

        onValueChanged(value)
        render()

        minusButton.setOnClickListener {
            value = (value - step).coerceAtLeast(minValue)
            onValueChanged(value)
            render()
        }
        plusButton.setOnClickListener {
            value = (value + step).coerceAtMost(maxValue)
            onValueChanged(value)
            render()
        }
    }
}
