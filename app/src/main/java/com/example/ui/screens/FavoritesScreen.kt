package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.ui.components.MixtunFileInfoSheet
import com.example.ui.components.MixtunFileRowItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    repository: FileRepository,
    onOpenFile: (FileRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val favoriteFiles by repository.favoriteFiles.collectAsState(initial = emptyList())
    val playbackStates by repository.allPlaybackStates.collectAsState(initial = emptyList())
    val playbackMap = remember(playbackStates) { playbackStates.associateBy { it.fileId } }

    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var selectedFileForInfo by remember { mutableStateOf<FileRecord?>(null) }

    val filterCategories = listOf(
        FileCategory.ALL,
        FileCategory.VIDEO,
        FileCategory.IMAGE,
        FileCategory.AUDIO,
        FileCategory.DOCUMENT,
        FileCategory.PDF,
        FileCategory.JSON
    )

    val displayedFiles = remember(favoriteFiles, selectedCategory) {
        if (selectedCategory == FileCategory.ALL) {
            favoriteFiles
        } else {
            favoriteFiles.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Favorites",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Category Filter Chips Row
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
                            .background(if (isSelected) MixtunRose.copy(alpha = 0.25f) else GlassSurfaceDark)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) MixtunRose else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (displayedFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Save files you want to reach quickly.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(displayedFiles) { file ->
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
