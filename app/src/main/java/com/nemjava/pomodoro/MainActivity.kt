package com.nemjava.pomodoro

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nemjava.pomodoro.commons.*
import com.nemjava.pomodoro.databinding.MainScreenFragmentBinding
import com.nemjava.pomodoro.screen.AboutActivity
import com.nemjava.pomodoro.screen.MainIntroActivity
import com.nemjava.pomodoro.screen.MainScreenViewModel
import com.nemjava.pomodoro.service.ForegroundTimerService
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieCompositionFactory

class MainActivity : AppCompatActivity() {

    private var POMODORO_DEFAULT_TIME = 1200L
    private val viewModel by viewModels<MainScreenViewModel>()
    private lateinit var binding: MainScreenFragmentBinding
    private lateinit var preferencesPrivate: SharedPreferences

    // Service binding
    private var timerService: ForegroundTimerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ForegroundTimerService.TimerBinder
            timerService = binder.getService()
            serviceBound = true

            // Set up callbacks
            timerService?.setTimerUpdateCallback { timeInSeconds, timerType, timerStatus ->
                runOnUiThread {
                    updateUI(timeInSeconds, timerType, timerStatus)
                }
            }

            // Sync current state
            timerService?.getCurrentState()?.let { (time, type, status) ->
                updateUI(time, type, status)
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
        preferencesPrivate = this.getSharedPreferences(
            this.packageName + "_private_preferences",
            MODE_PRIVATE
        )
        if (isFirstRun()) {
            val i = Intent(this, MainIntroActivity::class.java)
            startActivity(i)
            consumeFirstRun()
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = DataBindingUtil.setContentView(this, R.layout.main_screen_fragment)
        binding.bottomSheet.mainScreenViewModel = viewModel
        binding.lifecycleOwner = this
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LottieCompositionFactory.fromRawRes(this, R.raw.confetti).addListener {
            binding.fireworksAnim.setComposition(it)
            binding.fireworksAnim.visibility = View.INVISIBLE
        }
        LottieCompositionFactory.fromRawRes(this, R.raw.cat).addListener {
            binding.catAnim.setComposition(it)
            binding.catAnim.visibility = View.INVISIBLE
        }

        setProgressTime(POMODORO_DEFAULT_TIME)
        setupUI()
        bindTimerService()
    }

    private fun bindTimerService() {
        val intent = Intent(this, ForegroundTimerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun setupUI() {
        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)

        binding.bottomSheet.pomodoroSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val pomodoroTime = (progress * 300L) + 600L
                viewModel.setPomodoroTime(pomodoroTime / 60)
                setProgressTime(pomodoroTime)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        binding.bottomSheet.breakSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val breakTime = progress * 180L + 180L
                viewModel.setBreakTime(breakTime / 60)
                setProgressTime(breakTime)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        binding.bottomSheet.longBreakSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                val longBreakTime = progress * 300L + 300L
                viewModel.setLongBreakTime(longBreakTime / 60)
                setProgressTime(longBreakTime)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        binding.bottomSheet.sessionsSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.setSessions(progress + 2L)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        setupBottomSheet(bottomSheetBehavior)
        setupButtons()
    }

    private fun setupBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>) {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.coordinatorLayout.bringToFront()
            bottomSheetBehavior.isHideable = true
            bottomSheetBehavior.peekHeight = 150
            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {}
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset > 0.1) {
                        binding.startButton.alpha = ((0.9 - slideOffset).toFloat())
                        binding.startButton.isEnabled = false
                    } else {
                        binding.startButton.alpha = 1F
                        binding.startButton.isEnabled = true
                    }
                }
            })
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

        binding.startButton.setOnClickListener {
            startTimer()
        }
        binding.pauseButton.setOnClickListener {
            pauseTimer()
        }
        binding.continueButton.setOnClickListener {
            resumeTimer()
        }
        binding.quitButton.setOnClickListener {
            stopTimer()
        }
    }

    private fun startTimer() {
        // Update service with current settings
        val pomodoroMinutes = viewModel.pomodoroTimeString.value?.toLongOrNull() ?: 20L
        val breakMinutes = viewModel.breakTimeString.value?.toLongOrNull() ?: 3L
        val longBreakMinutes = viewModel.longBreakTimeString.value?.toLongOrNull() ?: 5L
        val sessions = viewModel.sessionsString.value?.toLongOrNull() ?: 2L

        timerService?.setTimerSettings(pomodoroMinutes, breakMinutes, longBreakMinutes, sessions)

        // Start service if not running
        val serviceIntent = Intent(this, ForegroundTimerService::class.java).apply {
            putExtra("action", "START")
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

    private fun updateUI(timeInSeconds: Long, timerType: TimerType, timerStatus: TimerStatus) {
        // Update progress indicator
        val timeLeftString = DateUtils.formatElapsedTime(timeInSeconds)
        val timeTextAdapter = CircularProgressIndicator.ProgressTextAdapter { timeLeftString }
        binding.circularProgress.setProgressTextAdapter(timeTextAdapter)

        // Calculate start time based on timer type
        val startTime = when (timerType) {
            TimerType.POMODORO -> (viewModel.pomodoroTimeString.value?.toLongOrNull() ?: 20L) * 60
            TimerType.BREAK -> (viewModel.breakTimeString.value?.toLongOrNull() ?: 3L) * 60
            TimerType.LONG_BREAK -> (viewModel.longBreakTimeString.value?.toLongOrNull() ?: 5L) * 60
            else -> 1200L
        }

        binding.circularProgress.setProgress(timeInSeconds.toDouble(), startTime.toDouble())

        // Update timer type text
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
                binding.fireworksAnim.playAnimation()
                binding.catAnim.visibility = View.INVISIBLE
            }
        }

        // Update buttons based on status
        when (timerStatus) {
            TimerStatus.IN_PROGRESS -> {
                visibleButton(TimerButton.PAUSE_BTN)
                val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                binding.catAnim.visibility = View.VISIBLE
                binding.catAnim.playAnimation()
                binding.fireworksAnim.visibility = View.INVISIBLE
            }
            TimerStatus.STOPPED -> {
                visibleButton(TimerButton.START_BTN)
                val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.catAnim.visibility = View.INVISIBLE
                binding.catAnim.cancelAnimation()
            }
            TimerStatus.PAUSED -> {
                visibleButton(TimerButton.CONTINUE_BTN)
                val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.root)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
        binding.circularProgress.setProgress(time.toDouble(), POMODORO_DEFAULT_TIME.toDouble())
    }

    private fun visibleButton(button: TimerButton) {
        binding.continueButton.isVisible = button == TimerButton.CONTINUE_BTN
        binding.quitButton.isVisible = button == TimerButton.CONTINUE_BTN
        binding.pauseButton.isVisible = button == TimerButton.PAUSE_BTN
        binding.startButton.isVisible = button == TimerButton.START_BTN
        binding.menu.isVisible = button == TimerButton.START_BTN
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun isFirstRun() = preferencesPrivate.getBoolean(Constants.FIRST_RUN, true)

    private fun consumeFirstRun() = preferencesPrivate.edit { putBoolean(Constants.FIRST_RUN, false) }
}