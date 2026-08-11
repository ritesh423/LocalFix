package com.localfix.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.localfix.app.ui.requests.RequestStatusTone
import com.localfix.app.ui.theme.LocalFixRadius
import com.localfix.app.ui.theme.LocalFixTheme

@Composable
fun RequestStatusBadge(
    label: String,
    tone: RequestStatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = when (tone) {
        RequestStatusTone.ACTIVE -> LocalFixTheme.statusColors.activeContainer to
            LocalFixTheme.statusColors.onActiveContainer
        RequestStatusTone.ATTENTION -> LocalFixTheme.statusColors.attentionContainer to
            LocalFixTheme.statusColors.onAttentionContainer
        RequestStatusTone.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        RequestStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(LocalFixRadius.small),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Unspecified,
        )
    }
}
