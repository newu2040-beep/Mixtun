package com.example.ui.viewers

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

@Composable
fun TextViewer(
    file: FileRecord,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    var rawText by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showLineNumbers by remember { mutableStateOf(true) }
    var isMonospace by remember { mutableStateOf(true) }
    var fontSizeSp by remember { mutableFloatStateOf(13f) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(file.uri) {
        isLoading = true
        val result = FilePreviewEngine.readText(context, Uri.parse(file.uri))
        result.onSuccess { text ->
            rawText = text
            lines = text.lines()
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.message
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
                    text = "${lines.size} lines • ${FilePreviewEngine.formatFileSize(file.size)}",
                    color = MixtunCyan,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { isSearchActive = !isSearchActive },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) MixtunCyan else TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

        // Search Bar
        AnimatedVisibility(visible = isSearchActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Find in text...", color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = MixtunCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = GlassSurfaceDark,
                        unfocusedContainerColor = GlassSurfaceDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Toolbar: Line numbers, Monospace, Font Size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = showLineNumbers,
                    onClick = { showLineNumbers = !showLineNumbers },
                    label = { Text("Lines", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MixtunCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MixtunCyan
                    )
                )
                FilterChip(
                    selected = isMonospace,
                    onClick = { isMonospace = !isMonospace },
                    label = { Text("Mono", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MixtunCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MixtunCyan
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (fontSizeSp > 10f) fontSizeSp -= 1f },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("A-", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("${fontSizeSp.toInt()}sp", color = MixtunCyan, fontSize = 12.sp)
                IconButton(
                    onClick = { if (fontSizeSp < 24f) fontSizeSp += 1f },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("A+", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(rawText))
                        Toast.makeText(context, "Copied text to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy All", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Text Canvas with line numbers
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassSurfaceDark)
                .padding(10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MixtunCyan, modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(text = "Error: $errorMessage", color = MixtunRose, modifier = Modifier.align(Alignment.Center))
            } else {
                SelectionContainer {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(lines) { index, line ->
                            val matches = searchQuery.isNotEmpty() && line.contains(searchQuery, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (matches) MixtunCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(vertical = 1.dp)
                            ) {
                                if (showLineNumbers) {
                                    Text(
                                        text = "${index + 1}",
                                        color = TextTertiary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (fontSizeSp - 2).sp,
                                        modifier = Modifier.width(38.dp)
                                    )
                                }
                                Text(
                                    text = line,
                                    color = TextPrimary,
                                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp + 6).sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
