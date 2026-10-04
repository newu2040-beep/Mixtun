package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalPastelTheme = staticCompositionLocalOf { PastelTheme.CYAN_ICE }
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun MixtunTheme(
    darkTheme: Boolean = true,
    pastelTheme: PastelTheme = PastelTheme.CYAN_ICE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = pastelTheme.primary,
            onPrimary = Color(0xFF001F29),
            primaryContainer = pastelTheme.primary.copy(alpha = 0.2f),
            onPrimaryContainer = pastelTheme.primary,
            secondary = pastelTheme.secondary,
            onSecondary = Color.White,
            secondaryContainer = pastelTheme.secondary.copy(alpha = 0.2f),
            onSecondaryContainer = pastelTheme.secondary,
            tertiary = MixtunViolet,
            onTertiary = Color.White,
            background = pastelTheme.darkBackgroundStart,
            onBackground = TextPrimary,
            surface = pastelTheme.darkBackgroundEnd,
            onSurface = TextPrimary,
            surfaceVariant = Color(0x33FFFFFF),
            onSurfaceVariant = TextSecondary,
            outline = pastelTheme.primary.copy(alpha = 0.35f)
        )
    } else {
        lightColorScheme(
            primary = pastelTheme.primary,
            onPrimary = Color.White,
            primaryContainer = pastelTheme.primary.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = pastelTheme.secondary,
            onSecondary = Color.White,
            secondaryContainer = pastelTheme.secondary.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF0F172A),
            tertiary = Color(0xFF8B5CF6),
            onTertiary = Color.White,
            background = pastelTheme.lightBackgroundStart,
            onBackground = TextLightPrimary,
            surface = Color.White.copy(alpha = 0.9f),
            onSurface = TextLightPrimary,
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = TextLightSecondary,
            outline = pastelTheme.primary.copy(alpha = 0.4f)
        )
    }

    CompositionLocalProvider(
        LocalPastelTheme provides pastelTheme,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
