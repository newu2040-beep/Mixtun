package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.FileCategory
import com.example.ui.theme.*

@Composable
fun MixtunFileIcon(
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val (icon, tint, bgAlpha) = when (category) {
        FileCategory.VIDEO -> Triple(Icons.Outlined.PlayCircleOutline, MixtunCyan, 0.15f)
        FileCategory.IMAGE -> Triple(Icons.Outlined.Image, Color(0xFF4FC3F7), 0.15f)
        FileCategory.AUDIO -> Triple(Icons.Outlined.GraphicEq, MixtunBlue, 0.15f)
        FileCategory.PDF -> Triple(Icons.Outlined.PictureAsPdf, Color(0xFFFF5252), 0.15f)
        FileCategory.TEXT -> Triple(Icons.Outlined.TextSnippet, MixtunEmerald, 0.15f)
        FileCategory.JSON -> Triple(Icons.Outlined.DataObject, MixtunAmber, 0.15f)
        FileCategory.CSV -> Triple(Icons.Outlined.TableChart, Color(0xFF00BFA5), 0.15f)
        FileCategory.XML -> Triple(Icons.Outlined.Code, Color(0xFFFF9100), 0.15f)
        FileCategory.MARKDOWN -> Triple(Icons.Outlined.Article, MixtunViolet, 0.15f)
        FileCategory.HTML -> Triple(Icons.Outlined.Language, Color(0xFF29B6F6), 0.15f)
        FileCategory.ARCHIVE -> Triple(Icons.Outlined.FolderZip, Color(0xFFFFAB00), 0.15f)
        FileCategory.CODE -> Triple(Icons.Outlined.Terminal, Color(0xFF8C9EFF), 0.15f)
        FileCategory.DOCUMENT -> Triple(Icons.Outlined.Description, Color(0xFF448AFF), 0.15f)
        FileCategory.ALL, FileCategory.UNKNOWN -> Triple(Icons.Outlined.InsertDriveFile, TextSecondary, 0.12f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.label,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
