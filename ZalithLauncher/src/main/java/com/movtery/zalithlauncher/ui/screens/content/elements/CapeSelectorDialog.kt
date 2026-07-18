package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clipToBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.wardrobe.AccountCapeCollection
import com.movtery.zalithlauncher.game.account.wardrobe.CapeEntry
import com.movtery.zalithlauncher.ui.components.ModelAnimation
import com.movtery.zalithlauncher.ui.components.PlayerSkin
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
    val context = LocalContext.current
    var manifest by remember(accountUUID) { mutableStateOf(AccountCapeCollection.loadManifest(accountUUID)) }
    val sortedCapes = remember(manifest) {
        manifest.capes.sortedByDescending { it.favorite }
    }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    val playerSkin = remember { PlayerSkin(context) }
    DisposableEffect(Unit) {
        onDispose { playerSkin.destroy() }
    }

    var previewReady by remember { mutableStateOf(false) }
    val activeEntry = remember(manifest) {
        manifest.activeCapeId?.let { id -> manifest.capes.find { it.id == id } }
    }

    LaunchedEffect(activeEntry) {
        if (previewReady) {
            if (activeEntry != null) {
                val capeFile = File(AccountCapeCollection.getCollectionDir(accountUUID), "${activeEntry.id}.${activeEntry.ext}")
                runCatching {
                    capeFile.inputStream().use { stream ->
                        playerSkin.loadCape(stream)
                    }
                }
            } else {
                playerSkin.loadCape(inputStream = null)
            }
        }
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clipToBounds()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                playerSkin.loadWebView(
                                    context = ctx,
                                    onPageFinished = {
                                        previewReady = true
                                        playerSkin.startAnim(ModelAnimation.NewIdle)
                                        playerSkin.setAzimuthAndPitch(180, 5, 50)
                                        if (activeEntry != null) {
                                            val capeFile = File(AccountCapeCollection.getCollectionDir(accountUUID), "${activeEntry.id}.${activeEntry.ext}")
                                            runCatching {
                                                capeFile.inputStream().use { stream ->
                                                    playerSkin.loadCape(stream)
                                                }
                                            }
                                        }
                                    }
                                ).apply {
                                    isClickable = false
                                    isFocusable = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val hasActiveCape = manifest.activeCapeId != null

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "__no_cape__") {
                            CapeEntryCard(
                                accountUUID = accountUUID,
                                entry = null,
                                isActive = !hasActiveCape,
                                onActivate = {
                                    AccountCapeCollection.clearActiveCape(accountUUID)
                                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                                    onCapeActivated()
                                },
                                onToggleFavorite = {},
                                onRename = {},
                                onDelete = {}
                            )
                        }

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
                                    confirmDeleteId = entry.id
                                }
                            )
                        }

                        if (sortedCapes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_capes_no_capes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
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

    confirmDeleteId?.let { id ->
        val entry = manifest.capes.find { it.id == id }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.account_capes_delete_title)) },
            text = {
                Text(stringResource(R.string.account_capes_delete_message, entry?.name ?: ""))
            },
            confirmButton = {
                Button(onClick = {
                    AccountCapeCollection.removeCape(accountUUID, id)
                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                    confirmDeleteId = null
                    onCapeDeleted()
                }) {
                    Text(stringResource(R.string.generic_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDeleteId = null }) {
                    Text(stringResource(R.string.generic_cancel))
                }
            }
        )
    }
}

@Composable
private fun CapeEntryCard(
    accountUUID: String,
    entry: CapeEntry?,
    isActive: Boolean,
    onActivate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isNoCape = entry == null
    var editing by remember { mutableStateOf(false) }
    var editName by remember(entry?.name) { mutableStateOf(entry?.name ?: "") }

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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isNoCape) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                } else {
                    val capeFile = File(AccountCapeCollection.getCollectionDir(accountUUID), "${entry.id}.${entry.ext}")
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
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isNoCape) stringResource(R.string.account_capes_none)
                    else entry!!.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (!isNoCape) Modifier.clickable {
                        editName = entry.name
                        editing = true
                    } else Modifier
                )

                if (!isNoCape && editing) {
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
                }

                Text(
                    text = if (isActive) "● Active" else if (isNoCape) "" else "● ${entry.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (!isNoCape) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry!!.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
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
}
