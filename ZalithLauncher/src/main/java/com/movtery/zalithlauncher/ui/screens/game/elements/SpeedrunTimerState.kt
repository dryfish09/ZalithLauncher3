package com.movtery.zalithlauncher.ui.screens.game.elements

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.LogMultiplexer
import com.movtery.zalithlauncher.setting.launcherMMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONTokener

object SpeedrunTimerState : LoggerBridge.EventLogListener {
    var rtaMs by mutableLongStateOf(0L)
    var igtMs by mutableLongStateOf(0L)
    var currentWorld by mutableStateOf<String?>(null)
    var isRunning by mutableStateOf(false)

    private var sessionStartMs = 0L
    private var gameActiveStartMs = 0L
    private var accumulatedIgtMs = 0L
    private var tickJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mmkv = launcherMMKV()

    private fun worldRtaKey(name: String) = "sr_rta_$name"
    private fun worldIgtKey(name: String) = "sr_igt_$name"
    private val worldsListKey = "sr_worlds"

    fun getWorldRta(worldName: String): Long = mmkv.getLong(worldRtaKey(worldName), 0L)
    fun getWorldIgt(worldName: String): Long = mmkv.getLong(worldIgtKey(worldName), 0L)

    fun getAllWorlds(): List<String> {
        val raw = mmkv.getString(worldsListKey, "[]") ?: "[]"
        return try {
            val arr = JSONArray(JSONTokener(raw))
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveWorldsList(worlds: Collection<String>) {
        mmkv.putString(worldsListKey, JSONArray(worlds.toList()).toString()).apply()
    }

    fun startTimer(worldName: String? = null) {
        if (isRunning) return
        if (worldName != null && worldName != currentWorld) {
            saveCurrentWorld()
            currentWorld = worldName
        }
        sessionStartMs = SystemClock.elapsedRealtime()
        gameActiveStartMs = sessionStartMs
        accumulatedIgtMs = getWorldIgt(currentWorld ?: "")
        rtaMs = getWorldRta(currentWorld ?: "")
        igtMs = accumulatedIgtMs
        isRunning = true
        tickJob = scope.launch {
            while (isActive) {
                delay(100)
                if (isRunning) {
                    val now = SystemClock.elapsedRealtime()
                    rtaMs = getWorldRta(currentWorld ?: "") + (now - sessionStartMs)
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
        rtaMs += (now - sessionStartMs)
        igtMs = accumulatedIgtMs
        saveCurrentWorld()
        tickJob?.cancel()
        tickJob = null
        isRunning = false
    }

    fun resetTimer() {
        saveCurrentWorld()
        tickJob?.cancel()
        tickJob = null
        isRunning = false
        rtaMs = 0L
        igtMs = 0L
        accumulatedIgtMs = 0L
        sessionStartMs = 0L
        gameActiveStartMs = 0L
    }

    fun deleteWorld(worldName: String) {
        if (currentWorld == worldName) resetTimer()
        mmkv.removeValue(worldRtaKey(worldName))
        mmkv.removeValue(worldIgtKey(worldName))
        val worlds = getAllWorlds().toMutableList()
        worlds.remove(worldName)
        saveWorldsList(worlds)
    }

    private fun saveCurrentWorld() {
        val world = currentWorld ?: return
        if (rtaMs > 0) mmkv.putLong(worldRtaKey(world), rtaMs).apply()
        if (igtMs > 0) mmkv.putLong(worldIgtKey(world), igtMs).apply()
        val worlds = getAllWorlds().toMutableList()
        if (world !in worlds) {
            worlds.add(world)
            saveWorldsList(worlds)
        }
    }

    fun enable() {
        LogMultiplexer.addListener(this)
    }

    fun disable() {
        LogMultiplexer.removeListener(this)
        stopTimer()
    }

    override fun onEventLogged(text: String) {
        if (text.contains("Preparing spawn area")) {
            val world = parseWorldName(text) ?: "World"
            if (world != currentWorld) {
                saveCurrentWorld()
                currentWorld = world
                startTimer(world)
            }
        }
        if (text.contains("Saving worlds") || text.contains("Disconnected")) {
            pauseIgt()
        }
    }

    private fun parseWorldName(line: String): String? {
        val match = Regex("Preparing spawn area: (.+)").find(line)
        return match?.groupValues?.getOrNull(1)?.trim()
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
}
