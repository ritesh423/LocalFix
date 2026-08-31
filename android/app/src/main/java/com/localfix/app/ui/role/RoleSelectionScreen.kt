package com.localfix.app.ui.role

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.session.AppRole
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (AppRole) -> Unit,
    modifier: Modifier = Modifier,
    roles: List<AppRole> = AppRole.entries,
    propertyName: String = "Lakeview Residency",
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = LocalFixSpacing.extraLarge),
    ) {
        item {
            RoleSelectionHeader(propertyName)
        }
        item {
            Column(
                modifier = Modifier.padding(
                    start = LocalFixSpacing.large,
                    end = LocalFixSpacing.large,
                    top = LocalFixSpacing.extraLarge,
                    bottom = LocalFixSpacing.medium,
                ),
            ) {
                Text(
                    text = "Choose your workspace",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = "You can switch roles later from your profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(roles) { role ->
            RoleCard(
                role = role,
                onClick = { onRoleSelected(role) },
                modifier = Modifier.padding(
                    horizontal = LocalFixSpacing.large,
                    vertical = LocalFixSpacing.small,
                ),
            )
        }
    }
}

@Composable
private fun RoleSelectionHeader(propertyName: String) {
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
            .padding(LocalFixSpacing.large),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(LocalFixRadius.medium),
        ) {
            Text(
                text = "LF",
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
        Text(
            text = "LocalFix",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = "$propertyName · Apartment operations",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RoleCard(
    role: AppRole,
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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(LocalFixRadius.medium),
            ) {
                Icon(
                    imageVector = role.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(LocalFixSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
                Text(
                    text = role.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.width(LocalFixSpacing.small))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val AppRole.icon: ImageVector
    get() = when (this) {
        AppRole.RESIDENT -> Icons.Outlined.HomeWork
        AppRole.MANAGER -> Icons.Outlined.AdminPanelSettings
        AppRole.WORKER -> Icons.Outlined.Engineering
    }

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun RoleSelectionPreview() {
    LocalFixTheme(darkTheme = false) {
        RoleSelectionScreen(onRoleSelected = {})
    }
}
