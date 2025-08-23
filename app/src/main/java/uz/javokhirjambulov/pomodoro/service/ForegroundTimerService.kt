package uz.javokhirjambulov.pomodoro.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.text.format.DateUtils
import kotlinx.coroutines.*
import uz.javokhirjambulov.pomodoro.commons.*

class ForegroundTimerService : Service() {

    private lateinit var notificationManager: MyNotificationManager
    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Timer state
    private var timeInSeconds = 0L
    private var startTime = 0L
    private var currentTimer = TimerType.SESSION_NOT_STARTED_YET
    private var timerStatus = TimerStatus.STOPPED
    private var sessionCounter = 0L

    // Timer settings
    private var pomodoroTime = 20L // minutes
    private var breakTime = 3L // minutes
    private var longBreakTime = 5L // minutes
    private var sessions = 2L

    // Callbacks
    private var onTimerUpdate: ((Long, TimerType, TimerStatus) -> Unit)? = null
    private var onTimerComplete: ((TimerType) -> Unit)? = null

    inner class TimerBinder : Binder() {
        fun getService(): ForegroundTimerService = this@ForegroundTimerService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = MyNotificationManager(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        when (action) {
            "START" -> startTimer()
            "PAUSE" -> pauseTimer()
            "RESUME" -> resumeTimer()
            "STOP" -> stopTimer()
        }
        updateNotification()
        return START_STICKY
    }

    fun setTimerSettings(pomodoro: Long, shortBreak: Long, longBreak: Long, sessionCount: Long) {
        pomodoroTime = pomodoro
        breakTime = shortBreak
        longBreakTime = longBreak
        sessions = sessionCount
    }

    fun setTimerUpdateCallback(callback: (Long, TimerType, TimerStatus) -> Unit) {
        onTimerUpdate = callback
    }

    fun setTimerCompleteCallback(callback: (TimerType) -> Unit) {
        onTimerComplete = callback
    }

    fun startTimer() {
        if (currentTimer == TimerType.SESSION_NOT_STARTED_YET || currentTimer == TimerType.SESSION_COMPLETED) {
            currentTimer = TimerType.POMODORO
            sessionCounter = 0L
        }
        when (currentTimer) {

            TimerType.POMODORO -> {
                startTime = pomodoroTime * 60
                timeInSeconds = startTime
            }
            TimerType.BREAK -> {
                startTime = breakTime * 60
                timeInSeconds = startTime
            }
            TimerType.LONG_BREAK -> {
                startTime = longBreakTime * 60
                timeInSeconds = startTime
            }
            else -> return
        }

        timerStatus = TimerStatus.IN_PROGRESS
        startTimerJob()
        updateNotification()
        onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
    }

    fun pauseTimer() {
        timerStatus = TimerStatus.PAUSED
        cancelTimerJob()
        updateNotification()
        onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
    }

    fun resumeTimer() {
        timerStatus = TimerStatus.IN_PROGRESS
        startTimerJob()
        updateNotification()
        onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
    }

    fun stopTimer() {
        cancelTimerJob()
        timeInSeconds = 0L
        timerStatus = TimerStatus.STOPPED
        currentTimer = TimerType.SESSION_NOT_STARTED_YET
        sessionCounter = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clearNotification()
        onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
        stopSelf()
    }

    private fun startTimerJob() {
        timerJob = serviceScope.launch {
            while (timeInSeconds > 0 && timerStatus == TimerStatus.IN_PROGRESS) {
                delay(1000)
                timeInSeconds -= 1

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    updateNotification()
                    onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
                }
            }

            if (timeInSeconds <= 0 && timerStatus == TimerStatus.IN_PROGRESS) {
                withContext(Dispatchers.Main) {
                    checkTimer()
                }
            }
        }
    }

    private fun checkTimer() {
        when (currentTimer) {
            TimerType.POMODORO -> {
                sessionCounter += 1
                onTimerComplete?.invoke(TimerType.POMODORO)

                if (sessionCounter >= sessions) {
                    currentTimer = TimerType.LONG_BREAK
                    startTime = longBreakTime * 60
                } else {
                    currentTimer = TimerType.BREAK
                    startTime = breakTime * 60
                }
                timeInSeconds = startTime
                startTimerJob()
            }
            TimerType.BREAK -> {
                onTimerComplete?.invoke(TimerType.BREAK)
                currentTimer = TimerType.POMODORO
                startTime = pomodoroTime * 60
                timeInSeconds = startTime
                startTimerJob()
            }
            TimerType.LONG_BREAK -> {
                onTimerComplete?.invoke(TimerType.LONG_BREAK)
                currentTimer = TimerType.SESSION_COMPLETED
                timerStatus = TimerStatus.STOPPED
                sessionCounter = 0L
                timeInSeconds = 0L
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager.clearNotification()
                onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
                stopSelf()
            }
            else -> {
                stopTimer()
            }
        }

        updateNotification()
        onTimerUpdate?.invoke(timeInSeconds, currentTimer, timerStatus)
    }

    private fun cancelTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification() {
        if (currentTimer != TimerType.SESSION_NOT_STARTED_YET) {
            val timeLeftString = DateUtils.formatElapsedTime(timeInSeconds)
            val notification = notificationManager
                .getNotification(timeLeftString, timerStatus, currentTimer)
                .build()

            startForeground(Constants.POMODORO_NOTIFICATION_ID, notification)
        }
    }

    fun getCurrentState() = Triple(timeInSeconds, currentTimer, timerStatus)

    override fun onDestroy() {
        cancelTimerJob()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clearNotification()
        super.onDestroy()
    }
}