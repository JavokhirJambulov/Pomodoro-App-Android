package com.nemjava.pomodoro

import android.content.*
import android.content.pm.PackageManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.airbnb.lottie.LottieCompositionFactory
import com.nemjava.pomodoro.commons.TimerAnimationCatalog
import com.nemjava.pomodoro.commons.TimerStatus
import com.nemjava.pomodoro.commons.TimerType
import com.nemjava.pomodoro.databinding.MainScreenFragmentBinding
import com.nemjava.pomodoro.screen.AnimationSelectionActivity
import com.nemjava.pomodoro.service.ForegroundTimerService
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import androidx.core.view.isInvisible

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenFragmentBinding
    private var currentTimerStatus = TimerStatus.STOPPED
    private var currentSessionIndex = 1L
    private var timerService: ForegroundTimerService? = null
    private var serviceBound = false
    private var loadedTimerAnimationKey: String? = null
    private var isTimerAnimationEnabled = true

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ForegroundTimerService.TimerBinder
            timerService = binder.getService()
            serviceBound = true

            // Set up callbacks
            timerService?.setTimerUpdateCallback { timeInSeconds, timerType, timerStatus, sessionIndex, totalSessions ->
                runOnUiThread {
                    updateUI(timeInSeconds, timerType, timerStatus, sessionIndex, totalSessions)
                }
            }

            // Sync current state
            timerService?.getCurrentState()?.let { (time, type, status) ->
                val (sessionIndex, totalSessions) = timerService?.getSessionProgress()
                    ?: Pair(1L, currentTimerPlanSettings().sessions)
                updateUI(time, type, status, sessionIndex, totalSessions)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen_fragment)
        binding.lifecycleOwner = this
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadAnimations()
        bindTimerService()
        setupButtons()
        syncPlanUiWithCurrentState()
    }



    private fun bindTimerService() {
        val intent = Intent(this, ForegroundTimerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun loadAnimations() {
        loadConfettiAnimation()
        loadSelectedTimerAnimation(true)
    }

    private fun loadConfettiAnimation() {
        LottieCompositionFactory.fromRawRes(this, R.raw.confetti).addListener {
            binding.confettiAnimation.setComposition(it)
            binding.confettiAnimation.visibility = View.INVISIBLE
            binding.confettiAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.confettiAnimation.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {
                    binding.confettiAnimation.visibility = View.GONE
                }
            })
        }
    }

    private fun loadSelectedTimerAnimation(force: Boolean = false) {
        val selectedAnimation = TimerAnimationCatalog.getSelectedOption(this)
        if (!force && loadedTimerAnimationKey == selectedAnimation.key) return

        loadedTimerAnimationKey = selectedAnimation.key
        isTimerAnimationEnabled = selectedAnimation.hasAnimation
        binding.timerAnimation.cancelAnimation()
        if (!selectedAnimation.hasAnimation) {
            binding.timerAnimation.visibility = View.INVISIBLE
            return
        }
        LottieCompositionFactory.fromRawRes(this, selectedAnimation.rawResId).addListener { composition ->
            if (loadedTimerAnimationKey != selectedAnimation.key) return@addListener
            binding.timerAnimation.setComposition(composition)
            if (currentTimerStatus == TimerStatus.IN_PROGRESS) {
                binding.timerAnimation.visibility = View.VISIBLE
                binding.timerAnimation.playAnimation()
            } else {
                binding.timerAnimation.visibility = View.INVISIBLE
            }
        }
    }

    private fun setupButtons() {
        binding.menu.setOnClickListener {
            this.startActivity(Intent(this, AnimationSelectionActivity::class.java))
        }

        binding.actionButton.setOnClickListener {
            when (currentTimerStatus) {
                TimerStatus.IN_PROGRESS -> pauseTimer()
                TimerStatus.PAUSED -> resumeTimer()
                TimerStatus.STOPPED -> startTimer()
            }
        }
        binding.quitButton.setOnClickListener { stopTimer() }
        binding.editPlanAction.setOnClickListener { showEditTimerDialog() }
    }

    private fun updatePlanSummary() {
        val timerPlanSettings = currentTimerPlanSettings()
        binding.currentPlanDurations.text = getString(
            R.string.plan_durations_format,
            timerPlanSettings.pomodoroMinutes.toInt(),
            timerPlanSettings.breakMinutes.toInt(),
            timerPlanSettings.longBreakMinutes.toInt()
        )
        binding.currentPlanSessions.text = getString(
            R.string.plan_sessions_format,
            timerPlanSettings.sessions.toInt()
        )
    }

    private fun showEditTimerDialog() {
        if (currentTimerStatus != TimerStatus.STOPPED) return

        EditTimerDialog.show(
            activity = this,
            initialSettings = currentTimerPlanSettings()
        ) { updatedSettings ->
            updateTimerPlanSettings(updatedSettings)
        }
    }

    private fun currentTimerPlanSettings(): TimerPlanSettings = TimerPlanPreferences.load(this)

    private fun updateTimerPlanSettings(updatedSettings: TimerPlanSettings) {
        TimerPlanPreferences.save(this, updatedSettings)
        syncPlanUiWithCurrentState()
    }

    private fun syncPlanUiWithCurrentState() {
        val timerPlanSettings = currentTimerPlanSettings()
        updatePlanSummary()
        if (currentTimerStatus == TimerStatus.STOPPED) {
            setProgressTime(timerPlanSettings.pomodoroMinutes * 60)
            updateSessionStatusChip(1L, timerPlanSettings.sessions)
        } else {
            updateSessionStatusChip(currentSessionIndex, timerPlanSettings.sessions)
        }
    }

    private fun startTimer() {
        val serviceIntent = Intent(this, ForegroundTimerService::class.java).apply {
            putExtra(ForegroundTimerService.EXTRA_ACTION, ForegroundTimerService.ACTION_START)
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun pauseTimer() {
        timerService?.pauseTimer()
    }

    private fun resumeTimer() {
        timerService?.resumeTimer()
    }

    private fun stopTimer() {
        timerService?.stopTimer()
        val intent = Intent(this, ForegroundTimerService::class.java)
        stopService(intent)
    }

    private fun updateUI(
        timeInSeconds: Long,
        timerType: TimerType,
        timerStatus: TimerStatus,
        sessionIndex: Long,
        totalSessions: Long
    ) {
        val timerPlanSettings = currentTimerPlanSettings()
        val previousTimerStatus = currentTimerStatus
        currentTimerStatus = timerStatus
        currentSessionIndex = sessionIndex
        updateSessionStatusChip(sessionIndex, totalSessions)

        val timeLeftString = DateUtils.formatElapsedTime(timeInSeconds)
        val timeTextAdapter = CircularProgressIndicator.ProgressTextAdapter { timeLeftString }
        binding.circularProgress.setProgressTextAdapter(timeTextAdapter)

        val startTime = when (timerType) {
            TimerType.POMODORO -> timerPlanSettings.pomodoroMinutes * 60
            TimerType.BREAK -> timerPlanSettings.breakMinutes * 60
            TimerType.LONG_BREAK -> timerPlanSettings.longBreakMinutes * 60
            else -> timerPlanSettings.pomodoroMinutes * 60
        }
        binding.circularProgress.setProgress(timeInSeconds.toDouble(), startTime.toDouble())

        when (timerType) {
            TimerType.SESSION_NOT_STARTED_YET -> {
                binding.timerType.text = getString(R.string.ready_for_new_session)
            }

            TimerType.POMODORO -> {
                binding.timerType.text = getString(R.string.timer_type_pomodoro)
            }

            TimerType.BREAK -> {
                binding.timerType.text = getString(R.string.timer_type_break)
            }

            TimerType.LONG_BREAK -> {
                binding.timerType.text = getString(R.string.timer_type_long_break)
            }

            TimerType.SESSION_COMPLETED -> {
                binding.timerType.text = getString(R.string.timer_type_completed)
                binding.confettiAnimation.visibility = View.VISIBLE
                binding.confettiAnimation.progress = 0f
                binding.confettiAnimation.playAnimation()
                hideRunningAnimation()
            }
        }

        when (timerStatus) {
            TimerStatus.IN_PROGRESS -> {
                updateActionState(timerStatus)
                binding.confettiAnimation.visibility = View.INVISIBLE
                if (previousTimerStatus != TimerStatus.IN_PROGRESS) {
                    loadSelectedTimerAnimation(true)
                } else {
                    showRunningAnimation()
                }
            }

            TimerStatus.STOPPED -> {
                updateActionState(timerStatus)
                hideRunningAnimation()
            }

            TimerStatus.PAUSED -> {
                updateActionState(timerStatus)
                hideRunningAnimation()
            }
        }
    }

    private fun showRunningAnimation() {
        if (!isTimerAnimationEnabled) {
            binding.timerAnimation.visibility = View.INVISIBLE
            return
        }
        if (binding.timerAnimation.isInvisible) {
            binding.timerAnimation.visibility = View.VISIBLE
        }
        if (!binding.timerAnimation.isAnimating) {
            binding.timerAnimation.playAnimation()
        }
    }

    private fun hideRunningAnimation() {
        binding.timerAnimation.visibility = View.INVISIBLE
        binding.timerAnimation.cancelAnimation()
    }

    private fun setProgressTime(time: Long) {
        val timeLeftString = DateUtils.formatElapsedTime(time)
        val timeTextAdapter = CircularProgressIndicator.ProgressTextAdapter { timeLeftString }
        binding.circularProgress.setProgressTextAdapter(timeTextAdapter)
        binding.circularProgress.setProgress(time.toDouble(), time.toDouble())
    }

    private fun updateSessionStatusChip(sessionIndex: Long, totalSessions: Long) {
        val safeTotal = totalSessions.coerceAtLeast(1L)
        val safeIndex = sessionIndex.coerceAtLeast(1L).coerceAtMost(safeTotal)
        binding.sessionStatusText.text = getString(
            R.string.session_status_format,
            getString(R.string.session),
            safeIndex.toInt(),
            safeTotal.toInt()
        )
    }

    private fun updateActionState(timerStatus: TimerStatus) {
        val canEditPlan = timerStatus == TimerStatus.STOPPED
        binding.actionButton.text = when (timerStatus) {
            TimerStatus.IN_PROGRESS -> getString(R.string.pause)
            TimerStatus.PAUSED -> getString(R.string.continue_button)
            TimerStatus.STOPPED -> getString(R.string.start)
        }
        binding.quitButton.isVisible = timerStatus == TimerStatus.PAUSED
        binding.menu.isVisible = canEditPlan
        binding.currentPlanCard.isVisible = canEditPlan
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.timerAnimation.cancelAnimation()
        binding.confettiAnimation.cancelAnimation()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        loadSelectedTimerAnimation()
    }
}
