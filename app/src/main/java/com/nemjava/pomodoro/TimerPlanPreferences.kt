package com.nemjava.pomodoro

import android.content.Context
import androidx.core.content.edit

object TimerPlanPreferences {
    private const val PREFS_NAME = "timer_plan_preferences"
    private const val PREF_POMODORO_MINUTES = "pomodoro_minutes"
    private const val PREF_BREAK_MINUTES = "break_minutes"
    private const val PREF_LONG_BREAK_MINUTES = "long_break_minutes"
    private const val PREF_SESSIONS = "sessions"
    val defaultSettings = TimerPlanSettings(
        pomodoroMinutes = 20L,
        breakMinutes = 3L,
        longBreakMinutes = 5L,
        sessions = 2L
    )

    fun load(context: Context): TimerPlanSettings = load(context, defaultSettings)

    fun load(context: Context, defaultSettings: TimerPlanSettings): TimerPlanSettings {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TimerPlanSettings(
            pomodoroMinutes = preferences.getLong(
                PREF_POMODORO_MINUTES,
                defaultSettings.pomodoroMinutes
            ),
            breakMinutes = preferences.getLong(
                PREF_BREAK_MINUTES,
                defaultSettings.breakMinutes
            ),
            longBreakMinutes = preferences.getLong(
                PREF_LONG_BREAK_MINUTES,
                defaultSettings.longBreakMinutes
            ),
            sessions = preferences.getLong(
                PREF_SESSIONS,
                defaultSettings.sessions
            )
        )
    }

    fun save(context: Context, settings: TimerPlanSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(PREF_POMODORO_MINUTES, settings.pomodoroMinutes)
            putLong(PREF_BREAK_MINUTES, settings.breakMinutes)
            putLong(PREF_LONG_BREAK_MINUTES, settings.longBreakMinutes)
            putLong(PREF_SESSIONS, settings.sessions)
        }
    }
}
