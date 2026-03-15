package com.nemjava.pomodoro

import android.content.*
import android.content.pm.PackageManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.airbnb.lottie.LottieCompositionFactory
import com.nemjava.pomodoro.commons.TimerStatus
import com.nemjava.pomodoro.commons.TimerType
import com.nemjava.pomodoro.databinding.MainScreenFragmentBinding
import com.nemjava.pomodoro.screen.AboutActivity
import com.nemjava.pomodoro.screen.MainScreenViewModel
import com.nemjava.pomodoro.service.ForegroundTimerService
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainScreenViewModel>()
    private lateinit var binding: MainScreenFragmentBinding
    private var currentTimerStatus = TimerStatus.STOPPED
    private var currentSessionIndex = 1L
    private var timerService: ForegroundTimerService? = null
    private var serviceBound = false

    companion object {
        private const val DEFAULT_POMODORO_MINUTES = 20L
        private const val DEFAULT_BREAK_MINUTES = 3L
        private const val DEFAULT_LONG_BREAK_MINUTES = 5L
        private const val DEFAULT_SESSIONS = 2L
    }

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
                    ?: Pair(1L, getSessions())
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
        setProgressTime(getPomodoroMinutes() * 60)
        bindTimerService()
        setupButtons()
        observePlanSettings()
        updatePlanSummary()
        updateSessionStatusChip(1L, getSessions())
    }



    private fun bindTimerService() {
        val intent = Intent(this, ForegroundTimerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun loadAnimations() {
        LottieCompositionFactory.fromRawRes(this, R.raw.confetti).addListener {
            binding.fireworksAnim.setComposition(it)
            binding.fireworksAnim.visibility = View.INVISIBLE
            binding.fireworksAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.fireworksAnim.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {
                    binding.fireworksAnim.visibility = View.GONE
                }
            })
        }
        LottieCompositionFactory.fromRawRes(this, R.raw.cat).addListener {
            binding.catAnim.setComposition(it)
            binding.catAnim.visibility = View.INVISIBLE
        }
    }

    private fun setupButtons() {
        binding.menu.setOnClickListener {
            val popup = PopupMenu(this, binding.menu)
            popup.menuInflater.inflate(R.menu.menu, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.about -> {
                        startAboutActivity()
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popup.show()
        }

        binding.actionButton.setOnClickListener {
            when (currentTimerStatus) {
                TimerStatus.IN_PROGRESS -> pauseTimer()
                TimerStatus.PAUSED -> resumeTimer()
                TimerStatus.STOPPED -> startTimer()
            }
        }
        binding.quitButton.setOnClickListener { stopTimer() }
        binding.editPlanButton.setOnClickListener { showEditTimerDialog() }
    }

    private fun observePlanSettings() {
        viewModel.pomodoroTimeString.observe(this) {
            updatePlanSummary()
            if (currentTimerStatus == TimerStatus.STOPPED) {
                setProgressTime(getPomodoroMinutes() * 60)
            }
        }
        viewModel.breakTimeString.observe(this) { updatePlanSummary() }
        viewModel.longBreakTimeString.observe(this) { updatePlanSummary() }
        viewModel.sessionsString.observe(this) {
            updatePlanSummary()
            if (currentTimerStatus == TimerStatus.STOPPED) {
                updateSessionStatusChip(1L, getSessions())
            } else {
                updateSessionStatusChip(currentSessionIndex, getSessions())
            }
        }
    }

    private fun updatePlanSummary() {
        binding.currentPlanDurations.text = getString(
            R.string.plan_durations_format,
            getPomodoroMinutes().toInt(),
            getBreakMinutes().toInt(),
            getLongBreakMinutes().toInt()
        )
        binding.currentPlanSessions.text = getString(
            R.string.plan_sessions_format,
            getSessions().toInt()
        )
    }

    private fun showEditTimerDialog() {
        if (currentTimerStatus != TimerStatus.STOPPED) return

        EditTimerDialog.show(
            activity = this,
            initialSettings = TimerPlanSettings(
                pomodoroMinutes = getPomodoroMinutes(),
                breakMinutes = getBreakMinutes(),
                longBreakMinutes = getLongBreakMinutes(),
                sessions = getSessions()
            )
        ) { updatedSettings ->
            viewModel.setPomodoroTime(updatedSettings.pomodoroMinutes)
            viewModel.setBreakTime(updatedSettings.breakMinutes)
            viewModel.setLongBreakTime(updatedSettings.longBreakMinutes)
            viewModel.setSessions(updatedSettings.sessions)
            applyTimerSettingsToService()
            setProgressTime(updatedSettings.pomodoroMinutes * 60)
        }
    }

    private fun applyTimerSettingsToService() {
        timerService?.setTimerSettings(
            getPomodoroMinutes(),
            getBreakMinutes(),
            getLongBreakMinutes(),
            getSessions()
        )
    }

    private fun getPomodoroMinutes(): Long {
        return viewModel.pomodoroTimeString.value?.toLongOrNull() ?: DEFAULT_POMODORO_MINUTES
    }

    private fun getBreakMinutes(): Long {
        return viewModel.breakTimeString.value?.toLongOrNull() ?: DEFAULT_BREAK_MINUTES
    }

    private fun getLongBreakMinutes(): Long {
        return viewModel.longBreakTimeString.value?.toLongOrNull() ?: DEFAULT_LONG_BREAK_MINUTES
    }

    private fun getSessions(): Long {
        return viewModel.sessionsString.value?.toLongOrNull() ?: DEFAULT_SESSIONS
    }

    private fun startTimer() {
        val serviceIntent = Intent(this, ForegroundTimerService::class.java).apply {
            putExtra(ForegroundTimerService.EXTRA_ACTION, ForegroundTimerService.ACTION_START)
            putExtra(ForegroundTimerService.EXTRA_POMODORO_MINUTES, getPomodoroMinutes())
            putExtra(ForegroundTimerService.EXTRA_BREAK_MINUTES, getBreakMinutes())
            putExtra(ForegroundTimerService.EXTRA_LONG_BREAK_MINUTES, getLongBreakMinutes())
            putExtra(ForegroundTimerService.EXTRA_SESSIONS, getSessions())
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
        currentTimerStatus = timerStatus
        currentSessionIndex = sessionIndex
        updateSessionStatusChip(sessionIndex, totalSessions)

        val timeLeftString = DateUtils.formatElapsedTime(timeInSeconds)
        val timeTextAdapter = CircularProgressIndicator.ProgressTextAdapter { timeLeftString }
        binding.circularProgress.setProgressTextAdapter(timeTextAdapter)

        val startTime = when (timerType) {
            TimerType.POMODORO -> getPomodoroMinutes() * 60
            TimerType.BREAK -> getBreakMinutes() * 60
            TimerType.LONG_BREAK -> getLongBreakMinutes() * 60
            else -> getPomodoroMinutes() * 60
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
                binding.fireworksAnim.visibility = View.VISIBLE
                binding.fireworksAnim.progress = 0f
                binding.fireworksAnim.playAnimation()
                binding.catAnim.visibility = View.INVISIBLE
            }
        }

        when (timerStatus) {
            TimerStatus.IN_PROGRESS -> {
                updateActionState(timerStatus)
                binding.catAnim.visibility = View.VISIBLE
                binding.catAnim.playAnimation()
                binding.fireworksAnim.visibility = View.INVISIBLE
            }

            TimerStatus.STOPPED -> {
                updateActionState(timerStatus)
                binding.catAnim.visibility = View.INVISIBLE
                binding.catAnim.cancelAnimation()
            }

            TimerStatus.PAUSED -> {
                updateActionState(timerStatus)
                binding.catAnim.visibility = View.INVISIBLE
                binding.catAnim.cancelAnimation()
            }
        }
    }

    private fun startAboutActivity() {
        val intent = Intent(this, AboutActivity::class.java)
        this.startActivity(intent)
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
        binding.catAnim.cancelAnimation()
        binding.fireworksAnim.cancelAnimation()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
