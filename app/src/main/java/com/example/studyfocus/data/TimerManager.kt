package com.example.studyfocus.data

import android.content.Context
import android.content.SharedPreferences

class TimerManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_focus_timer_prefs", Context.MODE_PRIVATE)

    var isRunning: Boolean
        get() = prefs.getBoolean("is_running", false)
        private set(value) {
            prefs.edit().putBoolean("is_running", value).apply()
        }

    var startTimeMillis: Long
        get() = prefs.getLong("start_time_millis", 0L)
        private set(value) {
            prefs.edit().putLong("start_time_millis", value).apply()
        }

    var targetMinutes: Int
        get() = prefs.getInt("target_minutes", 0)
        set(value) {
            prefs.edit().putInt("target_minutes", value).apply()
        }

    fun startTimer(targetMins: Int) {
        isRunning = true
        startTimeMillis = System.currentTimeMillis()
        targetMinutes = targetMins
    }

    fun getElapsedSeconds(): Long {
        if (!isRunning || startTimeMillis == 0L) return 0L
        val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000L
        return if (elapsed < 0L) 0L else elapsed
    }

    fun stopTimer(): Long {
        val elapsed = getElapsedSeconds()
        isRunning = false
        startTimeMillis = 0L
        return elapsed
    }
}
