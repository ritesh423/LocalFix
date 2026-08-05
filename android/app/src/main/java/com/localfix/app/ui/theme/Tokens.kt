package com.localfix.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object LocalFixSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

object LocalFixRadius {
    val small = 8.dp
    val medium = 14.dp
    val large = 20.dp
    val extraLarge = 28.dp
}

@Immutable
data class LocalFixStatusColors(
    val activeContainer: Color,
    val onActiveContainer: Color,
    val attentionContainer: Color,
    val onAttentionContainer: Color,
    val overdueContainer: Color,
    val onOverdueContainer: Color,
)
