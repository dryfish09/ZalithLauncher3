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

package com.movtery.zalithlauncher.ui.screens.content.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.settings.MobileGluesConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileGluesSettingsDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val config = remember {
        MobileGluesConfig.load() ?: MobileGluesConfig()
    }

    var enableANGLE by remember { mutableIntStateOf(config.enableANGLE) }
    var enableNoError by remember { mutableIntStateOf(config.enableNoError) }
    var enableExtTimerQuery by remember { mutableStateOf(config.enableExtTimerQuery == 0) }
    var enableExtComputeShader by remember { mutableStateOf(config.enableExtComputeShader == 1) }
    var enableExtDirectStateAccess by remember { mutableStateOf(config.enableExtDirectStateAccess == 1) }
    var maxGlslCacheSize by remember { mutableStateOf(config.maxGlslCacheSize.toString()) }
    var multidrawMode by remember { mutableIntStateOf(config.multidrawMode) }
    var angleDepthClearFixMode by remember { mutableIntStateOf(config.angleDepthClearFixMode) }
    var customGLVersion by remember { mutableIntStateOf(config.customGLVersion) }
    val disabledLabel = stringResource(R.string.mobileglues_gl_disabled)
    val glVersions = remember(disabledLabel) {
        listOf(
            0 to disabledLabel,
            32 to "OpenGL 3.2", 33 to "OpenGL 3.3",
            40 to "OpenGL 4.0", 41 to "OpenGL 4.1", 42 to "OpenGL 4.2",
            43 to "OpenGL 4.3", 44 to "OpenGL 4.4", 45 to "OpenGL 4.5",
            46 to "OpenGL 4.6"
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = cardColor(false),
            contentColor = onCardColor(),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mobileglues_settings_title),
                    style = MaterialTheme.typography.titleLarge
                )

                HorizontalDivider()

                SectionHeader(stringResource(R.string.mobileglues_section_rendering))

                ANGLEPicker(
                    value = enableANGLE,
                    onValueChange = { enableANGLE = it }
                )

                NoErrorPicker(
                    value = enableNoError,
                    onValueChange = { enableNoError = it }
                )

                MultidrawModeSegmented(
                    value = multidrawMode,
                    onValueChange = { multidrawMode = it }
                )

                AngleClearPicker(
                    value = angleDepthClearFixMode,
                    onValueChange = { angleDepthClearFixMode = it }
                )

                GLVersionDropdown(
                    value = customGLVersion,
                    options = glVersions,
                    onValueChange = { customGLVersion = it }
                )

                HorizontalDivider()

                SectionHeader(stringResource(R.string.mobileglues_section_extensions))

                SettingsSwitchRow(
                    title = stringResource(R.string.mobileglues_compute_shader),
                    summary = stringResource(R.string.mobileglues_compute_shader_summary),
                    checked = enableExtComputeShader,
                    onCheckedChange = { enableExtComputeShader = it }
                )

                SettingsSwitchRow(
                    title = stringResource(R.string.mobileglues_timer_query),
                    summary = stringResource(R.string.mobileglues_timer_query_summary),
                    checked = enableExtTimerQuery,
                    onCheckedChange = { enableExtTimerQuery = it }
                )

                SettingsSwitchRow(
                    title = stringResource(R.string.mobileglues_direct_state_access),
                    summary = stringResource(R.string.mobileglues_direct_state_access_summary),
                    checked = enableExtDirectStateAccess,
                    onCheckedChange = { enableExtDirectStateAccess = it }
                )

                HorizontalDivider()
                SectionHeader(stringResource(R.string.mobileglues_section_cache))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = maxGlslCacheSize,
                    onValueChange = { maxGlslCacheSize = it },
                    label = { Text(stringResource(R.string.mobileglues_cache_size_label)) },
                    supportingText = { Text(stringResource(R.string.mobileglues_cache_size_supporting)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        config.enableANGLE = enableANGLE
                        config.enableNoError = enableNoError
                        config.enableExtTimerQuery = if (enableExtTimerQuery) 0 else 1
                        config.enableExtComputeShader = if (enableExtComputeShader) 1 else 0
                        config.enableExtDirectStateAccess = if (enableExtDirectStateAccess) 1 else 0
                        config.maxGlslCacheSize = maxGlslCacheSize.toIntOrNull() ?: 32
                        config.multidrawMode = multidrawMode
                        config.angleDepthClearFixMode = angleDepthClearFixMode
                        config.customGLVersion = customGLVersion
                        config.save()
                        Toast.makeText(context, context.getString(R.string.mobileglues_saved_toast), Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.generic_confirm))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ANGLEPicker(value: Int, onValueChange: (Int) -> Unit) {
    val disable = stringResource(R.string.mobileglues_angle_disable)
    val auto = stringResource(R.string.mobileglues_angle_auto)
    val force = stringResource(R.string.mobileglues_angle_force)
    val compatible = stringResource(R.string.mobileglues_angle_compatible)
    val options = remember(disable, auto, force, compatible) {
        listOf(disable to 0, auto to 1, force to 2, compatible to 3)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_angle_mode),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun NoErrorPicker(value: Int, onValueChange: (Int) -> Unit) {
    val strict = stringResource(R.string.mobileglues_no_error_strict)
    val moderate = stringResource(R.string.mobileglues_no_error_moderate)
    val relaxed = stringResource(R.string.mobileglues_no_error_relaxed)
    val ignoreAll = stringResource(R.string.mobileglues_no_error_ignore_all)
    val options = remember(strict, moderate, relaxed, ignoreAll) {
        listOf(strict to 0, moderate to 1, relaxed to 2, ignoreAll to 3)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_no_error_mode),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun MultidrawModeSegmented(value: Int, onValueChange: (Int) -> Unit) {
    Text(stringResource(R.string.mobileglues_multidraw_mode), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    val auto = stringResource(R.string.mobileglues_multidraw_auto)
    val baseVertex = stringResource(R.string.mobileglues_multidraw_basevertex)
    val compute = stringResource(R.string.mobileglues_multidraw_compute)
    val options = remember(auto, baseVertex, compute) {
        listOf(auto to 0, baseVertex to 1, compute to 2)
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, v) ->
            SegmentedButton(
                selected = value == v,
                onClick = { onValueChange(v) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AngleClearPicker(value: Int, onValueChange: (Int) -> Unit) {
    val off = stringResource(R.string.mobileglues_angle_clear_off)
    val mode1 = stringResource(R.string.mobileglues_angle_clear_mode1)
    val mode2 = stringResource(R.string.mobileglues_angle_clear_mode2)
    val options = remember(off, mode1, mode2) {
        listOf(off to 0, mode1 to 1, mode2 to 2)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_angle_depth_clear_fix),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GLVersionDropdown(
    value: Int,
    options: List<Pair<Int, String>>,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == value }?.second ?: stringResource(R.string.mobileglues_gl_disabled)

    Text(stringResource(R.string.mobileglues_custom_gl_version), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (v, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingRow(
    label: String,
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.second == selectedValue }?.first ?: options.first().first

    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, v) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
