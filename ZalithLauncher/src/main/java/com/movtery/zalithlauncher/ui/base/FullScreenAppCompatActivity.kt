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

package com.movtery.zalithlauncher.ui.base

import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

abstract class FullScreenAppCompatActivity : AbstractAppCompatActivity() {
    /**
     * @return 决定是否忽略前置摄像头区域
     */
    open fun isIgnoreNotch(): Boolean = true

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFullscreen()
    }

    @CallSuper
    override fun onPostResume() {
        super.onPostResume()
        applyFullscreen()
    }

    fun applyFullscreen() {
        val decorView = window.decorView
        val visibilityChangeListener = OnSystemUiVisibilityChangeListener { visibility: Int ->
            if (!isInMultiWindowMode) {
                if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                }
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        decorView.setOnSystemUiVisibilityChangeListener(visibilityChangeListener)
        visibilityChangeListener.onSystemUiVisibilityChange(decorView.systemUiVisibility) //call it once since the UI state may not change after the call, so the activity wont become fullscreen

        ignoreNotch()
    }

    private fun ignoreNotch() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = if (isIgnoreNotch()) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
        }
    }
}

/**
 * 实时监听全屏设置变化并更新刘海屏模式
 */
@Composable
fun ObserveFullScreenSetting(fullScreen: Boolean) {
    val activity = LocalActivity.current as? FullScreenAppCompatActivity ?: return
    LaunchedEffect(fullScreen) {
        activity.applyFullscreen()
    }
}