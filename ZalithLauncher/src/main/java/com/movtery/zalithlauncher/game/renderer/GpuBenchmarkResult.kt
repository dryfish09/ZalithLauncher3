package com.movtery.zalithlauncher.game.renderer

data class GpuBenchmarkResult(
    val gpuVendor: String,
    val gpuRenderer: String,
    val glVersion: String,
    val hasVulkan: Boolean,
    val fps: Float,
    val recommendation: String
)
