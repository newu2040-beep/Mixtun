package com.example.ui.viewers

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.CsvTableData
import com.example.engine.FilePreviewEngine
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

@Composable
fun CsvViewer(
    file: FileRecord,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var csvData by remember { mutableStateOf<CsvTableData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTableView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDelimiter by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(file.uri, selectedDelimiter) {
        isLoading = true
        val result = FilePreviewEngine.readText(context, Uri.parse(file.uri))
        result.onSuccess { text ->
            rawText = text
            csvData = FilePreviewEngine.parseCsv(text, selectedDelimiter)
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
                    text = "${csvData?.totalRowCount ?: 0} rows • ${csvData?.headers?.size ?: 0} columns",
                    color = MixtunCyan,
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

        // Delimiter and View Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = isTableView,
                    onClick = { isTableView = true },
                    label = { Text("Table", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MixtunCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MixtunCyan
                    )
                )
                FilterChip(
                    selected = !isTableView,
                    onClick = { isTableView = false },
                    label = { Text("Raw", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MixtunCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MixtunCyan
                    )
                )
            }

            // Delimiter chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(',' to "Comma", '\t' to "Tab", ';' to "Semicolon", '|' to "Pipe").forEach { (char, name) ->
                    val isSelected = (selectedDelimiter ?: csvData?.delimiter) == char
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MixtunCyan.copy(alpha = 0.2f) else GlassSurfaceDark)
                            .clickable { selectedDelimiter = char }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (char == '\t') "\\t" else "$char",
                            color = if (isSelected) MixtunCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Table Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MixtunCyan, modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(text = "Error: $errorMessage", color = MixtunRose, modifier = Modifier.align(Alignment.Center))
            } else if (!isTableView) {
                // Raw View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurfaceDark)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(text = rawText, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            } else {
                // Interactive Spreadsheet Table Grid
                val data = csvData
                if (data != null && data.headers.isNotEmpty()) {
                    val horizontalScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurfaceDark)
                    ) {
                        // Header Row (Sticky)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1E4A))
                                .horizontalScroll(horizontalScroll)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "#",
                                color = MixtunCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(44.dp).padding(start = 12.dp)
                            )
                            data.headers.forEach { header ->
                                Text(
                                    text = header,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(130.dp).padding(horizontal = 6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = GlassBorder)

                        // Data Rows
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(data.rows) { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScroll)
                                        .background(if (index % 2 == 0) Color.Transparent else Color(0x11FFFFFF))
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(44.dp).padding(start = 12.dp)
                                    )
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.width(130.dp).padding(horizontal = 6.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
