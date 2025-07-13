
package uz.javokhirjambulov.pomodoro.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import uz.javokhirjambulov.pomodoro.commons.MyNotificationManager
import uz.javokhirjambulov.pomodoro.commons.TimerStatus
import uz.javokhirjambulov.pomodoro.commons.TimerType
import uz.javokhirjambulov.pomodoro.commons.Constants

class ForegroundTimerService : Service() {

    private lateinit var notificationManager: MyNotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = MyNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = intent?.getSerializableExtra("timerType") as? TimerType ?: TimerType.POMODORO
        val status = intent?.getSerializableExtra("timerStatus") as? TimerStatus ?: TimerStatus.IN_PROGRESS

        val notification = notificationManager
            .getNotification("Pomodoro running...", status, type)
            .build()

        startForeground(Constants.POMODORO_NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        stopForeground(true) // removes the notification
        super.onDestroy()
    }
}
