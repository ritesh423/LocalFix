package com.localfix.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme
import com.localfix.app.ui.components.RequestSyncNotice

@Composable
fun ResidentHomeScreen(
    uiState: ResidentHomeUiState,
    onReportIssue: () -> Unit,
    onRequestClick: (String) -> Unit,
    onCategoryClick: (ServiceCategoryType) -> Unit,
    onRetryRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = LocalFixSpacing.large),
    ) {
        item {
            ResidentHeader(
                uiState = uiState,
                onReportIssue = onReportIssue,
            )
        }

        item {
            StatusOverview(
                activeCount = uiState.activeRequestCount,
                awaitingCount = uiState.awaitingConfirmationCount,
                modifier = Modifier.padding(
                    horizontal = LocalFixSpacing.medium,
                    vertical = LocalFixSpacing.large,
                ),
            )
        }

        if (uiState.isLoadingRequests || uiState.requestErrorMessage != null) {
            item {
                RequestSyncNotice(
                    isLoading = uiState.isLoadingRequests,
                    errorMessage = uiState.requestErrorMessage,
                    onRetry = onRetryRequests,
                    modifier = Modifier.padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        bottom = LocalFixSpacing.large,
                    ),
                )
            }
        }

        uiState.activeRequest?.let { request ->
            item {
                SectionHeader(
                    title = "Active request",
                    action = "View all",
                )
            }
            item {
                ActiveRequestCard(
                    request = request,
                    onClick = { onRequestClick(request.id) },
                    modifier = Modifier.padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        top = LocalFixSpacing.small,
                        bottom = LocalFixSpacing.large,
                    ),
                )
            }
        }

        item {
            SectionHeader(
                title = "What needs fixing?",
                subtitle = "Choose a category to report an issue",
            )
        }

        items(uiState.categories.chunked(2)) { categoryRow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LocalFixSpacing.medium,
                        vertical = LocalFixSpacing.extraSmall,
                    ),
                horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
            ) {
                categoryRow.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (categoryRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ResidentHeader(
    uiState: ResidentHomeUiState,
    onReportIssue: () -> Unit,
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
                start = LocalFixSpacing.large,
                end = LocalFixSpacing.large,
                top = LocalFixSpacing.medium,
                bottom = LocalFixSpacing.extraLarge,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Apartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(LocalFixSpacing.small)
                        .size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(LocalFixSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.propertyName,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = uiState.unitLabel,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = uiState.residentName.take(1).uppercase(),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
        Text(
            text = "Good morning, ${uiState.residentName}",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
        Text(
            text = "Your home, taken care of.",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        Button(
            onClick = onReportIssue,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            shape = RoundedCornerShape(LocalFixRadius.medium),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(LocalFixSpacing.small))
            Text("Report an issue")
        }
    }
}

@Composable
private fun StatusOverview(
    activeCount: Int,
    awaitingCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LocalFixSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusCount(
                count = activeCount,
                label = "Active",
                supportingText = "Request in progress",
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            StatusCount(
                count = awaitingCount,
                label = "To review",
                supportingText = "Waiting for you",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = LocalFixSpacing.medium),
            )
        }
    }
}

@Composable
private fun StatusCount(
    count: Int,
    label: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.width(LocalFixSpacing.small))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalFixSpacing.medium),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        action?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ActiveRequestCard(
    request: MaintenanceRequestSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(LocalFixSpacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = LocalFixTheme.statusColors.activeContainer,
                    shape = RoundedCornerShape(LocalFixRadius.small),
                ) {
                    Text(
                        text = request.statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = LocalFixTheme.statusColors.onActiveContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = request.reference,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            Text(
                text = request.assignedWorker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(LocalFixSpacing.extraSmall))
                Text(
                    text = request.updatedLabel,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: ServiceCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LocalFixRadius.medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(LocalFixSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(LocalFixRadius.small),
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(LocalFixSpacing.small))
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private val ServiceCategory.icon: ImageVector
    get() = when (type) {
        ServiceCategoryType.PLUMBING -> Icons.Outlined.Build
        ServiceCategoryType.ELECTRICAL -> Icons.Outlined.ElectricalServices
        ServiceCategoryType.APPLIANCE -> Icons.Outlined.Handyman
        ServiceCategoryType.OTHER -> Icons.Outlined.MoreHoriz
    }

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ResidentHomePreview() {
    LocalFixTheme(darkTheme = false) {
        ResidentHomeScreen(
            uiState = ResidentHomeUiState.sample,
            onReportIssue = {},
            onRequestClick = {},
            onCategoryClick = {},
            onRetryRequests = {},
        )
    }
}
