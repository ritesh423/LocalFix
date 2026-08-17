package com.localfix.app.ui.requestdetail

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
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.localfix.app.data.resident.ResidentReviewDecision
import com.localfix.app.ui.components.RequestStatusBadge
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun ResidentRequestDetailScreen(
    uiState: ResidentRequestDetailUiState?,
    onBack: () -> Unit,
    onReviewDecisionSelected: (ResidentReviewDecision) -> Unit,
    onRatingSelected: (Int) -> Unit,
    onFeedbackChanged: (String) -> Unit,
    onSubmitReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState == null) {
        MissingRequestScreen(onBack = onBack, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("request-detail"),
    ) {
        item {
            DetailHeader(uiState = uiState, onBack = onBack)
        }
        uiState.photoUri?.let { photoUri ->
            item {
                DetailSection(title = "Photo evidence") {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Photo attached to ${uiState.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(LocalFixRadius.medium)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        if (uiState.completionNote != null) {
            item {
                DetailSection(title = "Worker completion") {
                    uiState.completionPhotoUrl?.let { photoUrl ->
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "After-repair photo for ${uiState.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(LocalFixRadius.medium))
                                .testTag("resident-completion-photo"),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                    }
                    Text(
                        text = "Work completed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = uiState.completionNote,
                        modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (uiState.partsUsed.isNotEmpty()) {
                        Text(
                            text = "Parts used",
                            modifier = Modifier.padding(top = LocalFixSpacing.medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = uiState.partsUsed.joinToString(),
                            modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        item {
            DetailSection(title = "Issue details") {
                DetailRow(
                    icon = Icons.Outlined.Build,
                    label = "Service",
                    value = uiState.categoryLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                Text(
                    text = "Description",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = uiState.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item {
            DetailSection(title = "Visit preferences") {
                DetailRow(
                    icon = Icons.Outlined.PriorityHigh,
                    label = "Suggested urgency",
                    value = uiState.urgencyLabel,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                DetailRow(
                    icon = Icons.Outlined.AccessTime,
                    label = "Access window",
                    value = uiState.accessWindowLabel,
                )
            }
        }
        item {
            DetailSection(title = "Assignment") {
                DetailRow(
                    icon = Icons.Outlined.AssignmentInd,
                    label = "Assigned worker",
                    value = uiState.assignedWorker,
                )
            }
        }
        if (uiState.canReview) {
            item {
                ResidentReviewForm(
                    review = uiState.review,
                    onDecisionSelected = onReviewDecisionSelected,
                    onRatingSelected = onRatingSelected,
                    onFeedbackChanged = onFeedbackChanged,
                    onSubmit = onSubmitReview,
                )
            }
        } else if (uiState.residentRating != null || uiState.residentFeedback != null) {
            item { ResidentReviewSummary(uiState) }
        }
        item {
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
        }
    }
}

@Composable
private fun ResidentReviewForm(
    review: ResidentReviewUiState,
    onDecisionSelected: (ResidentReviewDecision) -> Unit,
    onRatingSelected: (Int) -> Unit,
    onFeedbackChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = LocalFixSpacing.medium,
            vertical = LocalFixSpacing.large,
        ),
    ) {
        Text("Review the repair", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Check the completed work before closing this request.",
            modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.padding(top = LocalFixSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
        ) {
            FilterChip(
                selected = review.selectedDecision == ResidentReviewDecision.CONFIRM,
                onClick = { onDecisionSelected(ResidentReviewDecision.CONFIRM) },
                label = { Text("Repair is complete") },
                modifier = Modifier.testTag("resident-review-confirm"),
            )
            FilterChip(
                selected = review.selectedDecision == ResidentReviewDecision.REQUEST_REWORK,
                onClick = { onDecisionSelected(ResidentReviewDecision.REQUEST_REWORK) },
                label = { Text("Needs more work") },
                modifier = Modifier.testTag("resident-review-rework"),
            )
        }
        review.decisionError?.let { FormError(it) }
        if (review.selectedDecision == ResidentReviewDecision.CONFIRM) {
            Text(
                text = "Rate the repair",
                modifier = Modifier.padding(top = LocalFixSpacing.medium),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.padding(top = LocalFixSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.extraSmall),
            ) {
                (1..5).forEach { rating ->
                    FilterChip(
                        selected = review.rating == rating,
                        onClick = { onRatingSelected(rating) },
                        label = { Text(rating.toString()) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.testTag("resident-rating-$rating"),
                    )
                }
            }
            review.ratingError?.let { FormError(it) }
        }
        if (review.selectedDecision != null) {
            OutlinedTextField(
                value = review.feedback,
                onValueChange = onFeedbackChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LocalFixSpacing.medium)
                    .testTag("resident-review-feedback"),
                label = {
                    Text(
                        if (review.selectedDecision == ResidentReviewDecision.REQUEST_REWORK) {
                            "What still needs attention?"
                        } else {
                            "Feedback (optional)"
                        },
                    )
                },
                supportingText = {
                    Text(review.feedbackError ?: "${review.feedback.length}/500")
                },
                isError = review.feedbackError != null,
                minLines = 2,
                maxLines = 4,
            )
        }
        review.submissionError?.let { FormError(it) }
        Button(
            onClick = onSubmit,
            enabled = !review.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LocalFixSpacing.medium)
                .testTag("resident-submit-review"),
            shape = RoundedCornerShape(LocalFixRadius.medium),
        ) {
            if (review.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    if (review.selectedDecision == ResidentReviewDecision.REQUEST_REWORK) {
                        "Request more work"
                    } else {
                        "Confirm repair"
                    },
                )
            }
        }
    }
}

@Composable
private fun ResidentReviewSummary(uiState: ResidentRequestDetailUiState) {
    DetailSection(title = "Your review") {
        uiState.residentRating?.let { rating ->
            DetailRow(
                icon = Icons.Outlined.Star,
                label = "Your rating",
                value = "$rating out of 5",
            )
        }
        uiState.residentFeedback?.let { feedback ->
            if (uiState.residentRating != null) {
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            }
            Text(
                text = if (uiState.residentRating == null) "Rework requested" else "Feedback",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = feedback,
                modifier = Modifier.padding(top = LocalFixSpacing.extraSmall),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FormError(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(top = LocalFixSpacing.small),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun DetailHeader(
    uiState: ResidentRequestDetailUiState,
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
                text = uiState.id,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelLarge,
            )
            RequestStatusBadge(
                label = uiState.statusLabel,
                tone = uiState.statusTone,
            )
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text(
            text = uiState.title,
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = uiState.updatedLabel,
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DetailSection(
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
        Text(
            text = title,
            modifier = Modifier.padding(start = LocalFixSpacing.extraSmall),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(LocalFixSpacing.medium)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(
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
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MissingRequestScreen(
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
            text = "This maintenance request may no longer be available.",
            modifier = Modifier.padding(top = LocalFixSpacing.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ResidentRequestDetailPreview() {
    LocalFixTheme(darkTheme = false) {
        ResidentRequestDetailScreen(
            uiState = ResidentRequestDetailUiState(
                requestId = "LF-1042",
                id = "LF-1042",
                title = "Leaking kitchen tap",
                description = "The tap keeps dripping even when fully closed.",
                categoryLabel = "Plumbing",
                statusLabel = "In progress",
                statusTone = RequestStatusTone.ACTIVE,
                urgencyLabel = "Soon",
                accessWindowLabel = "Morning · 8 AM–12 PM",
                assignedWorker = "Arun · Plumbing",
                updatedLabel = "Updated 18 min ago",
                photoUri = null,
                completionNote = null,
                partsUsed = emptyList(),
                completionPhotoUrl = null,
                residentRating = null,
                residentFeedback = null,
                canReview = false,
                review = ResidentReviewUiState(),
            ),
            onBack = {},
            onReviewDecisionSelected = {},
            onRatingSelected = {},
            onFeedbackChanged = {},
            onSubmitReview = {},
        )
    }
}
