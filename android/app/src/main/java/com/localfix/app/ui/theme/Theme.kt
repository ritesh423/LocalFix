package com.localfix.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = InkLight,
    onPrimaryContainer = OnInkLight,
    secondary = ServiceTeal,
    onSecondary = Color.White,
    secondaryContainer = ServiceTealLight,
    onSecondaryContainer = OnServiceTealLight,
    background = WarmCanvas,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE7ECE8),
    onSurfaceVariant = MutedSlate,
    outline = Outline,
    outlineVariant = OutlineSoft,
    error = Brick,
    errorContainer = BrickContainer,
    onErrorContainer = OnBrickContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkInk,
    onPrimary = OnDarkInk,
    primaryContainer = Color(0xFF294B64),
    onPrimaryContainer = Color(0xFFD4E8F7),
    secondary = DarkTeal,
    onSecondary = OnDarkTeal,
    secondaryContainer = Color(0xFF155052),
    onSecondaryContainer = Color(0xFFB8ECEA),
    background = DarkCanvas,
    onBackground = Color(0xFFE1E7E8),
    surface = DarkPaper,
    onSurface = Color(0xFFE1E7E8),
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Color(0xFFBBC5C9),
    outline = DarkOutline,
    outlineVariant = Color(0xFF3E494E),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C1D22),
    onErrorContainer = Color(0xFFFFDAD7),
)

private val LightStatusColors = LocalFixStatusColors(
    activeContainer = ServiceTealLight,
    onActiveContainer = OnServiceTealLight,
    attentionContainer = AmberContainer,
    onAttentionContainer = OnAmberContainer,
    overdueContainer = BrickContainer,
    onOverdueContainer = OnBrickContainer,
)

private val DarkStatusColors = LocalFixStatusColors(
    activeContainer = Color(0xFF155052),
    onActiveContainer = Color(0xFFB8ECEA),
    attentionContainer = Color(0xFF5E3B08),
    onAttentionContainer = Color(0xFFFFDDB5),
    overdueContainer = Color(0xFF7A272A),
    onOverdueContainer = Color(0xFFFFDAD7),
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

object LocalFixTheme {
    val statusColors: LocalFixStatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}

@Composable
fun LocalFixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LocalFixTypography,
            content = content,
        )
    }
}
