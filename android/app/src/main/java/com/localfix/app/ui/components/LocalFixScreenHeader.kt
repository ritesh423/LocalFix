package com.localfix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixSpacing

@Composable
fun LocalFixScreenHeader(
    title: String,
    subtitle: String,
    eyebrow: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
                top = LocalFixSpacing.large,
                bottom = LocalFixSpacing.extraLarge,
            ),
    ) {
        Text(
            text = eyebrow,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(LocalFixSpacing.small))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
