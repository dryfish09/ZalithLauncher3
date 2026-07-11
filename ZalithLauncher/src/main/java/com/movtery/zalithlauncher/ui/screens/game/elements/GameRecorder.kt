package com.movtery.zalithlauncher.ui.screens.game.elements

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.annotation.Keep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.path.PathManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GameRecorder {
    var isRecording by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var lastSavedFile by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var outputFile: File? = null
    private var frameWidth = 0
    private var frameHeight = 0

    fun startRecording(outputWidth: Int, outputHeight: Int) {
        if (isRecording) return
        errorMessage = null
        lastSavedFile = null
        frameWidth = outputWidth
        frameHeight = outputHeight

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "recording_$dateStr.mp4"
        val dir = File(PathManager.DIR_FILES_EXTERNAL, "screen_recorder")
        if (!dir.exists()) dir.mkdirs()
        outputFile = File(dir, fileName)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outputWidth, outputHeight)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        mediaCodec = codec

        mediaMuxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        nativeSetRecording(true, outputWidth, outputHeight)
        isRecording = true
    }

    fun stopRecording(scope: CoroutineScope) {
        if (!isRecording) return
        isRecording = false
        isSaving = true
        nativeSetRecording(false, 0, 0)

        scope.launch(Dispatchers.IO) {
            if (muxerStarted) {
                drainEncoder(eos = true)
            }

            cleanupEncoder()

            val savedPath = outputFile?.absolutePath
            lastSavedFile = savedPath
            isSaving = false
        }
    }

    private fun cleanupEncoder() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaMuxer?.stop()
            mediaMuxer?.release()
        } catch (e: Exception) {
            errorMessage = e.message
        }
        mediaCodec = null
        mediaMuxer = null
        muxerStarted = false
        trackIndex = -1
        frameWidth = 0
        frameHeight = 0
    }

    private external fun nativeSetRecording(active: Boolean, width: Int, height: Int)

    @Keep
    @JvmStatic
    fun onFrameNV12(nv12Buffer: ByteBuffer) {
        if (!isRecording) return
        nv12Buffer.position(0)
        val codec = mediaCodec ?: return

        var inputIndex: Int
        do {
            inputIndex = codec.dequeueInputBuffer(0)
        } while (inputIndex < 0)

        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(nv12Buffer)
        codec.queueInputBuffer(inputIndex, 0, nv12Buffer.capacity(), System.nanoTime() / 1000, 0)

        drainEncoder(eos = false)
    }

    private fun drainEncoder(eos: Boolean) {
        val codec = mediaCodec ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        if (eos && muxerStarted) {
            codec.signalEndOfInputStream()
        }

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!eos) break
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (trackIndex < 0) {
                        trackIndex = mediaMuxer?.addTrack(codec.outputFormat) ?: -1
                    }
                    if (trackIndex >= 0 && !muxerStarted) {
                        mediaMuxer?.start()
                        muxerStarted = true
                    }
                }
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer?.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }
    }
}
