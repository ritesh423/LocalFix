package com.localfix.app.ui.role

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.components.LocalFixScreenHeader
import com.localfix.app.ui.session.AppRole
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun RoleWorkspaceScreen(
    role: AppRole,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(role == AppRole.WORKER)
    BackHandler(onBack = onSwitchRole)

    val content = role.workspaceContent
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        LocalFixScreenHeader(
            eyebrow = "Lakeview Residency",
            title = content.title,
            subtitle = content.subtitle,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalFixSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
        ) {
            WorkspaceMetric(
                value = content.primaryCount,
                label = content.primaryLabel,
                modifier = Modifier.weight(1f),
            )
            WorkspaceMetric(
                value = content.secondaryCount,
                label = content.secondaryLabel,
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(LocalFixSpacing.large)) {
                Text(
                    text = content.cardEyebrow,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = content.cardTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = content.cardBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
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
}

@Composable
private fun WorkspaceMetric(
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

private data class WorkspaceContent(
    val title: String,
    val subtitle: String,
    val primaryCount: String,
    val primaryLabel: String,
    val secondaryCount: String,
    val secondaryLabel: String,
    val cardEyebrow: String,
    val cardTitle: String,
    val cardBody: String,
)

private val AppRole.workspaceContent: WorkspaceContent
    get() = when (this) {
        AppRole.WORKER -> WorkspaceContent(
            title = "My jobs",
            subtitle = "Work from your assigned queue, even with poor internet.",
            primaryCount = "3",
            primaryLabel = "Assigned today",
            secondaryCount = "1",
            secondaryLabel = "In progress",
            cardEyebrow = "Current job",
            cardTitle = "Leaking kitchen tap · A-204",
            cardBody = "Access available until 12:30 PM · Plumbing",
        )
        AppRole.RESIDENT, AppRole.MANAGER -> error("This role has its own navigation")
    }

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WorkerWorkspacePreview() {
    LocalFixTheme(darkTheme = false) {
        RoleWorkspaceScreen(
            role = AppRole.WORKER,
            onSwitchRole = {},
        )
    }
}
