package com.localfix.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.components.LocalFixScreenHeader
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun ResidentProfileScreen(
    uiState: ResidentProfileUiState,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        LocalFixScreenHeader(
            eyebrow = "Resident workspace",
            title = "Profile",
            subtitle = "Your apartment and contact details.",
        )
        ProfileIdentity(uiState)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(LocalFixRadius.large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                ProfileDetail(Icons.Outlined.Apartment, "Property", uiState.propertyName)
                ProfileDetail(Icons.Outlined.HomeWork, "Home", uiState.unitLabel)
                if (uiState.phone.isNotBlank()) {
                    ProfileDetail(Icons.Outlined.Phone, "Phone", uiState.phone)
                }
                if (uiState.email.isNotBlank()) {
                    ProfileDetail(Icons.Outlined.Email, "Email", uiState.email)
                }
            }
        }
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
private fun ProfileIdentity(uiState: ResidentProfileUiState) {
    Row(
        modifier = Modifier.padding(LocalFixSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = uiState.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.width(LocalFixSpacing.medium))
        Column {
            Text(
                text = uiState.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
            Text(
                text = uiState.statusLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ProfileDetail(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LocalFixSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(LocalFixSpacing.medium))
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ResidentProfilePreview() {
    LocalFixTheme(darkTheme = false) {
        ResidentProfileScreen(
            uiState = ResidentProfileUiState.sample,
            onSwitchRole = {},
        )
    }
}
