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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.scrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.getFileName
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.BaseFileItem
import com.movtery.zalithlauncher.ui.screens.content.elements.CreateNewDirDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.CreateNewFileDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.utils.file.sortWithFileName
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private const val TAG = "BuiltInFileManager"

/** 文本编辑器能够处理的文件后缀 */
private val EDITABLE_EXTENSIONS = setOf(
    "txt", "json", "json5", "properties", "yml", "yaml", "cfg", "conf",
    "toml", "log", "mcmeta", "lang", "ini", "xml", "md", "gradle", "sh"
)

fun isEditableTextFile(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext.isEmpty() || ext in EDITABLE_EXTENSIONS
}

private sealed interface FileManagerOperation {
    data object None : FileManagerOperation
    /** 创建文件夹时 */
    data object CreateDir : FileManagerOperation
    /** 创建文件时 */
    data object CreateFile : FileManagerOperation
    /** 重命名文件/文件夹时 */
    data class Rename(val file: File) : FileManagerOperation
    /** 删除文件/文件夹时 */
    data class Delete(val file: File) : FileManagerOperation
}

@Composable
private fun FileManagerOperation(
    operation: FileManagerOperation,
    onChange: (FileManagerOperation) -> Unit,
    currentPath: String,
    onCreateDir: (name: String) -> Unit,
    onCreateFile: (name: String) -> Unit,
    onRename: (file: File, newName: String) -> Unit,
    onDelete: (file: File) -> Unit,
) {
    when (operation) {
        FileManagerOperation.None -> {}
        FileManagerOperation.CreateDir -> {
            CreateNewDirDialog(
                onDismissRequest = { onChange(FileManagerOperation.None) },
                createDir = { onCreateDir(it) }
            )
        }
        FileManagerOperation.CreateFile -> {
            CreateNewFileDialog(
                onDismissRequest = { onChange(FileManagerOperation.None) },
                createFile = { onCreateFile(it) }
            )
        }
        is FileManagerOperation.Rename -> {
            var value by remember(operation.file) { mutableStateOf(operation.file.name) }
            val filenameInvalidMessage = isFilenameInvalid(value)
            val isError = value.isEmpty() || filenameInvalidMessage != null

            SimpleEditDialog(
                title = stringResource(R.string.file_manager_rename),
                value = value,
                onValueChange = { value = it },
                isError = isError,
                supportingText = {
                    when {
                        value.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                        filenameInvalidMessage != null -> Text(text = filenameInvalidMessage)
                    }
                },
                singleLine = true,
                onDismissRequest = { onChange(FileManagerOperation.None) },
                onConfirm = {
                    if (!isError) onRename(operation.file, value)
                }
            )
        }
        is FileManagerOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.file_manager_delete_confirm_title),
                text = stringResource(R.string.file_manager_delete_confirm_message, operation.file.name),
                onDismiss = { onChange(FileManagerOperation.None) },
                onConfirm = { onDelete(operation.file) }
            )
        }
    }
}

@Composable
fun BuiltInFileManagerScreen(
    key: NormalNavKey.BuiltInFileManager,
    backStackViewModel: ScreenBackStackViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    navigateToEditor: (path: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rootPath = remember { PathManager.DIR_FILES_EXTERNAL.absolutePath }
    var currentPath by remember { mutableStateOf(rootPath) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var operation by remember { mutableStateOf<FileManagerOperation>(FileManagerOperation.None) }

    fun refresh() {
        scope.launch {
            files = withContext(Dispatchers.IO) {
                File(currentPath).listFiles()?.toList()?.sortedWith { o1, o2 ->
                    sortWithFileName(o1, o2)
                } ?: emptyList()
            }
        }
    }

    LaunchedEffect(currentPath) {
        files = withContext(Dispatchers.IO) {
            File(currentPath).listFiles()?.toList()?.sortedWith { o1, o2 ->
                sortWithFileName(o1, o2)
            } ?: emptyList()
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val failed = mutableListOf<String>()
            uris.forEach { uri ->
                runCatching {
                    val name = context.getFileName(uri) ?: uri.lastPathSegment ?: "unknown"
                    val target = File(currentPath, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("openInputStream returned null")
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to copy uploaded file", e)
                    failed += e.message ?: e.javaClass.simpleName
                }
            }
            withContext(Dispatchers.Main) {
                if (failed.isNotEmpty()) {
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = context.getString(R.string.generic_warning),
                            message = context.getString(R.string.file_manager_upload_failed, failed.joinToString("\n"))
                        )
                    )
                }
                refresh()
            }
        }
    }

    FileManagerOperation(
        operation = operation,
        onChange = { operation = it },
        currentPath = currentPath,
        onCreateDir = { name ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    File(currentPath, name).mkdirs()
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to create folder", e)
                }
                withContext(Dispatchers.Main) {
                    operation = FileManagerOperation.None
                    refresh()
                }
            }
        },
        onCreateFile = { name ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val target = File(currentPath, name)
                    if (!target.createNewFile()) throw IllegalStateException("createNewFile returned false")
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to create file", e)
                    withContext(Dispatchers.Main) {
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.generic_warning),
                                message = context.getString(R.string.file_manager_create_file_failed, e.message ?: e.javaClass.simpleName)
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    operation = FileManagerOperation.None
                    refresh()
                }
            }
        },
        onRename = { file, newName ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val target = File(file.parentFile, newName)
                    if (!file.renameTo(target)) throw IllegalStateException("renameTo returned false")
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to rename file", e)
                    withContext(Dispatchers.Main) {
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.generic_warning),
                                message = context.getString(R.string.file_manager_rename_failed, e.message ?: e.javaClass.simpleName)
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    operation = FileManagerOperation.None
                    refresh()
                }
            }
        },
        onDelete = { file ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    if (file.isDirectory) FileUtils.deleteDirectory(file) else FileUtils.deleteQuietly(file)
                }.onFailure { e ->
                    Logger.warning(TAG, "Failed to delete file", e)
                    withContext(Dispatchers.Main) {
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.generic_warning),
                                message = context.getString(R.string.file_manager_delete_failed, e.message ?: e.javaClass.simpleName)
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    operation = FileManagerOperation.None
                    refresh()
                }
            }
        }
    )

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey,
        useClassEquality = true
    ) { isVisible ->
        Row(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize()
        ) {
            LeftActionMenu(
                isVisible = isVisible,
                createFile = { operation = FileManagerOperation.CreateFile },
                createDir = { operation = FileManagerOperation.CreateDir },
                upload = { uploadLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.5f)
            )

            FilesLayout(
                isVisible = isVisible,
                currentPath = currentPath,
                files = files,
                canGoBack = currentPath != rootPath,
                onBack = {
                    File(currentPath).parentFile?.let {
                        currentPath = it.absolutePath
                    }
                },
                onOpenFolder = { path -> currentPath = path },
                onOpenFile = { file ->
                    if (isEditableTextFile(file)) {
                        navigateToEditor(file.absolutePath)
                    }
                },
                onRename = { file -> operation = FileManagerOperation.Rename(file) },
                onDelete = { file -> operation = FileManagerOperation.Delete(file) },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7.5f)
                    .padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun LeftActionMenu(
    isVisible: Boolean,
    createFile: () -> Unit,
    createDir: () -> Unit,
    upload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceXOffset by com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Column(
        modifier = modifier
            .offset { IntOffset(x = surfaceXOffset.roundToPx(), y = 0) },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
    ) {
        ScalingActionButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = createFile
        ) {
            MarqueeText(text = stringResource(R.string.files_create_file))
        }
        ScalingActionButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = createDir
        ) {
            MarqueeText(text = stringResource(R.string.files_create_dir))
        }
        ScalingActionButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = upload
        ) {
            MarqueeText(text = stringResource(R.string.file_manager_upload))
        }
    }
}

@Composable
private fun TopPathHeader(
    path: String,
    modifier: Modifier = Modifier,
) {
    CardTitleLayout(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.files_current_path, path),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun FilesLayout(
    isVisible: Boolean,
    currentPath: String,
    files: List<File>,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceXOffset by com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    BackgroundCard(
        modifier = modifier.offset { IntOffset(x = surfaceXOffset.roundToPx(), y = 0) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopPathHeader(
                modifier = Modifier.fillMaxWidth(),
                path = currentPath
            )

            if (canGoBack || files.isNotEmpty()) {
                //每次切换目录时都重新创建滚动状态，确保列表从顶部开始显示，
                //否则从深层目录返回/切换文件夹时，列表会保留旧的滚动位置，
                //导致"返回上级"那一行和最上面的文件被滚动到看不见的地方
                val scrollState = remember(currentPath) { LazyListState() }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .scrollbar(
                            state = scrollState.scrollIndicatorState,
                            orientation = Orientation.Vertical,
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    state = scrollState,
                ) {
                    if (canGoBack) {
                        item(key = "..back..") {
                            BackFileItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                onClick = onBack
                            )
                        }
                    }
                    items(files, key = { it.absolutePath }) { file ->
                        ManagedFileItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            file = file,
                            onClick = {
                                if (file.isDirectory) onOpenFolder(file.absolutePath) else onOpenFile(file)
                            },
                            onRename = { onRename(file) },
                            onDelete = { onDelete(file) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ScalingLabel(
                        text = stringResource(R.string.file_manager_no_files)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackFileItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_folder_outlined),
                contentDescription = null
            )
            MarqueeText(
                text = stringResource(R.string.files_back_to_parent),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ManagedFileItem(
    modifier: Modifier = Modifier,
    file: File,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        BaseFileItem(
            file = file,
            modifier = Modifier.padding(all = 12.dp),
            suffix = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.file_manager_rename)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_outlined),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.file_manager_delete)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_filled),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        )
    }
}
