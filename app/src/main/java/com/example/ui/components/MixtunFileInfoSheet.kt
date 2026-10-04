package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixtunFileInfoSheet(
    file: FileRecord,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MixtunDarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(GlassBorder)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                MixtunFileIcon(
                    category = file.category,
                    size = 52.dp,
                    iconSize = 28.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${file.category.label} • ${FilePreviewEngine.formatFileSize(file.size)}",
                        color = MixtunCyan,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row (Copy, Share, Favorite, Open With)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoActionButton(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Copy Path",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(file.uri))
                    }
                )
                InfoActionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    onClick = onShare
                )
                InfoActionButton(
                    icon = if (file.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = if (file.isFavorite) "Favorited" else "Favorite",
                    iconTint = if (file.isFavorite) MixtunRose else TextPrimary,
                    onClick = onFavoriteToggle
                )
                InfoActionButton(
                    icon = Icons.Outlined.OpenInNew,
                    label = "Open With",
                    onClick = onOpenWith
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = GlassBorder.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata items
            Text(
                text = "File Details",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Format", value = file.extension.uppercase().ifEmpty { file.category.label })
            DetailRow(label = "MIME Type", value = file.mimeType)
            DetailRow(label = "Size", value = "${file.size} bytes (${FilePreviewEngine.formatFileSize(file.size)})")
            DetailRow(label = "Modified", value = FilePreviewEngine.formatDate(file.modifiedAt))

            if (file.width != null && file.height != null) {
                DetailRow(label = "Resolution", value = "${file.width} × ${file.height}")
            }
            if (file.durationMs != null && file.durationMs > 0) {
                DetailRow(label = "Duration", value = FilePreviewEngine.formatDuration(file.durationMs))
            }
            if (file.pageCount != null && file.pageCount > 0) {
                DetailRow(label = "Pages", value = "${file.pageCount} pages")
            }

            DetailRow(label = "Location", value = file.uri)

            Spacer(modifier = Modifier.height(20.dp))

            // Delete action button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_file_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MixtunRose),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MixtunRose.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete File Entry")
            }
        }
    }
}

@Composable
private fun InfoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GlassSurfaceLighter)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
