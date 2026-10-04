package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.ui.components.MixtunFileInfoSheet
import com.example.ui.components.MixtunFileRowItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun RecentScreen(
    repository: FileRepository,
    onOpenFile: (FileRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allFiles by repository.recentFiles.collectAsState(initial = emptyList())
    val playbackStates by repository.allPlaybackStates.collectAsState(initial = emptyList())
    val playbackMap = remember(playbackStates) { playbackStates.associateBy { it.fileId } }

    var selectedFileForInfo by remember { mutableStateOf<FileRecord?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Group files by Today, Yesterday, Earlier
    val groupedFiles = remember(allFiles) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = todayCal.timeInMillis
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000

        val todayList = mutableListOf<FileRecord>()
        val yesterdayList = mutableListOf<FileRecord>()
        val earlierList = mutableListOf<FileRecord>()

        allFiles.forEach { file ->
            when {
                file.lastOpenedAt >= todayStart -> todayList.add(file)
                file.lastOpenedAt >= yesterdayStart -> yesterdayList.add(file)
                else -> earlierList.add(file)
            }
        }

        listOf(
            "Today" to todayList,
            "Yesterday" to yesterdayList,
            "Earlier" to earlierList
        ).filter { it.second.isNotEmpty() }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recents",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (allFiles.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MixtunRose
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (groupedFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent files opened yet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            groupedFiles.forEach { (sectionTitle, files) ->
                item {
                    Text(
                        text = sectionTitle,
                        color = MixtunCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(files) { file ->
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
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = MixtunDarkSurface,
            title = { Text("Clear History?", color = TextPrimary) },
            text = { Text("This will clear your recent file records. Your actual files will not be deleted.", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    scope.launch { repository.clearHistory() }
                }) {
                    Text("Clear", color = MixtunRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = MixtunCyan)
                }
            }
        )
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
