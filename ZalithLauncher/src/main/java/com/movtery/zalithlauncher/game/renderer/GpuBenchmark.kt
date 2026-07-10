package com.movtery.zalithlauncher.game.renderer

import android.opengl.GLES20
import android.opengl.GLES30
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.logging.Logger
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

// EGL 1.1+ constants not in the EGL10 interface
private const val EGL_OPENGL_ES2_BIT = 0x0004
private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098

private const val TAG = "GpuBenchmark"
private const val BENCHMARK_DURATION_NS = 3_000_000_000L // 3 seconds
private const val SURFACE_WIDTH = 256
private const val SURFACE_HEIGHT = 256
private const val TRIANGLE_COUNT = 100

fun runGpuBenchmark(): GpuBenchmarkResult {
    val egl = EGLContext.getEGL() as EGL10
    val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
    if (display == EGL10.EGL_NO_DISPLAY) {
        Logger.error(TAG, "Failed to get EGL display")
        return errorResult("No EGL display")
    }

    val version = IntArray(2)
    if (!egl.eglInitialize(display, version)) {
        Logger.error(TAG, "Failed to initialize EGL")
        return errorResult("EGL init failed")
    }

    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfigs = intArrayOf(1)
    val configAttribs = intArrayOf(
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_NONE
    )
    if (!egl.eglChooseConfig(display, configAttribs, configs, 1, numConfigs) || numConfigs[0] == 0) {
        egl.eglTerminate(display)
        Logger.error(TAG, "Failed to choose EGL config")
        return errorResult("No EGL config")
    }

    val surfAttribs = intArrayOf(
        EGL10.EGL_WIDTH, SURFACE_WIDTH,
        EGL10.EGL_HEIGHT, SURFACE_HEIGHT,
        EGL10.EGL_NONE
    )
    val surface = egl.eglCreatePbufferSurface(display, configs[0]!!, surfAttribs)
    if (surface == EGL10.EGL_NO_SURFACE) {
        egl.eglTerminate(display)
        Logger.error(TAG, "Failed to create pbuffer surface")
        return errorResult("No pbuffer surface")
    }

    val ctxAttribs = intArrayOf(
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL10.EGL_NONE
    )
    val context = egl.eglCreateContext(display, configs[0]!!, EGL10.EGL_NO_CONTEXT, ctxAttribs)
    if (context == EGL10.EGL_NO_CONTEXT) {
        egl.eglDestroySurface(display, surface)
        egl.eglTerminate(display)
        Logger.error(TAG, "Failed to create EGL context")
        return errorResult("No EGL context")
    }

    if (!egl.eglMakeCurrent(display, surface, surface, context)) {
        egl.eglDestroyContext(display, context)
        egl.eglDestroySurface(display, surface)
        egl.eglTerminate(display)
        Logger.error(TAG, "Failed to make current")
        return errorResult("EGL make current failed")
    }

    // Query GL info
    val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
    val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
    val glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"

    // Check for ES 3.0
    val isES30 = try {
        GLES30.glGetString(GLES30.GL_VERSION) != null
    } catch (_: Exception) {
        false
    }

    // Set up simple shader program
    val program = createProgram()
    val useProgram = program >= 0

    // Set up vertex data (small triangles scattered around)
    val vertexData = generateVertexData()
    val buffers = IntArray(1)
    GLES20.glGenBuffers(1, buffers, 0)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])

    val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertexData.size * 4)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer()
    vertexBuffer.put(vertexData)
    vertexBuffer.position(0)

    GLES20.glBufferData(
        GLES20.GL_ARRAY_BUFFER,
        vertexData.size * 4,
        vertexBuffer,
        GLES20.GL_STATIC_DRAW
    )

    if (useProgram) {
        val positionHandle = GLES20.glGetAttribLocation(program, "position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, 0)
    }

    // Benchmark loop
    GLES20.glClearColor(0.1f, 0.1f, 0.2f, 1.0f)
    var frameCount = 0
    val startTime = System.nanoTime()
    val deadline = startTime + BENCHMARK_DURATION_NS

    while (System.nanoTime() < deadline) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (useProgram) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexData.size / 3)
        }

        egl.eglSwapBuffers(display, surface)
        frameCount++
    }

    val elapsedNs = System.nanoTime() - startTime
    val fps = (frameCount.toFloat() / elapsedNs) * 1_000_000_000f

    // Cleanup
    if (useProgram) {
        val positionHandle = GLES20.glGetAttribLocation(program, "position")
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDeleteProgram(program)
    }
    GLES20.glDeleteBuffers(1, buffers, 0)

    egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
    egl.eglDestroyContext(display, context)
    egl.eglDestroySurface(display, surface)
    egl.eglTerminate(display)

    val hasVulkan = isES30 // Vulkan requires at least ES 3.0 on Android
    val recommendationResId = recommendRenderer(vendor, renderer, hasVulkan)

    Logger.info(TAG, "Benchmark complete: $fps FPS, $frameCount frames in ${elapsedNs / 1_000_000}ms")

    return GpuBenchmarkResult(
        gpuVendor = vendor,
        gpuRenderer = renderer,
        glVersion = "OpenGL ES ${if (isES30) "3.0+" else "2.0"} / $glVersion",
        hasVulkan = hasVulkan,
        fps = fps,
        recommendationResId = recommendationResId
    )
}

private fun createProgram(): Int {
    val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
    GLES20.glShaderSource(vertexShader, "attribute vec4 position; void main() { gl_Position = position; }")
    GLES20.glCompileShader(vertexShader)

    val fragShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
    GLES20.glShaderSource(fragShader, "precision mediump float; void main() { gl_FragColor = vec4(0.2, 0.6, 1.0, 1.0); }")
    GLES20.glCompileShader(fragShader)

    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragShader)
    GLES20.glLinkProgram(program)

    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (status[0] == 0) {
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteShader(fragShader)
        GLES20.glDeleteShader(vertexShader)
        return -1
    }

    GLES20.glDeleteShader(vertexShader)
    GLES20.glDeleteShader(fragShader)
    return program
}

private fun generateVertexData(): FloatArray {
    val vertsPerTriangle = 3
    val coordsPerVertex = 3
    val data = FloatArray(TRIANGLE_COUNT * vertsPerTriangle * coordsPerVertex)

    for (i in 0 until TRIANGLE_COUNT) {
        val base = i * 9
        // Each triangle is a small random-ish shape in NDC
        val cx = (Math.random().toFloat() * 2f) - 1f
        val cy = (Math.random().toFloat() * 2f) - 1f
        val size = 0.1f + Math.random().toFloat() * 0.2f

        data[base + 0] = cx
        data[base + 1] = cy + size
        data[base + 2] = 0f

        data[base + 3] = cx - size
        data[base + 4] = cy - size
        data[base + 5] = 0f

        data[base + 6] = cx + size
        data[base + 7] = cy - size
        data[base + 8] = 0f
    }

    return data
}

private fun recommendRenderer(vendor: String, renderer: String, hasVulkan: Boolean): Int {
    val vendorLower = vendor.lowercase()
    val rendererLower = renderer.lowercase()

    return if (hasVulkan) {
        if (rendererLower.contains("adreno") || vendorLower.contains("qualcomm")) {
            R.string.gpu_benchmark_recommend_adreno
        } else if (rendererLower.contains("mali") || vendorLower.contains("arm")) {
            R.string.gpu_benchmark_recommend_mali
        } else {
            R.string.gpu_benchmark_recommend_vulkan
        }
    } else {
        R.string.gpu_benchmark_recommend_fallback
    }
}

private fun errorResult(message: String): GpuBenchmarkResult {
    return GpuBenchmarkResult(
        gpuVendor = "Error",
        gpuRenderer = message,
        glVersion = "",
        hasVulkan = false,
        fps = 0f,
        recommendationResId = R.string.gpu_benchmark_error
    )
}
