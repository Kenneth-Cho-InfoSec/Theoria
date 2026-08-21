/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.launch

/** A directly discoverable rename action for the viewer action bar. */
@Composable
fun <T : Media> RenameButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false
) {
    val handler = LocalMediaHandler.current
    val scope = rememberCoroutineScope()
    var showDialog by remember(media) { mutableStateOf(false) }
    var newName by remember(media) { mutableStateOf(media.label) }

    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.EditNote,
        title = stringResource(R.string.rename),
        enabled = enabled,
        followTheme = followTheme
    ) {
        newName = media.label
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label)) },
                    singleLine = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && newName != media.label,
                    onClick = {
                        val requestedName = newName.trim()
                        scope.launch {
                            if (handler.renameMedia(media, requestedName)) {
                                showDialog = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }
}
