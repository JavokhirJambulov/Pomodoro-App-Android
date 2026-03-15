package com.nemjava.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nemjava.pomodoro.commons.Constants
import com.nemjava.pomodoro.service.ForegroundTimerService

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        val action = intent?.getStringExtra(Constants.BUTTON_ACTION)
            ?: throw AssertionError("Provide Button Action")

        val serviceIntent = Intent(context, ForegroundTimerService::class.java)

        when (action) {
            Constants.BUTTON_STOP -> {
                serviceIntent.putExtra(ForegroundTimerService.EXTRA_ACTION, ForegroundTimerService.ACTION_STOP)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Constants.BUTTON_START -> {
                serviceIntent.putExtra(ForegroundTimerService.EXTRA_ACTION, ForegroundTimerService.ACTION_RESUME)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Constants.BUTTON_PAUSE -> {
                serviceIntent.putExtra(ForegroundTimerService.EXTRA_ACTION, ForegroundTimerService.ACTION_PAUSE)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
