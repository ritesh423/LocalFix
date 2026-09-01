package com.localfix.app.ui.manager

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.ui.theme.LocalFixSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ManagerWorkerInviteScreen(
    uiState: ManagerWorkerInviteUiState,
    onNameChange: (String) -> Unit,
    onSpecialtySelected: (ServiceCategory) -> Unit,
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
            Text("Invite a worker", style = MaterialTheme.typography.headlineSmall)
        }
        Column(modifier = Modifier.padding(LocalFixSpacing.large)) {
            if (uiState.inviteCode == null) {
                WorkerInviteSetup(
                    uiState = uiState,
                    onNameChange = onNameChange,
                    onSpecialtySelected = onSpecialtySelected,
                    onCreateInvite = onCreateInvite,
                )
            } else {
                CreatedWorkerInvite(uiState = uiState, onDone = onBack)
            }
        }
    }
}

@Composable
private fun WorkerInviteSetup(
    uiState: ManagerWorkerInviteUiState,
    onNameChange: (String) -> Unit,
    onSpecialtySelected: (ServiceCategory) -> Unit,
    onCreateInvite: () -> Unit,
) {
    Text("Worker details", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
    Text(
        "This creates the worker in your property and gives them a one-time sign-up code.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    OutlinedTextField(
        value = uiState.name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth().testTag("manager-worker-name"),
        label = { Text("Worker name") },
        enabled = !uiState.isCreating,
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    Text("Main specialty", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
    ) {
        ServiceCategory.entries.take(2).forEach { specialty ->
            SpecialtyChip(
                specialty = specialty,
                selected = uiState.specialty == specialty,
                enabled = !uiState.isCreating,
                onClick = { onSpecialtySelected(specialty) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
    ) {
        ServiceCategory.entries.drop(2).forEach { specialty ->
            SpecialtyChip(
                specialty = specialty,
                selected = uiState.specialty == specialty,
                enabled = !uiState.isCreating,
                onClick = { onSpecialtySelected(specialty) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        Text(error, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
    Button(
        onClick = onCreateInvite,
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("create-worker-invite"),
        enabled = uiState.canCreate,
    ) {
        if (uiState.isCreating) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Create worker invite")
        }
    }
}

@Composable
private fun SpecialtyChip(
    specialty: ServiceCategory,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(specialty.label) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun CreatedWorkerInvite(
    uiState: ManagerWorkerInviteUiState,
    onDone: () -> Unit,
) {
    Text("Worker invite ready", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
    Text(
        "Send this code privately to ${uiState.invitedWorkerName}. It can be claimed once.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.large))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(LocalFixSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Worker sign-up code", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            SelectionContainer {
                Text(
                    text = uiState.inviteCode.orEmpty(),
                    modifier = Modifier.testTag("worker-invite-code"),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
    Text(
        "Expires ${uiState.expiresAt.toReadableExpiry()}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(LocalFixSpacing.extraLarge))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("Done")
    }
}

private fun String?.toReadableExpiry(): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(this))
}.getOrDefault("soon")
