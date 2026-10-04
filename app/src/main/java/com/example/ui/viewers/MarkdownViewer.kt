package com.example.ui.viewers

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

@Composable
fun MarkdownViewer(
    file: FileRecord,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isReadMode by remember { mutableStateOf(true) }

    LaunchedEffect(file.uri) {
        isLoading = true
        val result = FilePreviewEngine.readText(context, Uri.parse(file.uri))
        result.onSuccess { text ->
            rawText = text
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "Markdown • ${FilePreviewEngine.formatFileSize(file.size)}",
                    color = MixtunCyan,
                    fontSize = 12.sp
                )
            }

            // Mode toggle
            TextButton(onClick = { isReadMode = !isReadMode }) {
                Text(if (isReadMode) "Source" else "Preview", color = MixtunCyan, fontSize = 13.sp)
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

        // Markdown Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurfaceDark)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MixtunCyan, modifier = Modifier.align(Alignment.Center))
            } else if (!isReadMode) {
                // Source
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = rawText,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Formatted Markdown
                val lines = rawText.lines()
                var inCodeBlock = false
                val codeBlockBuilder = StringBuilder()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lines) { line ->
                        val trimmed = line.trim()

                        when {
                            trimmed.startsWith("```") -> {
                                inCodeBlock = !inCodeBlock
                                if (!inCodeBlock) {
                                    val blockText = codeBlockBuilder.toString()
                                    codeBlockBuilder.setLength(0)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF040714))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = blockText,
                                            color = MixtunCyan,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            inCodeBlock -> {
                                codeBlockBuilder.append(line).append("\n")
                            }
                            trimmed.startsWith("# ") -> {
                                Text(
                                    text = trimmed.removePrefix("# "),
                                    color = MixtunCyan,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            trimmed.startsWith("## ") -> {
                                Text(
                                    text = trimmed.removePrefix("## "),
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            trimmed.startsWith("### ") -> {
                                Text(
                                    text = trimmed.removePrefix("### "),
                                    color = MixtunBlue,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            trimmed.startsWith("> ") -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(28.dp)
                                            .background(MixtunCyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = trimmed.removePrefix("> "),
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                Row(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("• ", color = MixtunCyan, fontSize = 14.sp)
                                    Text(
                                        text = trimmed.substring(2),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            trimmed.isEmpty() -> {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            else -> {
                                Text(
                                    text = trimmed,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
