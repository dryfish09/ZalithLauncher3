package com.movtery.zalithlauncher.game.renderer

import androidx.annotation.StringRes

data class GpuBenchmarkResult(
    val gpuVendor: String,
    val gpuRenderer: String,
    val glVersion: String,
    val hasVulkan: Boolean,
    val fps: Float,
    @StringRes val recommendationResId: Int
)
