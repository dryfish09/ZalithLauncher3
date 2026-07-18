package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.wardrobe.AccountCapeCollection
import com.movtery.zalithlauncher.game.account.wardrobe.CapeEntry
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import java.io.File

@Composable
fun CapeSelectorDialog(
    accountUUID: String,
    onDismiss: () -> Unit,
    onCapeActivated: () -> Unit,
    onCapeDeleted: () -> Unit = {}
) {
    var manifest by remember(accountUUID) { mutableStateOf(AccountCapeCollection.loadManifest(accountUUID)) }
    val sortedCapes = remember(manifest) {
        manifest.capes.sortedByDescending { it.favorite }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxSize()
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_capes_select_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (sortedCapes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.account_capes_no_capes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortedCapes, key = { it.id }) { entry ->
                                CapeEntryCard(
                                    accountUUID = accountUUID,
                                    entry = entry,
                                    isActive = entry.id == manifest.activeCapeId,
                                    onActivate = {
                                        AccountCapeCollection.setActiveCape(accountUUID, entry.id)
                                        manifest = AccountCapeCollection.loadManifest(accountUUID)
                                        onCapeActivated()
                                    },
                                    onToggleFavorite = {
                                        AccountCapeCollection.toggleFavorite(accountUUID, entry.id)
                                        manifest = AccountCapeCollection.loadManifest(accountUUID)
                                    },
                                    onRename = { newName ->
                                        AccountCapeCollection.renameCape(accountUUID, entry.id, newName)
                                        manifest = AccountCapeCollection.loadManifest(accountUUID)
                                    },
                                    onDelete = {
                                        AccountCapeCollection.removeCape(accountUUID, entry.id)
                                        manifest = AccountCapeCollection.loadManifest(accountUUID)
                                        onCapeDeleted()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.generic_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun CapeEntryCard(
    accountUUID: String,
    entry: CapeEntry,
    isActive: Boolean,
    onActivate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var editName by remember(entry.name) { mutableStateOf(entry.name) }

    val capeFile = File(AccountCapeCollection.getCollectionDir(accountUUID), "${entry.id}.${entry.ext}")

    SettingsCard(
        modifier = Modifier.fillMaxWidth(),
        position = CardPosition.Single,
        onClick = {
            if (!editing) onActivate()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(capeFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (editing) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.account_capes_rename_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onRename(editName)
                                editing = false
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            editName = entry.name
                            editing = true
                        }
                    )
                }

                Text(
                    text = if (isActive) "● Active" else "● ${entry.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (entry.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (entry.favorite) R.string.account_capes_unfavorite
                        else R.string.account_capes_favorite
                    ),
                    tint = if (entry.favorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.generic_delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
