package com.example.ui.viewers

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.media.PlaybackManager
import com.example.ui.components.MixtunFileIcon
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

@Composable
fun AudioViewer(
    file: FileRecord,
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()

    // Subtle audio wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MixtunBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Audio • ${FilePreviewEngine.formatFileSize(file.size)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
            ) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (file.isFavorite) MixtunRose else TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onShowInfo,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Center Album Art / Glowing Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val radius = size.minDimension / 2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MixtunBlue.copy(alpha = 0.35f),
                            MixtunCyan.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    radius = radius
                )
            }

            MixtunGlassSurface(
                modifier = Modifier.size(190.dp),
                shape = CircleShape,
                backgroundColor = GlassSurfaceDark,
                borderColor = MixtunCyan.copy(alpha = 0.4f),
                elevation = 16.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MixtunFileIcon(
                        category = file.category,
                        size = 80.dp,
                        iconSize = 44.dp
                    )
                }
            }
        }

        // Live Audio Visualizer Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barCount = 20
            for (i in 0 until barCount) {
                val animHeight = if (isPlaying) {
                    val factor = Math.sin((i.toFloat() / barCount.toFloat() * Math.PI * 2) + wavePhase * Math.PI * 2).toFloat()
                    (12.dp + (24.dp * (factor * 0.5f + 0.5f)))
                } else {
                    8.dp
                }

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(animHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(MixtunCyan, MixtunBlue)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Bottom Controls Container
        MixtunGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = GlassSurfaceDark.copy(alpha = 0.85f),
            borderColor = GlassBorderCyan.copy(alpha = 0.3f),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Scrubber
                val totalDuration = if (duration > 0) duration else (file.durationMs ?: 1L)
                Slider(
                    value = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f),
                    onValueChange = { fraction ->
                        playbackManager.seekTo((fraction * totalDuration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MixtunCyan,
                        activeTrackColor = MixtunCyan,
                        inactiveTrackColor = Color(0x3300E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FilePreviewEngine.formatDuration(currentPosition),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = FilePreviewEngine.formatDuration(totalDuration),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control buttons: Rewind 10, Play/Pause, Forward 10, Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            playbackManager.setSpeed(nextSpeed)
                        }
                    ) {
                        Text("${playbackSpeed}x", color = MixtunCyan, fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = { playbackManager.seekBy(-10_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceLighter)
                            .testTag("audio_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { playbackManager.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MixtunCyan)
                            .testTag("audio_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MixtunNavy,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { playbackManager.seekBy(10_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceLighter)
                            .testTag("audio_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forward10,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { playbackManager.toggleMute() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Volume",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
