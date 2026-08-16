package com.localfix.app.ui.worker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.components.LocalFixScreenHeader
import com.localfix.app.ui.components.RequestLoadUiState
import com.localfix.app.ui.components.RequestStatePanel
import com.localfix.app.ui.components.RequestStatusBadge
import com.localfix.app.ui.components.RequestSyncNotice
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun WorkerQueueScreen(
    uiState: WorkerQueueUiState,
    onJobClick: (String) -> Unit,
    onRetry: () -> Unit,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onSwitchRole)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("worker-queue"),
    ) {
        item {
            LocalFixScreenHeader(
                eyebrow = "${uiState.propertyName} · ${uiState.workerName}",
                title = "My jobs",
                subtitle = "See where to go and what needs attention.",
            )
        }
        item {
            WorkerMetrics(
                ready = uiState.readyCount,
                inProgress = uiState.inProgressCount,
                modifier = Modifier.padding(LocalFixSpacing.medium),
            )
        }
        item {
            Text(
                text = "Assigned work",
                modifier = Modifier.padding(
                    start = LocalFixSpacing.large,
                    end = LocalFixSpacing.large,
                    bottom = LocalFixSpacing.small,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (uiState.loadState is RequestLoadUiState.Refreshing ||
            uiState.loadState is RequestLoadUiState.Stale
        ) {
            item {
                RequestSyncNotice(
                    state = uiState.loadState,
                    onRetry = onRetry,
                    modifier = Modifier.padding(
                        horizontal = LocalFixSpacing.medium,
                        vertical = LocalFixSpacing.extraSmall,
                    ),
                )
            }
        }
        if (uiState.loadState is RequestLoadUiState.Loading ||
            uiState.loadState is RequestLoadUiState.Failed ||
            uiState.jobs.isEmpty()
        ) {
            item {
                RequestStatePanel(
                    state = when {
                        uiState.loadState is RequestLoadUiState.Loading -> uiState.loadState
                        uiState.loadState is RequestLoadUiState.Failed -> uiState.loadState
                        else -> RequestLoadUiState.Empty
                    },
                    emptyTitle = "No assigned jobs",
                    emptyMessage = "Jobs assigned by your property manager will appear here.",
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = LocalFixSpacing.medium),
                )
            }
        } else {
            items(items = uiState.jobs, key = WorkerJobItem::id) { job ->
                WorkerJobCard(
                    job = job,
                    onClick = { onJobClick(job.id) },
                    modifier = Modifier.padding(
                        horizontal = LocalFixSpacing.medium,
                        vertical = LocalFixSpacing.extraSmall,
                    ),
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onSwitchRole,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalFixSpacing.medium),
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                Text("Switch workspace")
            }
        }
        item { Spacer(modifier = Modifier.height(LocalFixSpacing.large)) }
    }
}

@Composable
private fun WorkerMetrics(
    ready: Int,
    inProgress: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
    ) {
        WorkerMetric(
            value = ready.toString(),
            label = "Ready to start",
            modifier = Modifier.weight(1f),
        )
        WorkerMetric(
            value = inProgress.toString(),
            label = "In progress",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WorkerMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(LocalFixSpacing.medium)) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun WorkerJobCard(
    job: WorkerJobItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("worker-job-${job.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LocalFixSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = job.reference,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = job.unitLabel,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    RequestStatusBadge(label = job.statusLabel, tone = job.statusTone)
                }
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(text = job.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = "${job.categoryLabel} · ${job.priorityLabel} priority",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = job.accessWindowLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = job.updatedLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WorkerQueuePreview() {
    LocalFixTheme(darkTheme = false) {
        WorkerQueueScreen(
            uiState = WorkerQueueUiState(
                workerName = "Arun Kumar",
                propertyName = "Lakeview Residency",
                readyCount = 1,
                inProgressCount = 0,
                jobs = listOf(
                    WorkerJobItem(
                        id = "1",
                        reference = "LF-90000000",
                        unitLabel = "Apartment A-204",
                        title = "Bathroom pipe is leaking",
                        categoryLabel = "Plumbing",
                        priorityLabel = "Soon",
                        accessWindowLabel = "Morning · 8 AM–12 PM",
                        statusLabel = "Ready to start",
                        statusTone = RequestStatusTone.ACTIVE,
                        updatedLabel = "Updated 12 min ago",
                    ),
                ),
                loadState = RequestLoadUiState.Content,
            ),
            onJobClick = {},
            onRetry = {},
            onSwitchRole = {},
        )
    }
}
