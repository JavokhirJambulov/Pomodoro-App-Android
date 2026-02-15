package com.nemjava.pomodoro.screen

import androidx.lifecycle.*
import com.nemjava.pomodoro.commons.BuzzType
import com.nemjava.pomodoro.commons.MediaType

class MainScreenViewModel : ViewModel() {

    private var POMODORO_DEFAULT_TIME = 20L
    private var BREAK_DEFAULT_TIME = 3L
    private var LONG_BREAK_DEFAULT_TIME = 5L
    private var SESSION_DEFAULT = 2L

    // Timer settings
    private val _pomodoroTime = MutableLiveData(POMODORO_DEFAULT_TIME)
    val pomodoroTimeString: LiveData<String> = _pomodoroTime.map { time ->
        time.toString()
    }

    private val _breakTime = MutableLiveData(BREAK_DEFAULT_TIME)
    val breakTimeString: LiveData<String> = _breakTime.map { time ->
        time.toString()
    }

    private val _longBreakTime = MutableLiveData(LONG_BREAK_DEFAULT_TIME)
    val longBreakTimeString: LiveData<String> = _longBreakTime.map { time ->
        time.toString()
    }

    private val _sessions = MutableLiveData(SESSION_DEFAULT)
    val sessionsString: LiveData<String> = _sessions.map { time ->
        time.toString()
    }



    fun setPomodoroTime(time: Long) {
        _pomodoroTime.value = time
    }

    fun setBreakTime(time: Long) {
        _breakTime.value = time
    }

    fun setLongBreakTime(time: Long) {
        _longBreakTime.value = time
    }

    fun setSessions(session: Long) {
        _sessions.value = session
    }
}