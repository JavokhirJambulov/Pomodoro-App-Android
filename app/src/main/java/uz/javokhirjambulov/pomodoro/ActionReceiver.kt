package uz.javokhirjambulov.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import uz.javokhirjambulov.pomodoro.commons.Constants
import uz.javokhirjambulov.pomodoro.service.ForegroundTimerService

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        val action = intent?.getStringExtra(Constants.BUTTON_ACTION)
            ?: throw AssertionError("Provide Button Action")

        val serviceIntent = Intent(context, ForegroundTimerService::class.java)

        when (action) {
            Constants.BUTTON_STOP -> {
                serviceIntent.putExtra("action", "STOP")
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Constants.BUTTON_START -> {
                serviceIntent.putExtra("action", "RESUME")
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Constants.BUTTON_PAUSE -> {
                serviceIntent.putExtra("action", "PAUSE")
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}