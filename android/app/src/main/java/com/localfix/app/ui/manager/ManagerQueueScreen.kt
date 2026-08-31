package com.localfix.app.ui.manager

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
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
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
fun ManagerQueueScreen(
    uiState: ManagerQueueUiState,
    onTicketClick: (String) -> Unit,
    onRetry: () -> Unit,
    onInviteResident: () -> Unit,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onSwitchRole)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("manager-queue"),
    ) {
        item {
            LocalFixScreenHeader(
                eyebrow = uiState.propertyName,
                title = "Property queue",
                subtitle = "Prioritize requests and send the right worker.",
            )
        }
        item {
            QueueMetrics(
                summary = uiState.summary,
                modifier = Modifier.padding(LocalFixSpacing.medium),
            )
        }
        item {
            Button(
                onClick = onInviteResident,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        bottom = LocalFixSpacing.large,
                    )
                    .height(52.dp)
                    .testTag("manager-invite-resident"),
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                Text(
                    text = "Invite a resident",
                    modifier = Modifier.padding(start = LocalFixSpacing.small),
                )
            }
        }
        item {
            Text(
                text = "Maintenance requests",
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
            uiState.tickets.isEmpty()
        ) {
            item {
                RequestStatePanel(
                    state = when {
                        uiState.loadState is RequestLoadUiState.Loading -> uiState.loadState
                        uiState.loadState is RequestLoadUiState.Failed -> uiState.loadState
                        else -> RequestLoadUiState.Empty
                    },
                    emptyTitle = "The queue is clear",
                    emptyMessage = "New resident requests will appear here.",
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = LocalFixSpacing.medium),
                )
            }
        } else {
            items(
                items = uiState.tickets,
                key = ManagerTicketItem::id,
            ) { ticket ->
                ManagerTicketCard(
                    ticket = ticket,
                    onClick = { onTicketClick(ticket.id) },
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
private fun QueueMetrics(
    summary: ManagerSummaryUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${summary.activeRequests} active requests",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
        MetricRow(
            leftValue = summary.needsAssignment,
            leftLabel = "Need assignment",
            rightValue = summary.assigned,
            rightLabel = "Assigned",
        )
        MetricRow(
            leftValue = summary.inProgress,
            leftLabel = "In progress",
            rightValue = summary.blocked,
            rightLabel = "Blocked",
        )
        MetricRow(
            leftValue = summary.awaitingConfirmation,
            leftLabel = "Resident review",
            rightValue = summary.completed,
            rightLabel = "Completed",
        )
    }
}

@Composable
private fun MetricRow(
    leftValue: Int,
    leftLabel: String,
    rightValue: Int,
    rightLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LocalFixSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
    ) {
        QueueMetric(
            value = leftValue.toString(),
            label = leftLabel,
            modifier = Modifier.weight(1f),
        )
        QueueMetric(
            value = rightValue.toString(),
            label = rightLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QueueMetric(
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
private fun ManagerTicketCard(
    ticket: ManagerTicketItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("manager-ticket-${ticket.id}"),
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
                            text = ticket.reference,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = ticket.unitLabel,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    RequestStatusBadge(
                        label = ticket.statusLabel,
                        tone = ticket.statusTone,
                    )
                }
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(text = ticket.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = "${ticket.categoryLabel} · Resident suggested ${ticket.urgencyLabel.lowercase()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = ticket.assignedWorker?.let { "Assigned to $it" }
                        ?: "Awaiting worker assignment",
                    color = if (ticket.assignedWorker == null) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = ticket.updatedLabel,
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
private fun ManagerQueuePreview() {
    LocalFixTheme(darkTheme = false) {
        ManagerQueueScreen(
            uiState = ManagerQueueUiState(
                propertyName = "Lakeview Residency",
                summary = ManagerSummaryUiState(
                    activeRequests = 2,
                    needsAssignment = 1,
                    assigned = 1,
                    inProgress = 0,
                    blocked = 0,
                    awaitingConfirmation = 0,
                    completed = 0,
                ),
                tickets = listOf(
                    ManagerTicketItem(
                        id = "1",
                        reference = "LF-90000000",
                        unitLabel = "Apartment A-204",
                        title = "Bathroom pipe is leaking",
                        categoryLabel = "Plumbing",
                        urgencyLabel = "Soon",
                        statusLabel = "Needs assignment",
                        statusTone = RequestStatusTone.ACTIVE,
                        assignedWorker = null,
                        updatedLabel = "Updated 12 min ago",
                    ),
                ),
                loadState = RequestLoadUiState.Content,
            ),
            onTicketClick = {},
            onRetry = {},
            onInviteResident = {},
            onSwitchRole = {},
        )
    }
}
