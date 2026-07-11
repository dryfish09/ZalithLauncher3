package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.ui.components.MarqueeText

sealed interface GameFolderOperation {
    data object None : GameFolderOperation
    data object SelectDefault : GameFolderOperation
}

@Composable
fun GameFolderOperationDialog(
    operation: GameFolderOperation,
    changeState: (GameFolderOperation) -> Unit,
    onSelectDefaultFolder: (String, String) -> Unit = { _, _ -> }
) {
    when (operation) {
        is GameFolderOperation.None -> {}
        is GameFolderOperation.SelectDefault -> SelectDefaultFolderDialog(
            onSelect = { id, path ->
                changeState(GameFolderOperation.None)
                onSelectDefaultFolder(id, path)
            },
            onDismiss = { changeState(GameFolderOperation.None) }
        )
    }
}

@Composable
private fun SelectDefaultFolderDialog(
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val gamePaths by GamePathManager.gamePathData.collectAsStateWithLifecycle()
    val currentPath by GamePathManager.currentPath.collectAsStateWithLifecycle()
    var selectedPath by remember { mutableStateOf(currentPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.versions_manage_select_default_folder_title)) },
        text = {
            LazyColumn {
                items(gamePaths) { path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPath == path.path,
                            onClick = { selectedPath = path.path }
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = path.title.ifEmpty { stringResource(R.string.generic_default) },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val selectedItem = gamePaths.find { it.path == selectedPath }
                val id = selectedItem?.id ?: GamePathManager.DEFAULT_ID
                val path = selectedItem?.path ?: selectedPath
                onSelect(id, path)
            }) {
                MarqueeText(text = stringResource(R.string.generic_select))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                MarqueeText(text = stringResource(R.string.generic_cancel))
            }
        }
    )
}
