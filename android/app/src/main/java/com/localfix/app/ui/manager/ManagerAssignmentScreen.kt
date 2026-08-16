package com.localfix.app.ui.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.data.manager.ManagerPriority
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun ManagerAssignmentScreen(
    uiState: ManagerAssignmentUiState,
    onBack: () -> Unit,
    onPrioritySelected: (ManagerPriority) -> Unit,
    onWorkerSelected: (String) -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ticket = uiState.ticket
    if (ticket == null) {
        MissingManagerTicket(onBack = onBack, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("manager-assignment"),
    ) {
        item { AssignmentHeader(ticket = ticket, onBack = onBack) }
        item {
            ManagerSection(title = "Issue details") {
                InformationRow(
                    icon = Icons.Outlined.Build,
                    label = "Service",
                    value = ticket.categoryLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                Text(
                    text = "Description",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(text = ticket.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            ManagerSection(title = "Resident preference") {
                InformationRow(
                    icon = Icons.Outlined.PriorityHigh,
                    label = "Suggested urgency",
                    value = ticket.urgencyLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                InformationRow(
                    icon = Icons.Outlined.AccessTime,
                    label = "Access window",
                    value = ticket.accessWindowLabel,
                )
            }
        }
        if (ticket.canBeAssigned) {
            item {
                Column(
                    modifier = Modifier.padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        top = LocalFixSpacing.large,
                    ),
                ) {
                    Text("Set final priority", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The resident's urgency is a suggestion. The manager makes the final call.",
                        modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.padding(top = LocalFixSpacing.small),
                        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
                    ) {
                        ManagerPriority.entries.forEach { priority ->
                            FilterChip(
                                selected = uiState.selectedPriority == priority,
                                onClick = { onPrioritySelected(priority) },
                                label = { Text(priority.label) },
                                modifier = Modifier.testTag(
                                    "manager-priority-${priority.name.lowercase()}",
                                ),
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Choose a worker",
                    modifier = Modifier.padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        top = LocalFixSpacing.large,
                        bottom = LocalFixSpacing.small,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(items = uiState.workers, key = ManagerWorkerItem::id) { worker ->
                WorkerChoice(
                    worker = worker,
                    selected = uiState.selectedWorkerId == worker.id,
                    onClick = { onWorkerSelected(worker.id) },
                    modifier = Modifier.padding(
                        horizontal = LocalFixSpacing.medium,
                        vertical = LocalFixSpacing.extraSmall,
                    ),
                )
            }
            uiState.errorMessage?.let { message ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LocalFixSpacing.medium),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(LocalFixRadius.medium),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(LocalFixSpacing.medium),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = onAssign,
                    enabled = uiState.canAssign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LocalFixSpacing.medium)
                        .testTag("manager-assign-ticket"),
                    shape = RoundedCornerShape(LocalFixRadius.medium),
                ) {
                    if (uiState.isAssigning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Assign request")
                    }
                }
            }
        } else {
            item {
                ManagerSection(title = "Assignment") {
                    InformationRow(
                        icon = Icons.Outlined.AssignmentInd,
                        label = ticket.statusLabel,
                        value = ticket.assignedWorker ?: "Worker details unavailable",
                    )
                    Text(
                        text = "This request has already moved beyond the assignment step.",
                        modifier = Modifier.padding(top = LocalFixSpacing.medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge)) }
    }
}

@Composable
private fun AssignmentHeader(
    ticket: ManagerAssignmentTicket,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    bottomStart = LocalFixRadius.extraLarge,
                    bottomEnd = LocalFixRadius.extraLarge,
                ),
            )
            .statusBarsPadding()
            .padding(
                start = LocalFixSpacing.small,
                end = LocalFixSpacing.large,
                bottom = LocalFixSpacing.extraLarge,
            ),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = "${ticket.reference} · ${ticket.unitLabel}",
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text(
            text = ticket.title,
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = ticket.statusLabel,
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ManagerSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LocalFixSpacing.medium,
                end = LocalFixSpacing.medium,
                top = LocalFixSpacing.large,
            ),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(LocalFixSpacing.medium)) { content() }
        }
    }
}

@Composable
private fun InformationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.medium),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(LocalFixRadius.medium),
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WorkerChoice(
    worker: ManagerWorkerItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("manager-worker-${worker.id}"),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(LocalFixRadius.medium),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(LocalFixSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
                ) {
                    Text(worker.name, style = MaterialTheme.typography.titleMedium)
                    if (worker.isRecommended) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(LocalFixRadius.small),
                        ) {
                            Text(
                                text = "Best match",
                                modifier = Modifier.padding(
                                    horizontal = LocalFixSpacing.small,
                                    vertical = LocalFixSpacing.extraSmall,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Text(
                    text = worker.specialtyLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MissingManagerTicket(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(LocalFixSpacing.medium),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
            )
        }
        Text(
            text = "Request not found",
            modifier = Modifier.padding(top = LocalFixSpacing.large),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Return to the queue and refresh the latest requests.",
            modifier = Modifier.padding(top = LocalFixSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ManagerAssignmentPreview() {
    LocalFixTheme(darkTheme = false) {
        ManagerAssignmentScreen(
            uiState = ManagerAssignmentUiState(
                ticket = ManagerAssignmentTicket(
                    id = "1",
                    reference = "LF-90000000",
                    unitLabel = "Apartment A-204",
                    title = "Bathroom pipe is leaking",
                    description = "Water is collecting below the washbasin pipe.",
                    categoryLabel = "Plumbing",
                    urgencyLabel = "Soon",
                    accessWindowLabel = "Morning · 8 AM–12 PM",
                    statusLabel = "Needs assignment",
                    assignedWorker = null,
                    canBeAssigned = true,
                    version = 1,
                ),
                workers = listOf(
                    ManagerWorkerItem(
                        id = "worker-1",
                        name = "Arun Kumar",
                        specialty = ServiceCategory.PLUMBING,
                        specialtyLabel = "Plumbing",
                        isRecommended = true,
                    ),
                ),
            ),
            onBack = {},
            onPrioritySelected = {},
            onWorkerSelected = {},
            onAssign = {},
        )
    }
}
