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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import kotlinx.coroutines.delay
import org.lwjgl.glfw.CallbackBridge

@Composable
fun SideBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onFpsClick: () -> Unit,
    onRamClick: () -> Unit,
    onVersionsClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    var fps by remember { mutableIntStateOf(0) }
    var memoryInfo by remember { mutableStateOf(getMemoryInfo()) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                fps = CallbackBridge.getCurrentFps()
                memoryInfo = getMemoryInfo()
                delay(1000)
            }
        }
    }

    BackgroundCard(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FPS Indicator & Shortcut
            SideBarIndicator(
                label = "FPS",
                value = fps.toString(),
                icon = painterResource(R.drawable.ic_video_settings),
                onClick = onFpsClick
            )

            // RAM Indicator & Shortcut
            SideBarIndicator(
                label = "RAM",
                value = "${memoryInfo.first}M",
                icon = painterResource(R.drawable.ic_dashboard_outlined),
                onClick = onRamClick
            )

            Spacer(modifier = Modifier.weight(1f))

            // Versions Shortcut
            SideBarShortcut(
                icon = painterResource(R.drawable.ic_assignment_filled),
                contentDescription = "Versions",
                onClick = onVersionsClick
            )

            // Info Shortcut
            SideBarShortcut(
                icon = painterResource(R.drawable.ic_info_outlined),
                contentDescription = "About",
                onClick = onInfoClick
            )
        }
    }
}

@Composable
private fun SideBarIndicator(
    label: String,
    value: String,
    icon: Painter,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(0.8f)
            .clickable(onClick = onClick)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SideBarShortcut(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getMemoryInfo(): Pair<Long, Long> {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val total = runtime.maxMemory() / 1024 / 1024
    return Pair(used, total)
}
