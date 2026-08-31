package com.localfix.app.ui.manager

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ManagerResidentInviteScreen(
    uiState: ManagerResidentInviteUiState,
    onUnitSelected: (String) -> Unit,
    onCreateInvite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalFixSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Invite a resident",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Column(modifier = Modifier.padding(LocalFixSpacing.large)) {
            if (uiState.inviteCode == null) {
                InviteSetup(
                    uiState = uiState,
                    onUnitSelected = onUnitSelected,
                    onCreateInvite = onCreateInvite,
                )
            } else {
                CreatedInvite(uiState = uiState, onDone = onBack)
            }
        }
    }
}

@Composable
private fun InviteSetup(
    uiState: ManagerResidentInviteUiState,
    onUnitSelected: (String) -> Unit,
    onCreateInvite: () -> Unit,
) {
    Text(
        text = "Choose the apartment",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
    Text(
        text = "The resident will be linked to the unit you select here.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    if (uiState.units.isEmpty()) {
        Text(
            text = "No active apartment units are available.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(LocalFixSpacing.small)) {
            uiState.units.forEach { unit ->
                val selected = unit.id == uiState.selectedUnitId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            onClick = { onUnitSelected(unit.id) },
                            role = Role.RadioButton,
                        )
                        .testTag("manager-invite-unit-${unit.id}"),
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        1.dp,
                        if (selected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(LocalFixSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                        )
                        Text(
                            text = unit.label,
                            modifier = Modifier.padding(start = LocalFixSpacing.small),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text(error, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    Button(
        onClick = onCreateInvite,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("manager-create-invite"),
        enabled = uiState.canCreate,
    ) {
        if (uiState.isCreating) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Create 7-day invite")
        }
    }
}

@Composable
private fun CreatedInvite(
    uiState: ManagerResidentInviteUiState,
    onDone: () -> Unit,
) {
    Text("Invite ready", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
    Text(
        text = "Share this code only with the resident of ${uiState.inviteUnitLabel}.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("manager-created-invite"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            LocalFixRadius.large
        ),
    ) {
        Column(
            modifier = Modifier.padding(LocalFixSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SelectionContainer {
                Text(
                    text = requireNotNull(uiState.inviteCode),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            uiState.expiresAt?.let { expiresAt ->
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = "Expires ${expiresAt.toReadableExpiry()}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
    Text(
        text = "For security, LocalFix cannot show this code again after you leave this screen.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text("Done")
    }
}

private fun String.toReadableExpiry(): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
        .format(Instant.parse(this).atZone(ZoneId.systemDefault()))
}.getOrDefault(this)
