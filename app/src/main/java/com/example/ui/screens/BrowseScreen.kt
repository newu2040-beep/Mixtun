package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    MODIFIED("Modified"),
    NAME("Name"),
    SIZE("Size"),
    TYPE("Type")
}

@Composable
fun BrowseScreen(
    repository: FileRepository,
    initialCategory: FileCategory = FileCategory.ALL,
    onOpenFile: (FileRecord) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedSort by remember { mutableStateOf(SortOption.MODIFIED) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedFileForInfo by remember { mutableStateOf<FileRecord?>(null) }

    val allFiles by repository.allFiles.collectAsState(initial = emptyList())
    val playbackStates by repository.allPlaybackStates.collectAsState(initial = emptyList())
    val playbackMap = remember(playbackStates) { playbackStates.associateBy { it.fileId } }

    val filterCategories = listOf(
        FileCategory.ALL,
        FileCategory.VIDEO,
        FileCategory.IMAGE,
        FileCategory.AUDIO,
        FileCategory.DOCUMENT,
        FileCategory.PDF,
        FileCategory.TEXT,
        FileCategory.JSON,
        FileCategory.ARCHIVE
    )

    val filteredFiles = remember(allFiles, selectedCategory, selectedSort) {
        val list = if (selectedCategory == FileCategory.ALL) {
            allFiles
        } else {
            allFiles.filter { it.category == selectedCategory }
        }

        when (selectedSort) {
            SortOption.MODIFIED -> list.sortedByDescending { it.modifiedAt }
            SortOption.NAME -> list.sortedBy { it.name.lowercase() }
            SortOption.SIZE -> list.sortedByDescending { it.size }
            SortOption.TYPE -> list.sortedBy { it.category.name }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browse",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceDark)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceDark)
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Sort", tint = TextPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category Filter Chips Row (matching Screen 2 mockup)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterCategories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) MixtunCyan.copy(alpha = 0.25f)
                                else GlassSurfaceDark
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) MixtunCyan else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Folders Section (matching Screen 2 mockup)
        item {
            Text(
                text = "Folders",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 grid of virtual folders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderCard(
                    name = "Camera",
                    count = "248 items",
                    color = Color(0xFF00B0FF),
                    onClick = { selectedCategory = FileCategory.IMAGE },
                    modifier = Modifier.weight(1f)
                )
                FolderCard(
                    name = "Downloads",
                    count = "86 items",
                    color = Color(0xFF00E5FF),
                    onClick = { selectedCategory = FileCategory.ALL },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderCard(
                    name = "Documents",
                    count = "42 items",
                    color = Color(0xFF7C4DFF),
                    onClick = { selectedCategory = FileCategory.DOCUMENT },
                    modifier = Modifier.weight(1f)
                )
                FolderCard(
                    name = "Movies",
                    count = "12 items",
                    color = Color(0xFF651FFF),
                    onClick = { selectedCategory = FileCategory.VIDEO },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Files Header with Sort Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files (${filteredFiles.size})",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurfaceDark)
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedSort.label,
                                color = MixtunCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = MixtunCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MixtunDeepSpace)
                    ) {
                        SortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, color = if (selectedSort == opt) MixtunCyan else TextPrimary) },
                                onClick = {
                                    selectedSort = opt
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Files List
        if (filteredFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No files found in this category.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredFiles) { file ->
                val pState = playbackMap[file.id]
                MixtunFileRowItem(
                    file = file,
                    playbackState = pState,
                    readingState = null,
                    onClick = { onOpenFile(file) },
                    onMoreClick = { selectedFileForInfo = file }
                )
            }
        }
    }

    if (selectedFileForInfo != null) {
        val target = selectedFileForInfo!!
        MixtunFileInfoSheet(
            file = target,
            onDismiss = { selectedFileForInfo = null },
            onFavoriteToggle = {
                scope.launch {
                    repository.setFavorite(target.id, !target.isFavorite)
                    selectedFileForInfo = target.copy(isFavorite = !target.isFavorite)
                }
            },
            onShare = {
                selectedFileForInfo = null
                try {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = target.mimeType.ifEmpty { "*/*" }
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(target.uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share ${target.name}"))
                } catch (_: Exception) {}
            },
            onOpenWith = {
                selectedFileForInfo = null
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(target.uri), target.mimeType.ifEmpty { "*/*" })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(viewIntent, "Open with"))
                } catch (_: Exception) {}
            },
            onDelete = {
                selectedFileForInfo = null
                scope.launch { repository.deleteFile(target.id) }
            }
        )
    }
}

@Composable
private fun FolderCard(
    name: String,
    count: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MixtunGlassSurface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassSurfaceDark,
        borderColor = color.copy(alpha = 0.25f),
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = count,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
