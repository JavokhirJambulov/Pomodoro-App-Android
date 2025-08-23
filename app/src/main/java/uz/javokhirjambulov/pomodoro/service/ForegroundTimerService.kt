package uz.javokhirjambulov.pomodoro.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.format.DateUtils
import kotlinx.coroutines.*
import uz.javokhirjambulov.pomodoro.R
import uz.javokhirjambulov.pomodoro.commons.*
import kotlin.math.ln

class ForegroundTimerService : Service() {

    private lateinit var notificationManager: MyNotificationManager
    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val oneMinute = 60
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

    fun startTimer() {
        if (currentTimer == TimerType.SESSION_NOT_STARTED_YET || currentTimer == TimerType.SESSION_COMPLETED) {
            currentTimer = TimerType.POMODORO
            sessionCounter = 0L
        }
        when (currentTimer) {

            TimerType.POMODORO -> {
                startTime = pomodoroTime * oneMinute
                timeInSeconds = startTime
            }
            TimerType.BREAK -> {
                startTime = breakTime * oneMinute
                timeInSeconds = startTime
            }
            TimerType.LONG_BREAK -> {
                startTime = longBreakTime * oneMinute
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
                handleTimerComplete(TimerType.POMODORO)
                if (sessionCounter >= sessions) {
                    currentTimer = TimerType.LONG_BREAK
                    startTime = longBreakTime * oneMinute
                } else {
                    currentTimer = TimerType.BREAK
                    startTime = breakTime * oneMinute
                }
                timeInSeconds = startTime
                startTimerJob()
            }
            TimerType.BREAK -> {
                handleTimerComplete(TimerType.BREAK)
                currentTimer = TimerType.POMODORO
                startTime = pomodoroTime * oneMinute
                timeInSeconds = startTime
                startTimerJob()
            }
            TimerType.LONG_BREAK -> {
                handleTimerComplete(TimerType.LONG_BREAK)
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

    private fun handleTimerComplete(timerType: TimerType) {
        when (timerType) {
            TimerType.POMODORO -> {
                vibrate(BuzzType.POMODORO_OVER)
                playSound(MediaType.POMODORO_OVER)
            }
            TimerType.BREAK -> {
                vibrate(BuzzType.BREAK_OVER)
                playSound(MediaType.BREAK_OVER)
            }
            TimerType.LONG_BREAK -> {
                vibrate(BuzzType.LONG_BREAK_OVER)
                playSound(MediaType.LONG_BREAK_OVER)
            }
            else -> {}
        }
    }

    private fun playSound(mediaType: MediaType) {
        try {
            val MAX_VOLUME = 100
            val soundVolume = 10
            val volume: Double = (1 - (ln((MAX_VOLUME - soundVolume).toDouble()) / ln(MAX_VOLUME.toDouble())))

            when (mediaType) {
                MediaType.POMODORO_OVER -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.mixkit_phone_ring_bell)
                    mediaPlayer?.apply {
                        setVolume(volume.toFloat(), volume.toFloat())
                        start()
                        // Release MediaPlayer after playback
                        setOnCompletionListener { mp ->
                            mp.release()
                        }
                    }
                }
                MediaType.BREAK_OVER -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.mixkit_achievement_bell)
                    mediaPlayer?.apply {
                        setVolume(volume.toFloat(), volume.toFloat())
                        start()
                        setOnCompletionListener { mp ->
                            mp.release()
                        }
                    }
                }
                MediaType.LONG_BREAK_OVER -> {
                    val mediaPlayer = MediaPlayer.create(this, R.raw.mixkit_bell_of_promise)
                    mediaPlayer?.apply {
                        setVolume(volume.toFloat(), volume.toFloat())
                        start()
                        setOnCompletionListener { mp ->
                            mp.release()
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(buzzType: BuzzType) {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(buzzType.pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(buzzType.pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        cancelTimerJob()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.clearNotification()
        super.onDestroy()
    }
}