package com.example.ui.viewers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.media.PlaybackManager
import com.example.ui.components.MixtunFileInfoSheet
import com.example.ui.theme.MixtunCyan
import com.example.ui.theme.MixtunDarkSurface
import com.example.ui.theme.MixtunRose
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun UniversalViewerScreen(
    file: FileRecord,
    repository: FileRepository,
    playbackManager: PlaybackManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentFile by remember(file) { mutableStateOf(file) }
    var isInfoSheetVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var forceTextMode by remember { mutableStateOf(false) }

    // Update last opened timestamp
    LaunchedEffect(currentFile.id) {
        repository.updateLastOpened(currentFile.id)
    }

    // Auto-play media if category is video or audio
    LaunchedEffect(currentFile.id, currentFile.category) {
        if (currentFile.category == FileCategory.VIDEO || currentFile.category == FileCategory.AUDIO) {
            playbackManager.play(currentFile)
        }
    }

    BackHandler {
        if (currentFile.category == FileCategory.VIDEO || currentFile.category == FileCategory.AUDIO) {
            playbackManager.showMiniPlayer()
        }
        onBack()
    }

    fun handleShare() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = currentFile.mimeType.ifEmpty { "*/*" }
                putExtra(Intent.EXTRA_STREAM, Uri.parse(currentFile.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${currentFile.name}"))
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot share this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleOpenWith() {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(currentFile.uri), currentFile.mimeType.ifEmpty { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open with"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFavorite() {
        scope.launch {
            val newFav = !currentFile.isFavorite
            repository.setFavorite(currentFile.id, newFav)
            currentFile = currentFile.copy(isFavorite = newFav)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val effectiveCategory = if (forceTextMode) FileCategory.TEXT else currentFile.category

        when (effectiveCategory) {
            FileCategory.VIDEO -> {
                VideoViewer(
                    file = currentFile,
                    playbackManager = playbackManager,
                    onBack = {
                        playbackManager.showMiniPlayer()
                        onBack()
                    },
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() }
                )
            }
            FileCategory.AUDIO -> {
                AudioViewer(
                    file = currentFile,
                    playbackManager = playbackManager,
                    onBack = {
                        playbackManager.showMiniPlayer()
                        onBack()
                    },
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() }
                )
            }
            FileCategory.IMAGE -> {
                PhotoViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.PDF -> {
                PdfViewer(
                    file = currentFile,
                    repository = repository,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() }
                )
            }
            FileCategory.JSON -> {
                JsonViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() },
                    onOpenWith = { handleOpenWith() }
                )
            }
            FileCategory.CSV -> {
                CsvViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.TEXT, FileCategory.CODE -> {
                TextViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.XML -> {
                XmlViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.MARKDOWN -> {
                MarkdownViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.HTML -> {
                HtmlViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.ARCHIVE -> {
                ArchiveViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onFavoriteToggle = { toggleFavorite() },
                    onShare = { handleShare() }
                )
            }
            FileCategory.DOCUMENT, FileCategory.UNKNOWN, FileCategory.ALL -> {
                UnsupportedViewer(
                    file = currentFile,
                    onBack = onBack,
                    onShowInfo = { isInfoSheetVisible = true },
                    onOpenWith = { handleOpenWith() },
                    onShare = { handleShare() },
                    onViewAsText = { forceTextMode = true }
                )
            }
        }

        // Info Bottom Sheet
        if (isInfoSheetVisible) {
            MixtunFileInfoSheet(
                file = currentFile,
                onDismiss = { isInfoSheetVisible = false },
                onFavoriteToggle = { toggleFavorite() },
                onShare = {
                    isInfoSheetVisible = false
                    handleShare()
                },
                onOpenWith = {
                    isInfoSheetVisible = false
                    handleOpenWith()
                },
                onDelete = {
                    isInfoSheetVisible = false
                    showDeleteDialog = true
                }
            )
        }

        // Confirm Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = MixtunDarkSurface,
                title = { Text("Remove from Mixtun?", color = TextPrimary) },
                text = { Text("This will remove '${currentFile.name}' from your history and recents.", color = TextPrimary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                repository.deleteFile(currentFile.id)
                                onBack()
                            }
                        }
                    ) {
                        Text("Remove", color = MixtunRose)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = MixtunCyan)
                    }
                }
            )
        }
    }
}
