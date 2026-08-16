package com.localfix.app.ui.worker

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.components.RequestStatusBadge
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun WorkerJobDetailScreen(
    uiState: WorkerJobDetailUiState,
    onBack: () -> Unit,
    onStartJob: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val job = uiState.job
    if (job == null) {
        MissingWorkerJob(onBack = onBack, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("worker-job-detail"),
    ) {
        item { WorkerJobHeader(job = job, onBack = onBack) }
        item {
            WorkerDetailSection(title = "Issue details") {
                WorkerInformationRow(
                    icon = Icons.Outlined.Build,
                    label = "Service",
                    value = job.categoryLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                Text(
                    text = "Description",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(text = job.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            WorkerDetailSection(title = "Visit plan") {
                WorkerInformationRow(
                    icon = Icons.Outlined.Flag,
                    label = "Manager priority",
                    value = job.priorityLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                WorkerInformationRow(
                    icon = Icons.Outlined.PriorityHigh,
                    label = "Resident suggested",
                    value = job.residentUrgencyLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                WorkerInformationRow(
                    icon = Icons.Outlined.AccessTime,
                    label = "Access window",
                    value = job.accessWindowLabel,
                )
            }
        }
        uiState.startError?.let { message ->
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
        if (job.canStart) {
            item {
                Column(modifier = Modifier.padding(LocalFixSpacing.medium)) {
                    Text(
                        text = "Start when you arrive",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Starting the job tells the manager and resident that work is now in progress.",
                        modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onStartJob,
                        enabled = !uiState.isStarting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LocalFixSpacing.medium)
                            .testTag("worker-start-job"),
                        shape = RoundedCornerShape(LocalFixRadius.medium),
                    ) {
                        if (uiState.isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Start work")
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LocalFixSpacing.medium),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(LocalFixRadius.large),
                ) {
                    Column(modifier = Modifier.padding(LocalFixSpacing.medium)) {
                        Text(
                            text = if (uiState.hasJustStarted) {
                                "Work started"
                            } else {
                                job.statusLabel
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "The property team can now see the latest job status.",
                            modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge)) }
    }
}

@Composable
private fun WorkerJobHeader(
    job: WorkerJobDetail,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = LocalFixSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${job.reference} · ${job.unitLabel}",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelLarge,
            )
            RequestStatusBadge(label = job.statusLabel, tone = job.statusTone)
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text(
            text = job.title,
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun WorkerDetailSection(
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
private fun WorkerInformationRow(
    icon: ImageVector,
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
private fun MissingWorkerJob(
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
            text = "Job not found",
            modifier = Modifier.padding(top = LocalFixSpacing.large),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Return to your queue and refresh your assigned jobs.",
            modifier = Modifier.padding(top = LocalFixSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WorkerJobDetailPreview() {
    LocalFixTheme(darkTheme = false) {
        WorkerJobDetailScreen(
            uiState = WorkerJobDetailUiState(
                job = WorkerJobDetail(
                    id = "1",
                    reference = "LF-90000000",
                    unitLabel = "Apartment A-204",
                    title = "Bathroom pipe is leaking",
                    description = "Water is collecting below the bathroom washbasin pipe.",
                    categoryLabel = "Plumbing",
                    residentUrgencyLabel = "Soon",
                    priorityLabel = "Soon",
                    accessWindowLabel = "Morning · 8 AM–12 PM",
                    statusLabel = "Ready to start",
                    statusTone = RequestStatusTone.ACTIVE,
                    version = 2,
                    canStart = true,
                ),
            ),
            onBack = {},
            onStartJob = {},
        )
    }
}
