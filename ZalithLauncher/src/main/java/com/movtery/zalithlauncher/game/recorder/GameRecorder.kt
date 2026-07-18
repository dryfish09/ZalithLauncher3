/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.recorder

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "GameRecorder"
private const val FRAME_RATE = 30
private const val VIDEO_BIT_RATE = 6_000_000

object GameRecorder {
    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    @Volatile private var recorder: MediaRecorder? = null
    @Volatile private var inputSurface: android.view.Surface? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private val isCapturing = AtomicBoolean(false)
    @Volatile private var pendingUri: android.net.Uri? = null
    @Volatile private var pendingFile: File? = null

    fun start(context: Context, withMic: Boolean = false) {
        if (_state.value != RecordingState.IDLE) return

        val view = GameSurfaceRegistry.getView()
        if (view == null) {
            Log.e(TAG, "No game surface registered — cannot start recording")
            return
        }

        val w = (view.width.coerceAtLeast(2) / 2) * 2
        val h = (view.height.coerceAtLeast(2) / 2) * 2

        try {
            val (uri, file) = createOutputEntry(context)
            pendingUri = uri
            pendingFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                if (withMic) setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (withMic) setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(w, h)
                setVideoFrameRate(FRAME_RATE)
                setVideoEncodingBitRate(VIDEO_BIT_RATE)
                setOutputFile(
                    context.contentResolver.openFileDescriptor(uri, "w")!!.fileDescriptor
                )
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error what=$what extra=$extra")
                    cleanup()
                }
                prepare()
            }

            inputSurface = rec.surface
            recorder = rec
            rec.start()

            captureThread = HandlerThread("GameRecorder-Capture").also { it.start() }
            captureHandler = Handler(captureThread!!.looper)

            isCapturing.set(true)
            _state.value = RecordingState.RECORDING
            scheduleNextFrame()

            Log.i(TAG, "Recording started ${w}x${h} mic=$withMic")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            cleanup()
        }
    }

    fun pause() {
        if (_state.value != RecordingState.RECORDING) return
        try {
            recorder?.pause()
            _state.value = RecordingState.PAUSED
            Log.i(TAG, "Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause: ${e.message}")
        }
    }

    fun resume() {
        if (_state.value != RecordingState.PAUSED) return
        try {
            recorder?.resume()
            _state.value = RecordingState.RECORDING
            scheduleNextFrame()
            Log.i(TAG, "Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume: ${e.message}")
        }
    }

    fun stopAndSave(context: Context) {
        val current = _state.value
        if (current == RecordingState.IDLE || current == RecordingState.STOPPING) return
        _state.value = RecordingState.STOPPING
        isCapturing.set(false)

        captureHandler?.post { finalise(context) }
            ?: run { finalise(context) }
    }

    private fun scheduleNextFrame() {
        if (!isCapturing.get() || _state.value != RecordingState.RECORDING) return
        captureHandler?.postDelayed({ captureFrame() }, (1000L / FRAME_RATE))
    }

    private fun captureFrame() {
        if (!isCapturing.get()) return
        if (_state.value != RecordingState.RECORDING) return

        val view = GameSurfaceRegistry.getView()
        val surface = inputSurface
        if (view == null || surface == null) { scheduleNextFrame(); return }

        when (view) {
            is SurfaceView -> captureFromSurfaceView(view, surface)
            is TextureView -> captureFromTextureView(view, surface)
            else           -> scheduleNextFrame()
        }
    }

    private fun captureFromSurfaceView(sv: SurfaceView, out: android.view.Surface) {
        val w = sv.width.coerceAtLeast(1)
        val h = sv.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        PixelCopy.request(sv, bmp, { result ->
            if (result == PixelCopy.SUCCESS) drawToSurface(bmp, out)
            bmp.recycle()
            scheduleNextFrame()
        }, captureHandler!!)
    }

    private fun captureFromTextureView(tv: TextureView, out: android.view.Surface) {
        val bmp = tv.getBitmap(tv.width.coerceAtLeast(1), tv.height.coerceAtLeast(1))
        if (bmp != null) { drawToSurface(bmp, out); bmp.recycle() }
        scheduleNextFrame()
    }

    @Suppress("DEPRECATION")
    private fun drawToSurface(bmp: Bitmap, surface: android.view.Surface) {
        runCatching {
            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) surface.lockHardwareCanvas()
                else surface.lockCanvas(null)
            } catch (_: Exception) {
                surface.lockCanvas(null)
            }
            canvas.drawBitmap(bmp, 0f, 0f, null)
            surface.unlockCanvasAndPost(canvas)
        }.onFailure { Log.w(TAG, "drawToSurface failed: ${it.message}") }
    }

    private fun finalise(context: Context) {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            inputSurface?.release()
            inputSurface = null

            pendingUri?.let { uri ->
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                try {
                    context.contentResolver.update(uri, values, null, null)
                    Log.i(TAG, "Recording saved: $uri")
                } catch (e: Exception) {
                    Log.e(TAG, "MediaStore update failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Finalise error: ${e.message}")
        } finally {
            captureThread?.quit()
            captureThread = null
            captureHandler = null
            pendingUri = null
            pendingFile = null
            _state.value = RecordingState.IDLE
        }
    }

    private fun cleanup() {
        isCapturing.set(false)
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { inputSurface?.release() }
        inputSurface = null
        captureThread?.quit()
        captureThread = null
        captureHandler = null
        _state.value = RecordingState.IDLE
    }

    private fun createOutputEntry(context: Context): Pair<android.net.Uri, File> {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ZalithRec_$ts.mp4"
        val relPath = "Movies/Zalith Recordings"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relPath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create MediaStore entry for recording")

        val publicMovies = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MOVIES
        )
        val file = File(publicMovies, "Zalith Recordings/$fileName")
        return Pair(uri, file)
    }
}
