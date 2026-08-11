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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.localfix.app.ui.components.RequestStatusBadge
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun ResidentRequestDetailScreen(
    uiState: ResidentRequestDetailUiState?,
    onBack: () -> Unit,
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
        item {
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
        }
    }
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
            ),
            onBack = {},
        )
    }
}
