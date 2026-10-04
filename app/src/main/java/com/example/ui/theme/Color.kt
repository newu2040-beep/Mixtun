package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class PastelTheme(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val darkBackgroundStart: Color,
    val darkBackgroundEnd: Color,
    val lightBackgroundStart: Color,
    val lightBackgroundEnd: Color
) {
    CYAN_ICE(
        displayName = "Pastel Cyan",
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF00E5FF),
        darkBackgroundStart = Color(0xFF060B1E),
        darkBackgroundEnd = Color(0xFF0A163B),
        lightBackgroundStart = Color(0xFFF0F9FF),
        lightBackgroundEnd = Color(0xFFE0F2FE)
    ),
    LAVENDER_LILAC(
        displayName = "Pastel Lilac",
        primary = Color(0xFFC084FC),
        secondary = Color(0xFFE879F9),
        darkBackgroundStart = Color(0xFF110724),
        darkBackgroundEnd = Color(0xFF1E0E3D),
        lightBackgroundStart = Color(0xFFFAF5FF),
        lightBackgroundEnd = Color(0xFFF3E8FF)
    ),
    MINT_SAGE(
        displayName = "Pastel Mint",
        primary = Color(0xFF6EE7B7),
        secondary = Color(0xFF34D399),
        darkBackgroundStart = Color(0xFF041812),
        darkBackgroundEnd = Color(0xFF0A2B20),
        lightBackgroundStart = Color(0xFFF0FDF4),
        lightBackgroundEnd = Color(0xFFDCFCE7)
    ),
    PEACH_CORAL(
        displayName = "Pastel Peach",
        primary = Color(0xFFFDBA74),
        secondary = Color(0xFFFB923C),
        darkBackgroundStart = Color(0xFF1C0D05),
        darkBackgroundEnd = Color(0xFF2C1608),
        lightBackgroundStart = Color(0xFFFFF7ED),
        lightBackgroundEnd = Color(0xFFFFEDD5)
    ),
    ROSE_BLOSSOM(
        displayName = "Pastel Rose",
        primary = Color(0xFFFDA4AF),
        secondary = Color(0xFFF43F5E),
        darkBackgroundStart = Color(0xFF1C0813),
        darkBackgroundEnd = Color(0xFF2D0E20),
        lightBackgroundStart = Color(0xFFFFF1F2),
        lightBackgroundEnd = Color(0xFFFFE4E6)
    ),
    SKY_AZURE(
        displayName = "Pastel Sky",
        primary = Color(0xFF93C5FD),
        secondary = Color(0xFF60A5FA),
        darkBackgroundStart = Color(0xFF071224),
        darkBackgroundEnd = Color(0xFF0C203F),
        lightBackgroundStart = Color(0xFFF0F9FF),
        lightBackgroundEnd = Color(0xFFDBEAFE)
    )
}

// Core Colors
val MixtunNavy = Color(0xFF060B1E)
val MixtunDeepSpace = Color(0xFF0A1128)
val MixtunDarkSurface = Color(0xFF0D1739)
val MixtunCardSurface = Color(0xCC111D45)

val MixtunCyan = Color(0xFF00E5FF)
val MixtunBlue = Color(0xFF2979FF)
val MixtunViolet = Color(0xFF7C4DFF)
val MixtunEmerald = Color(0xFF00E676)
val MixtunAmber = Color(0xFFFFB300)
val MixtunRose = Color(0xFFFF4081)

val GlassSurfaceDark = Color(0xB20B1433)
val GlassSurfaceLighter = Color(0xCC13204D)
val GlassBorder = Color(0x33FFFFFF)
val GlassBorderCyan = Color(0x4000E5FF)

val TextPrimary = Color(0xFFF0F4F8)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

val TextLightPrimary = Color(0xFF0F172A)
val TextLightSecondary = Color(0xFF475569)
val TextLightTertiary = Color(0xFF94A3B8)

val MixtunBackgroundBrush = Brush.verticalGradient(
    listOf(Color(0xFF060B1E), Color(0xFF0A163B), Color(0xFF060B1E))
)

fun getBackgroundBrush(darkTheme: Boolean, pastelTheme: PastelTheme): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            listOf(
                pastelTheme.darkBackgroundStart,
                pastelTheme.darkBackgroundEnd,
                pastelTheme.darkBackgroundStart
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                pastelTheme.lightBackgroundStart,
                pastelTheme.lightBackgroundEnd,
                pastelTheme.lightBackgroundStart
            )
        )
    }
}
