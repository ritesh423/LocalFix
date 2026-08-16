package com.localfix.app.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.localfix.app.data.model.AccessWindow
import com.localfix.app.data.model.ServiceCategory
import com.localfix.app.data.model.UrgencySuggestion
import com.localfix.app.ui.components.persistPhotoAccess
import com.localfix.app.ui.components.releasePhotoAccess
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun CreateRequestScreen(
    uiState: CreateRequestUiState,
    onBack: () -> Unit,
    onDiscard: () -> Unit,
    onCategorySelected: (ServiceCategory) -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onUrgencySelected: (UrgencySuggestion) -> Unit,
    onAccessWindowSelected: (AccessWindow) -> Unit,
    onPhotoSelected: (String) -> Unit,
    onPhotoRemoved: () -> Unit,
    onPhotoSelectionFailed: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val previousPhotoUri = uiState.draft.photoUri
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .testTag("create-request-list"),
        contentPadding = PaddingValues(bottom = LocalFixSpacing.large),
    ) {
        item {
            CreateRequestHeader(onBack)
        }
        item {
            FormSection(
                title = "Service category",
                supportingText = "Choose the team most likely to handle this.",
            ) {
                CategoryPicker(
                    selected = uiState.draft.category,
                    onSelected = onCategorySelected,
                )
                uiState.errors.category?.let { error ->
                    FormError(error)
                }
            }
        }
        item {
            FormSection(
                title = "What is happening?",
                supportingText = "Keep the title short, then add useful detail.",
            ) {
                OutlinedTextField(
                    value = uiState.draft.title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request-title"),
                    label = { Text("Short title") },
                    placeholder = { Text("Leaking tap under the sink") },
                    supportingText = {
                        Text(uiState.errors.title ?: "${uiState.draft.title.length}/80")
                    },
                    isError = uiState.errors.title != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                OutlinedTextField(
                    value = uiState.draft.description,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request-description"),
                    label = { Text("Description") },
                    placeholder = { Text("Tell the worker what you noticed and where.") },
                    supportingText = {
                        Text(
                            uiState.errors.description
                                ?: "${uiState.draft.description.length}/500",
                        )
                    },
                    isError = uiState.errors.description != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    minLines = 4,
                    maxLines = 6,
                )
            }
        }
        item {
            FormSection(
                title = "Add a photo",
                supportingText = "A clear photo helps the maintenance team prepare before arrival.",
            ) {
                PhotoEvidencePicker(
                    photoUri = uiState.draft.photoUri,
                    error = uiState.photoError,
                    onChoosePhoto = {
                        photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = {
                        uiState.draft.photoUri?.let { uri -> releasePhotoAccess(context, uri) }
                        onPhotoRemoved()
                    },
                )
            }
        }
        item {
            FormSection(
                title = "How soon does it need attention?",
                supportingText = "This is your suggestion. The manager sets final priority.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
                ) {
                    UrgencySuggestion.entries.forEach { urgency ->
                        FilterChip(
                            selected = uiState.draft.urgencySuggestion == urgency,
                            onClick = { onUrgencySelected(urgency) },
                            label = { Text(urgency.label) },
                        )
                    }
                }
            }
        }
        item {
            FormSection(
                title = "Preferred access time",
                supportingText = "Choose when maintenance can enter the apartment.",
            ) {
                AccessWindow.entries.forEach { accessWindow ->
                    Surface(
                        onClick = { onAccessWindowSelected(accessWindow) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(LocalFixRadius.medium),
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = LocalFixSpacing.small,
                                vertical = LocalFixSpacing.extraSmall,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = uiState.draft.accessWindow == accessWindow,
                                onClick = null,
                            )
                            Text(
                                text = accessWindow.label,
                                modifier = Modifier.padding(start = LocalFixSpacing.small),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = LocalFixSpacing.medium),
            ) {
                uiState.submissionError?.let { error ->
                    FormError(error)
                    Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                }
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit-request"),
                    shape = RoundedCornerShape(LocalFixRadius.medium),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Submit request")
                    }
                }
                TextButton(
                    onClick = {
                        uiState.draft.photoUri?.let { uri -> releasePhotoAccess(context, uri) }
                        onDiscard()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Discard draft")
                }
                Text(
                    text = "Your draft and selected photo are saved automatically.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PhotoEvidencePicker(
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
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(modifier = Modifier.height(LocalFixSpacing.small))
                Text(
                    text = "Show the issue clearly",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Choose one photo from this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
                OutlinedButton(
                    onClick = onChoosePhoto,
                    modifier = Modifier.testTag("choose-request-photo"),
                ) {
                    Text("Choose photo")
                }
            }
        }
    } else {
        Column {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = "Selected maintenance photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(LocalFixRadius.large))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("request-photo-preview"),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(LocalFixSpacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Photo attached",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Saved with this draft",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = onChoosePhoto,
                    modifier = Modifier.testTag("replace-request-photo"),
                ) {
                    Text("Replace")
                }
                IconButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier.testTag("remove-request-photo"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove photo",
                    )
                }
            }
        }
    }

    error?.let { message ->
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        FormError(message)
    }
}

@Composable
private fun CreateRequestHeader(onBack: () -> Unit) {
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
                top = LocalFixSpacing.small,
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
        Text(
            text = "Report an issue",
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = "Give the maintenance team enough detail to act quickly.",
            modifier = Modifier.padding(start = LocalFixSpacing.medium),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    supportingText: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = LocalFixSpacing.medium,
            vertical = LocalFixSpacing.large,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.extraSmall))
        Text(
            text = supportingText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.medium))
        content()
    }
}

@Composable
private fun CategoryPicker(
    selected: ServiceCategory?,
    onSelected: (ServiceCategory) -> Unit,
) {
    ServiceCategory.entries.chunked(2).forEach { rowCategories ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalFixSpacing.small),
        ) {
            rowCategories.forEach { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelected(category) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("request-category-${category.name.lowercase()}"),
                    label = { Text(category.label) },
                )
            }
        }
    }
}

@Composable
private fun FormError(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private val UrgencySuggestion.label: String
    get() = when (this) {
        UrgencySuggestion.ROUTINE -> "Routine"
        UrgencySuggestion.SOON -> "Soon"
        UrgencySuggestion.URGENT -> "Urgent"
    }

private val AccessWindow.label: String
    get() = when (this) {
        AccessWindow.ANYTIME -> "Any time today"
        AccessWindow.MORNING -> "Morning · 8 AM–12 PM"
        AccessWindow.AFTERNOON -> "Afternoon · 12–4 PM"
        AccessWindow.EVENING -> "Evening · 4–8 PM"
    }

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun CreateRequestPreview() {
    LocalFixTheme(darkTheme = false) {
        CreateRequestScreen(
            uiState = CreateRequestUiState(),
            onBack = {},
            onDiscard = {},
            onCategorySelected = {},
            onTitleChanged = {},
            onDescriptionChanged = {},
            onUrgencySelected = {},
            onAccessWindowSelected = {},
            onPhotoSelected = {},
            onPhotoRemoved = {},
            onPhotoSelectionFailed = {},
            onSubmit = {},
        )
    }
}
