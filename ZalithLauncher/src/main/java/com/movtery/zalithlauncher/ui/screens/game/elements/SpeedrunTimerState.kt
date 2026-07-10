package com.movtery.zalithlauncher.ui.screens.game.elements

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.LogMultiplexer
import com.movtery.zalithlauncher.setting.AllSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SpeedrunTimerState : LoggerBridge.EventLogListener {
    var rtaMs by mutableLongStateOf(0L)
    var igtMs by mutableLongStateOf(0L)
    var currentWorld by mutableStateOf<String?>(null)
    var isRunning by mutableStateOf(false)
    var isIgtPaused by mutableStateOf(false)

    private var sessionStartMs = 0L
    private var gameActiveStartMs = 0L
    private var accumulatedIgtMs = 0L
    private var tickJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun toggleIgtPause() {
        if (!isRunning) return
        if (isIgtPaused) resumeIgt() else pauseIgt()
        isIgtPaused = !isIgtPaused
    }

    override fun onEventLogged(text: String) {
        if (!AllSettings.speedrunTimerEnabled.getValue()) return
        if (text.contains("Preparing spawn area")) {
            val world = parseWorldName(text) ?: "World"
            if (world != currentWorld) {
                currentWorld = world
                startTimer(world)
            }
        }
        if (text.contains("Saving worlds") || text.contains("Disconnected")) {
            if (isRunning) stopTimer()
        }
    }

    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val tenths = (ms % 1000) / 100
        return if (hours > 0) {
            "%02d:%02d:%02d.%d".format(hours, mins, secs, tenths)
        } else {
            "%02d:%02d.%d".format(mins, secs, tenths)
        }
    }

    fun startTimer(worldName: String? = null) {
        if (isRunning) return
        currentWorld = worldName ?: currentWorld
        sessionStartMs = SystemClock.elapsedRealtime()
        gameActiveStartMs = sessionStartMs
        accumulatedIgtMs = 0L
        rtaMs = 0L
        igtMs = 0L
        isIgtPaused = false
        isRunning = true
        tickJob = scope.launch {
            while (isActive) {
                delay(100)
                if (isRunning) {
                    val now = SystemClock.elapsedRealtime()
                    rtaMs = now - sessionStartMs
                    igtMs = accumulatedIgtMs + (now - gameActiveStartMs)
                }
            }
        }
    }

    fun pauseIgt() {
        if (!isRunning) return
        val now = SystemClock.elapsedRealtime()
        accumulatedIgtMs += (now - gameActiveStartMs)
        gameActiveStartMs = now
    }

    fun resumeIgt() {
        if (!isRunning) return
        gameActiveStartMs = SystemClock.elapsedRealtime()
    }

    fun stopTimer() {
        if (!isRunning) return
        val now = SystemClock.elapsedRealtime()
        accumulatedIgtMs += (now - gameActiveStartMs)
        rtaMs = now - sessionStartMs
        igtMs = accumulatedIgtMs
        tickJob?.cancel()
        tickJob = null
        isRunning = false
    }

    fun resetTimer() {
        tickJob?.cancel()
        tickJob = null
        isRunning = false
        rtaMs = 0L
        igtMs = 0L
        accumulatedIgtMs = 0L
        sessionStartMs = 0L
        gameActiveStartMs = 0L
    }

    fun enable() {
        LogMultiplexer.addListener(this)
    }

    fun disable() {
        LogMultiplexer.removeListener(this)
        stopTimer()
    }

    private fun parseWorldName(line: String): String? {
        val match = Regex("Preparing spawn area: (.+)").find(line)
        return match?.groupValues?.getOrNull(1)?.trim()
    }
}
