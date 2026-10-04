package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalPastelTheme

@Composable
fun MixtunGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val pastel = LocalPastelTheme.current

    val actualBg = backgroundColor ?: if (isDark) GlassSurfaceDark else Color.White.copy(alpha = 0.85f)
    val actualBorder = borderColor ?: if (isDark) pastel.primary.copy(alpha = 0.35f) else pastel.primary.copy(alpha = 0.45f)

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(actualBg)
            .border(
                BorderStroke(
                    borderWidth,
                    Brush.linearGradient(
                        listOf(
                            actualBorder.copy(alpha = 0.5f),
                            actualBorder.copy(alpha = 0.15f)
                        )
                    )
                ),
                shape
            )
            .then(clickableModifier)
    ) {
        content()
    }
}
