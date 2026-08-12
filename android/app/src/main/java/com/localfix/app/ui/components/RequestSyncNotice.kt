package com.localfix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing

sealed interface RequestLoadUiState {
    data object Loading : RequestLoadUiState
    data object Refreshing : RequestLoadUiState
    data object Content : RequestLoadUiState
    data object Empty : RequestLoadUiState
    data class Stale(val message: String) : RequestLoadUiState
    data class Failed(val message: String) : RequestLoadUiState
}

@Composable
fun RequestSyncNotice(
    state: RequestLoadUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RequestLoadUiState.Refreshing -> Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.medium),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(LocalFixSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Refreshing requests…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is RequestLoadUiState.Stale -> Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(LocalFixRadius.medium),
        ) {
            Column(
                modifier = Modifier.padding(LocalFixSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
            ) {
                Text(
                    "${state.message} Showing requests loaded earlier.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onRetry) {
                    Text("Try again")
                }
            }
        }

        else -> Unit
    }
}

@Composable
fun RequestStatePanel(
    state: RequestLoadUiState,
    emptyTitle: String,
    emptyMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val panelContent = when (state) {
        RequestLoadUiState.Loading -> RequestPanelContent.Loading
        RequestLoadUiState.Empty -> RequestPanelContent.Empty(emptyTitle, emptyMessage)
        is RequestLoadUiState.Failed -> RequestPanelContent.Failed(state.message)
        else -> return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = LocalFixSpacing.large,
                vertical = LocalFixSpacing.extraLarge,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
        ) {
            when (panelContent) {
                RequestPanelContent.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text("Loading your requests…", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This should only take a moment.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is RequestPanelContent.Empty -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(panelContent.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        panelContent.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is RequestPanelContent.Failed -> {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text("Requests couldn't be loaded", style = MaterialTheme.typography.titleMedium)
                    Text(
                        panelContent.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onRetry) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

private sealed interface RequestPanelContent {
    data object Loading : RequestPanelContent
    data class Empty(val title: String, val message: String) : RequestPanelContent
    data class Failed(val message: String) : RequestPanelContent
}
