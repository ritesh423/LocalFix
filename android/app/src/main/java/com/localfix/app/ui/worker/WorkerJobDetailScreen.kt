package com.localfix.app.ui.worker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.localfix.app.ui.components.RequestStatusBadge
import com.localfix.app.ui.components.persistPhotoAccess
import com.localfix.app.ui.components.releasePhotoAccess
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun WorkerJobDetailScreen(
    uiState: WorkerJobDetailUiState,
    onBack: () -> Unit,
    onStartJob: () -> Unit,
    onCompletionNoteChanged: (String) -> Unit,
    onPartsUsedChanged: (String) -> Unit,
    onPhotoSelected: (String) -> Unit,
    onPhotoRemoved: () -> Unit,
    onPhotoSelectionFailed: () -> Unit,
    onSubmitCompletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val job = uiState.job
    if (job == null) {
        MissingWorkerJob(onBack = onBack, modifier = modifier)
        return
    }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val previousPhotoUri = uiState.completionDraft.photoUri
            if (persistPhotoAccess(context, uri)) {
                onPhotoSelected(uri.toString())
                if (previousPhotoUri != null && previousPhotoUri != uri.toString()) {
                    releasePhotoAccess(context, previousPhotoUri)
                }
            } else {
                onPhotoSelectionFailed()
            }
        }
    }
    LaunchedEffect(uiState.hasJustSubmittedCompletion) {
        if (uiState.hasJustSubmittedCompletion) {
            uiState.completionDraft.photoUri?.let { releasePhotoAccess(context, it) }
            onPhotoRemoved()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("worker-job-detail"),
    ) {
        item { WorkerJobHeader(job = job, onBack = onBack) }
        job.reworkReason?.let { reason ->
            item { ReworkRequestCard(reason) }
        }
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
        } else if (job.canSubmitCompletion) {
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
                            text = if (uiState.hasJustStarted) "Work started" else job.statusLabel,
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
            item {
                CompletionEvidenceForm(
                    uiState = uiState,
                    onCompletionNoteChanged = onCompletionNoteChanged,
                    onPartsUsedChanged = onPartsUsedChanged,
                    onChoosePhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly),
                        )
                    },
                    onRemovePhoto = {
                        uiState.completionDraft.photoUri?.let {
                            releasePhotoAccess(context, it)
                        }
                        onPhotoRemoved()
                    },
                    onSubmit = onSubmitCompletion,
                )
            }
        } else {
            item {
                SubmittedCompletionCard(job = job, justSubmitted = uiState.hasJustSubmittedCompletion)
            }
        }
        item { WorkerHistorySection(uiState) }
        item { Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge)) }
    }
}

@Composable
private fun ReworkRequestCard(reason: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LocalFixSpacing.medium)
            .testTag("worker-rework-reason"),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(LocalFixRadius.large),
    ) {
        Row(
            modifier = Modifier.padding(LocalFixSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.medium),
        ) {
            Icon(imageVector = Icons.Outlined.Replay, contentDescription = null)
            Column {
                Text(
                    text = "Resident requested more work",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = reason,
                    modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun WorkerHistorySection(uiState: WorkerJobDetailUiState) {
    WorkerDetailSection(title = "Job activity") {
        when {
            uiState.isHistoryLoading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Loading job activity…")
                }
            }
            uiState.historyError != null -> {
                Text(
                    text = uiState.historyError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.history.isEmpty() -> {
                Text(
                    text = "No activity has been recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> {
                Column(
                    modifier = Modifier.testTag("worker-history"),
                    verticalArrangement = Arrangement.spacedBy(LocalFixSpacing.medium),
                ) {
                    uiState.history.asReversed().forEach { event ->
                        WorkerHistoryRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerHistoryRow(event: WorkerHistoryItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.padding(LocalFixSpacing.small),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(event.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "v${event.ticketVersion}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = "${event.statusLabel} · ${event.timeLabel}",
                modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            event.detail?.let { detail ->
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CompletionEvidenceForm(
    uiState: WorkerJobDetailUiState,
    onCompletionNoteChanged: (String) -> Unit,
    onPartsUsedChanged: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = LocalFixSpacing.medium,
            vertical = LocalFixSpacing.small,
        ),
    ) {
        Text("Completion evidence", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Show what was repaired before sending it to the resident.",
            modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = uiState.completionDraft.completionNote,
            onValueChange = onCompletionNoteChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalFixSpacing.medium)
                .testTag("worker-completion-note"),
            label = { Text("Work completed") },
            placeholder = { Text("Replaced the worn washer and tested the tap") },
            supportingText = {
                Text(
                    uiState.completionErrors.completionNote
                        ?: "${uiState.completionDraft.completionNote.length}/500",
                )
            },
            isError = uiState.completionErrors.completionNote != null,
            minLines = 3,
            maxLines = 5,
        )
        OutlinedTextField(
            value = uiState.completionDraft.partsUsed,
            onValueChange = onPartsUsedChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalFixSpacing.small)
                .testTag("worker-parts-used"),
            label = { Text("Parts used (optional)") },
            placeholder = { Text("Rubber washer, thread seal tape") },
            supportingText = {
                Text(
                    uiState.completionErrors.partsUsed
                        ?: "Separate multiple parts with commas.",
                )
            },
            isError = uiState.completionErrors.partsUsed != null,
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        CompletionPhotoPicker(
            photoUri = uiState.completionDraft.photoUri,
            error = uiState.completionErrors.photo,
            onChoosePhoto = onChoosePhoto,
            onRemovePhoto = onRemovePhoto,
        )
        uiState.completionSubmissionError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = LocalFixSpacing.small),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = !uiState.isSubmittingCompletion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalFixSpacing.medium)
                .testTag("worker-submit-completion"),
            shape = RoundedCornerShape(LocalFixRadius.medium),
        ) {
            if (uiState.isSubmittingCompletion) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Send for resident confirmation")
            }
        }
    }
}

@Composable
private fun CompletionPhotoPicker(
    photoUri: String?,
    error: String?,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    if (photoUri == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(LocalFixSpacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.padding(LocalFixSpacing.small),
                    )
                }
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text("Add an after-repair photo", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onChoosePhoto,
                    modifier = Modifier
                        .padding(top = LocalFixSpacing.small)
                        .testTag("choose-completion-photo"),
                ) {
                    Text("Choose photo")
                }
            }
        }
    } else {
        Column {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = "Selected completion photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .testTag("completion-photo-preview"),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "After-repair photo attached",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(onClick = onChoosePhoto) { Text("Replace") }
                IconButton(onClick = onRemovePhoto) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove completion photo",
                    )
                }
            }
        }
    }
    error?.let {
        Text(
            text = it,
            modifier = Modifier.padding(top = LocalFixSpacing.small),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SubmittedCompletionCard(
    job: WorkerJobDetail,
    justSubmitted: Boolean,
) {
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
                text = if (justSubmitted) "Sent to the resident" else job.statusLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (job.statusLabel == "Completed") {
                    "The resident confirmed that this repair is complete."
                } else {
                    "The resident can now review and confirm the repair."
                },
                modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                style = MaterialTheme.typography.bodyMedium,
            )
            job.completionNote?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = LocalFixSpacing.medium),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (job.partsUsed.isNotEmpty()) {
                Text(
                    text = "Parts: ${job.partsUsed.joinToString()}",
                    modifier = Modifier.padding(top = LocalFixSpacing.small),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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
                    canSubmitCompletion = false,
                    completionNote = null,
                    partsUsed = emptyList(),
                    hasCompletionPhoto = false,
                    reworkReason = "The lower pipe joint is still dripping.",
                ),
                history = listOf(
                    WorkerHistoryItem(
                        id = "event-1",
                        title = "Resident requested more work",
                        detail = "The lower pipe joint is still dripping.",
                        statusLabel = "Ready to start",
                        timeLabel = "Updated 12 min ago",
                        ticketVersion = 5,
                    ),
                ),
            ),
            onBack = {},
            onStartJob = {},
            onCompletionNoteChanged = {},
            onPartsUsedChanged = {},
            onPhotoSelected = {},
            onPhotoRemoved = {},
            onPhotoSelectionFailed = {},
            onSubmitCompletion = {},
        )
    }
}
