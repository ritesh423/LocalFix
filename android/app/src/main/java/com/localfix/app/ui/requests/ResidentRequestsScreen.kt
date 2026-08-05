package com.localfix.app.ui.requests

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.components.LocalFixScreenHeader
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun ResidentRequestsScreen(
    uiState: ResidentRequestsUiState,
    onReportIssue: () -> Unit,
    onRequestClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(RequestFilter.ALL) }
    val visibleRequests = uiState.requests.filter { request ->
        when (selectedFilter) {
            RequestFilter.ALL -> true
            RequestFilter.ACTIVE -> request.status != RequestStatus.COMPLETED
            RequestFilter.COMPLETED -> request.status == RequestStatus.COMPLETED
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item {
            LocalFixScreenHeader(
                eyebrow = "Apartment A-204",
                title = "My requests",
                subtitle = "Track repairs from report to completion.",
            )
        }
        item {
            Button(
                onClick = onReportIssue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = LocalFixSpacing.medium,
                        end = LocalFixSpacing.medium,
                        top = LocalFixSpacing.large,
                    ),
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Text(
                    text = "Report an issue",
                    modifier = Modifier.padding(start = LocalFixSpacing.small),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalFixSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
            ) {
                RequestFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                    )
                }
            }
        }
        items(
            items = visibleRequests,
            key = ResidentRequestItem::id,
        ) { request ->
            RequestCard(
                request = request,
                onClick = { onRequestClick(request.id) },
                modifier = Modifier.padding(
                    horizontal = LocalFixSpacing.medium,
                    vertical = LocalFixSpacing.extraSmall,
                ),
            )
        }
        item {
            Spacer(modifier = Modifier.height(LocalFixSpacing.large))
        }
    }
}

@Composable
private fun RequestCard(
    request: ResidentRequestItem,
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
                    Text(
                        text = request.id,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    RequestStatusPill(request)
                }
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = "${request.category} · ${request.updatedLabel}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun RequestStatusPill(request: ResidentRequestItem) {
    val colors = when (request.status) {
        RequestStatus.IN_PROGRESS -> LocalFixTheme.statusColors.activeContainer to
            LocalFixTheme.statusColors.onActiveContainer
        RequestStatus.AWAITING_CONFIRMATION -> LocalFixTheme.statusColors.attentionContainer to
            LocalFixTheme.statusColors.onAttentionContainer
        RequestStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(LocalFixRadius.small),
    ) {
        Text(
            text = request.statusLabel,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Unspecified,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ResidentRequestsPreview() {
    LocalFixTheme(darkTheme = false) {
        ResidentRequestsScreen(
            uiState = ResidentRequestsUiState.sample,
            onReportIssue = {},
            onRequestClick = {},
        )
    }
}
