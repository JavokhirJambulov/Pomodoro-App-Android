package com.nemjava.pomodoro.commons

import androidx.annotation.RawRes
import androidx.annotation.StringRes

data class TimerAnimationOption(
    val key: String,
    @StringRes val titleResId: Int,
    @RawRes val rawResId: Int
)
