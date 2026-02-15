package com.nemjava.pomodoro.commons


object Constants {
    const val POMODORO_NOTIFICATION_ID = 42

    val BUTTON_STOP = "button_stop"
    val BUTTON_START = "button_start"
    val BUTTON_PAUSE = "button_pause"


    // Intent Action
    val BUTTON_ACTION = "button_action"

    // Source code
    const val sourceCodeURL: String = "https://github.com/JavokhirJambulov/Pomodoro-App-Android"
    const val feedbackURL = "mailto:jambulovnemat01@gmail.com?subject=Feedback about %s"

    //Used to vibrate
    val POMODORO_OVER_BUZZ_PATTERN = longArrayOf(200, 100, 200, 100, 200, 100)
    val BREAK_OVER_BUZZ_PATTERN = longArrayOf(300, 200, 300, 200)
    val LONG_BREAK_OVER_BUZZ_PATTERN = longArrayOf(0, 500, 0, 500)
    val NO_BUZZ_PATTERN = longArrayOf(0)

    //used to Open the Intro Activity first
    const val FIRST_RUN = "pref_first_run"
}