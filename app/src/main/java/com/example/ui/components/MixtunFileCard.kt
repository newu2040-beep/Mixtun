package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.data.model.PlaybackState
import com.example.data.model.ReadingState
import com.example.engine.FilePreviewEngine
import com.example.ui.theme.*

@Composable
fun MixtunFileRowItem(
    file: FileRecord,
    playbackState: PlaybackState?,
    readingState: ReadingState?,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MixtunGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassSurfaceDark.copy(alpha = 0.7f),
        borderColor = GlassBorder.copy(alpha = 0.15f),
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MixtunFileIcon(
                category = file.category,
                size = 48.dp,
                iconSize = 24.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sizeStr = FilePreviewEngine.formatFileSize(file.size)
                    val dateStr = FilePreviewEngine.formatDate(file.modifiedAt)
                    Text(
                        text = "$sizeStr • $dateStr • ${file.category.label}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // If playback state exists, show sleek progress line
                if (playbackState != null && playbackState.durationMs > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val progress = (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MixtunCyan,
                        trackColor = Color(0x3300E5FF)
                    )
                } else if (readingState != null && readingState.totalPages > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val progress = (readingState.page.toFloat() / readingState.totalPages.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MixtunViolet,
                        trackColor = Color(0x337C4DFF)
                    )
                }
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("file_more_${file.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MixtunContinueCard(
    file: FileRecord,
    playbackState: PlaybackState?,
    readingState: ReadingState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MixtunGlassSurface(
        modifier = modifier
            .width(135.dp)
            .height(145.dp),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = GlassSurfaceDark,
        borderColor = GlassBorderCyan.copy(alpha = 0.25f),
        elevation = 6.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1B3E)),
                contentAlignment = Alignment.Center
            ) {
                MixtunFileIcon(
                    category = file.category,
                    size = 40.dp,
                    iconSize = 22.dp
                )

                // Bottom progress line on thumbnail
                val progress = when {
                    playbackState != null && playbackState.durationMs > 0 ->
                        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                    readingState != null && readingState.totalPages > 1 ->
                        (readingState.page.toFloat() / readingState.totalPages.toFloat()).coerceIn(0f, 1f)
                    else -> null
                }

                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MixtunCyan,
                        trackColor = Color(0x3300E5FF)
                    )
                }
            }

            Column {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = when {
                    playbackState != null && playbackState.durationMs > 0 -> {
                        val pct = ((playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()) * 100).toInt()
                        val remainingMs = (playbackState.durationMs - playbackState.positionMs).coerceAtLeast(0)
                        "$pct% • ${FilePreviewEngine.formatDuration(remainingMs)} left"
                    }
                    readingState != null -> {
                        "Page ${readingState.page} / ${readingState.totalPages}"
                    }
                    else -> FilePreviewEngine.formatRelativeTime(file.lastOpenedAt)
                }
                Text(
                    text = subtitle,
                    color = MixtunCyan,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
