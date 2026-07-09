package com.movtery.zalithlauncher.ui.screens.game.elements

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.path.PathManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenRecorderManager {
    var isRecording by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var outputFile: File? = null

    fun startRecording(context: Context, resultCode: Int, data: Intent) {
        if (isRecording) return
        errorMessage = null

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "recording_$dateStr.mp4"
        val dir = File(PathManager.DIR_FILES_EXTERNAL, "screen_recorder")
        if (!dir.exists()) dir.mkdirs()
        outputFile = File(dir, fileName)

        val recorder = MediaRecorder()
        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile?.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8_000_000)
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
            prepare()
        }
        mediaRecorder = recorder

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(DisplayManager.DISPLAY_DEFAULT)

        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.densityDpi.toFloat()

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecorder",
            1920, 1080, density.toInt(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder.surface, null, null
        )

        recorder.start()
        isRecording = true
    }

    fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }

        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaProjection = null
        mediaRecorder = null
        virtualDisplay = null
        isRecording = false
    }
}
