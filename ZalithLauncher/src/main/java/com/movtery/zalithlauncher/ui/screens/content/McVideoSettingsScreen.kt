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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

private const val TAG = "McVideoSettings"

private sealed class OptionValue {
    data class IntValue(val value: Int) : OptionValue()
    data class FloatValue(val value: Float) : OptionValue()
    data class BoolValue(val value: Boolean) : OptionValue()
    data class StringValue(val value: String) : OptionValue()
}

private data class McOption(
    val key: String,
    val label: String,
    val current: OptionValue,
    val default: OptionValue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McVideoSettingsScreen(
    backStackViewModel: ScreenBackStackViewModel
) {
    val scope = rememberCoroutineScope()

    BaseScreen(
        screenKey = NormalNavKey.McVideoSettings,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) {
        val allVersions = remember { VersionsManager.versions.value }
        val installedVersions = remember(allVersions) {
            allVersions.filter { it.isValid() }
        }

        var selectedVersion by remember {
            mutableStateOf(installedVersions.firstOrNull())
        }

        var options by remember(selectedVersion) {
            mutableStateOf(loadOptions(selectedVersion))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (installedVersions.isEmpty()) {
                Text(
                    text = stringResource(R.string.mc_video_settings_no_version),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                return@BaseScreen
            }

            // Version selector
            VersionSelector(
                versions = installedVersions,
                selected = selectedVersion,
                onSelect = {
                    selectedVersion = it
                    options = loadOptions(it)
                }
            )

            HorizontalDivider()

            // Options form
            var saved by remember { mutableStateOf(false) }

            options.forEach { option ->
                when (val v = option.current) {
                    is OptionValue.BoolValue -> BoolOption(
                        label = option.label,
                        value = v.value,
                        onValueChange = { newVal ->
                            options = options.map {
                                if (it.key == option.key) it.copy(current = OptionValue.BoolValue(newVal))
                                else it
                            }
                            saved = false
                        }
                    )
                    is OptionValue.IntValue -> IntSliderOption(
                        label = option.label,
                        key = option.key,
                        value = v.value,
                        onValueChange = { newVal ->
                            options = options.map {
                                if (it.key == option.key) it.copy(current = OptionValue.IntValue(newVal))
                                else it
                            }
                            saved = false
                        }
                    )
                    is OptionValue.FloatValue -> FloatSliderOption(
                        label = option.label,
                        key = option.key,
                        value = v.value,
                        onValueChange = { newVal ->
                            options = options.map {
                                if (it.key == option.key) it.copy(current = OptionValue.FloatValue(newVal))
                                else it
                            }
                            saved = false
                        }
                    )
                    is OptionValue.StringValue -> StringOption(
                        label = option.label,
                        value = v.value,
                        onValueChange = { newVal ->
                            options = options.map {
                                if (it.key == option.key) it.copy(current = OptionValue.StringValue(newVal))
                                else it
                            }
                            saved = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            saveOptions(selectedVersion, options)
                        }
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (saved) stringResource(R.string.mc_video_settings_saved)
                    else stringResource(R.string.mc_video_settings_save)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionSelector(
    versions: List<Version>,
    selected: Version?,
    onSelect: (Version) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.getVersionName() ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.mc_video_settings_version)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = { Text(version.getVersionName()) },
                    onClick = {
                        onSelect(version)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BoolOption(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = value, onCheckedChange = onValueChange)
        }
    }
}

@Composable
private fun IntSliderOption(
    label: String,
    key: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val range = remember(key) { getIntRange(key) }
    val step = remember(key) { getIntStep(key) }

    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt().coerceIn(range.first, range.last)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = ((range.last - range.first) / step) - 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FloatSliderOption(
    label: String,
    key: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val range = remember(key) { getFloatRange(key) }

    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "%.2f".format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = value,
                onValueChange = { onValueChange(it.coerceIn(range.first, range.last)) },
                valueRange = range.first..range.last,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StringOption(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    BackgroundCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

private fun getIntRange(key: String): IntRange {
    return when (key) {
        "renderDistance" -> 2..32
        "maxFps" -> 10..260
        "ao" -> 0..100
        "gamma" -> 0..10000
        "guiScale" -> 0..4
        "biomeBlendRadius" -> 0..7
        "mipmapLevels" -> 0..4
        "screenEffectScale" -> 0..100
        "fov" -> 30..110
        "chunkBuilderThreads" -> 0..8
        else -> 0..100
    }
}

private fun getIntStep(key: String): Int {
    return when (key) {
        "maxFps" -> 5
        "gamma" -> 50
        "ao", "screenEffectScale" -> 1
        else -> 1
    }
}

private fun getFloatRange(key: String): ClosedFloatingPointRange<Float> {
    return when (key) {
        "entityDistanceScaling" -> 0.5f..5.0f
        "fovScale" -> 0.1f..2.0f
        else -> 0.0f..1.0f
    }
}

private fun loadOptions(version: Version?): List<McOption> {
    if (version == null) return emptyList()

    val optionsFile = File(version.getGameDir(), "options.txt")
    if (!optionsFile.exists()) {
        addDefaultOptions(optionsFile)
    }

    val rawMap = mutableMapOf<String, String>()
    try {
        optionsFile.readLines().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                rawMap[line.take(idx)] = line.substring(idx + 1)
            }
        }
    } catch (e: IOException) {
        Logger.warning(TAG, "Failed to read options.txt", e)
    }

    return knownOptionKeys.mapNotNull { (key, label, defaultValue) ->
        val raw = rawMap[key] ?: defaultValue
        val parsed = parseValue(key, raw) ?: return@mapNotNull null
        McOption(key = key, label = label, current = parsed, default = parseValue(key, defaultValue)!!)
    }
}

private fun parseValue(key: String, raw: String): OptionValue? {
    return when (knownOptionTypes[key]) {
        "bool" -> OptionValue.BoolValue(raw == "true")
        "int" -> raw.toIntOrNull()?.let { OptionValue.IntValue(it) }
        "float" -> raw.toFloatOrNull()?.let { OptionValue.FloatValue(it) }
        "double" -> raw.toDoubleOrNull()?.let { OptionValue.FloatValue(it.toFloat()) }
        else -> OptionValue.StringValue(raw)
    }
}

private fun saveOptions(version: Version?, options: List<McOption>) {
    if (version == null) return

    val optionsFile = File(version.getGameDir(), "options.txt")
    if (!optionsFile.exists()) {
        addDefaultOptions(optionsFile)
    }

    try {
        val lines = optionsFile.readLines().toMutableList()

        options.forEach { opt ->
            val idx = lines.indexOfFirst { it.startsWith("${opt.key}:") }
            val valueStr = when (val v = opt.current) {
                is OptionValue.BoolValue -> if (v.value) "true" else "false"
                is OptionValue.IntValue -> v.value.toString()
                is OptionValue.FloatValue -> formatFloat(v.value)
                is OptionValue.StringValue -> v.value
            }
            val newLine = "${opt.key}:$valueStr"
            if (idx >= 0) {
                lines[idx] = newLine
            } else {
                lines.add(newLine)
            }
        }

        optionsFile.writeText(lines.joinToString("\n"))
        Logger.info(TAG, "Saved options.txt for ${version.getVersionName()}")
    } catch (e: IOException) {
        Logger.warning(TAG, "Failed to save options.txt", e)
    }
}

private fun formatFloat(value: Float): String {
    return if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
}

private fun addDefaultOptions(file: File) {
    file.parentFile?.mkdirs()
    file.writeText(
        knownOptionKeys.joinToString("\n") { (key, _, default) ->
            "$key:$default"
        }
    )
}

private val knownOptionTypes = mapOf(
    "renderDistance" to "int",
    "maxFps" to "int",
    "ao" to "int",
    "gamma" to "int",
    "guiScale" to "int",
    "biomeBlendRadius" to "int",
    "mipmapLevels" to "int",
    "screenEffectScale" to "int",
    "fov" to "int",
    "chunkBuilderThreads" to "int",
    "graphics" to "string",
    "smoothLighting" to "string",
    "particles" to "string",
    "clouds" to "string",
    "fullscreen" to "bool",
    "vsync" to "bool",
    "enableVsync" to "bool",
    "renderClouds" to "bool",
    "hideGLDebugInfo" to "bool",
    "entityShadows" to "bool",
    "entityDistanceScaling" to "double",
    "fovScale" to "double",
    "prioritizeChunkUpdates" to "string"
)

private val knownOptionKeys = listOf(
    "renderDistance" to "Render Distance" to "12",
    "graphics" to "Graphics" to "fast",
    "smoothLighting" to "Smooth Lighting" to "false",
    "particles" to "Particles" to "all",
    "ao" to "Ambient Occlusion" to "100",
    "clouds" to "Clouds" to "fancy",
    "fullscreen" to "Fullscreen" to "false",
    "vsync" to "VSync" to "false",
    "enableVsync" to "Enable VSync" to "false",
    "maxFps" to "Max Framerate" to "120",
    "entityDistanceScaling" to "Entity Distance" to "1.0",
    "gamma" to "Gamma" to "500",
    "guiScale" to "GUI Scale" to "0",
    "biomeBlendRadius" to "Biome Blend" to "4",
    "mipmapLevels" to "Mipmap Levels" to "4",
    "screenEffectScale" to "Screen Effect Scale" to "100",
    "fov" to "FOV" to "70",
    "fovScale" to "FOV Scale" to "1.0",
    "renderClouds" to "Render Clouds" to "true",
    "chunkBuilderThreads" to "Chunk Builder Threads" to "4",
    "hideGLDebugInfo" to "Hide GL Debug Info" to "true",
    "entityShadows" to "Entity Shadows" to "true",
    "prioritizeChunkUpdates" to "Prioritize Chunk Updates" to "default"
)
