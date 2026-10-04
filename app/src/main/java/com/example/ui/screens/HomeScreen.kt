package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: FileRepository,
    onOpenFile: (FileRecord) -> Unit,
    onNavigateToCategory: (FileCategory) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pastel = LocalPastelTheme.current

    val allFiles by repository.allFiles.collectAsState(initial = emptyList())
    val recentFiles by repository.recentFiles.collectAsState(initial = emptyList())
    val playbackStates by repository.allPlaybackStates.collectAsState(initial = emptyList())

    val playbackMap = remember(playbackStates) {
        playbackStates.associateBy { it.fileId }
    }

    var selectedFileForInfo by remember { mutableStateOf<FileRecord?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FileRecord?>(null) }
    var isManualScanning by remember { mutableStateOf(false) }

    // SAF file picker launchers
    val openSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val record = repository.importUri(uri)
                if (record != null) {
                    onOpenFile(record)
                } else {
                    Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var firstRecord: FileRecord? = null
                uris.forEach { u ->
                    val r = repository.importUri(u)
                    if (firstRecord == null) firstRecord = r
                }
                if (firstRecord != null) {
                    onOpenFile(firstRecord!!)
                }
            }
        }
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Folder selected: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter "Continue Watching / Reading" items (files with active playback or reading states)
    val continueItems = remember(allFiles, playbackMap) {
        allFiles.filter { file ->
            val pState = playbackMap[file.id]
            (pState != null && pState.positionMs > 0 && !pState.completed)
        }.take(8)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Brand Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(pastel.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = "Mixtun Logo",
                        tint = pastel.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MIXTUN",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Everything you have. One beautiful viewer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Scan real device files button
                IconButton(
                    onClick = {
                        scope.launch {
                            isManualScanning = true
                            val count = repository.scanDeviceMedia()
                            isManualScanning = false
                            Toast.makeText(context, "Scanned $count realtime media files", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceDark)
                        .testTag("home_scan_button")
                ) {
                    if (isManualScanning) {
                        CircularProgressIndicator(
                            color = pastel.primary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Scan Media", tint = pastel.primary)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceDark)
                        .testTag("home_search_button")
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Permissions Request Card (Gallery, Notifications, Storage)
        item {
            MixtunPermissionsCard(
                repository = repository,
                onPermissionsUpdated = {
                    // Triggers realtime refresh
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Bar Capsule
        item {
            MixtunGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = null,
                onClick = onSearchClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = pastel.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search realtime files, folders, formats...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // "Continue Watching" Section (Only shown if user has active media/documents in progress)
        if (continueItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue Watching",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { onNavigateToCategory(FileCategory.ALL) }) {
                        Text("See all", color = pastel.primary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueItems) { file ->
                        val pState = playbackMap[file.id]
                        MixtunContinueCard(
                            file = file,
                            playbackState = pState,
                            readingState = null,
                            onClick = { onOpenFile(file) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // "File Types" Grid Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File Types",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { onNavigateToCategory(FileCategory.ALL) }) {
                    Text("See all", color = pastel.primary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val typeShortcuts = listOf(
                FileCategory.VIDEO,
                FileCategory.IMAGE,
                FileCategory.AUDIO,
                FileCategory.PDF,
                FileCategory.DOCUMENT,
                FileCategory.TEXT,
                FileCategory.JSON,
                FileCategory.ARCHIVE
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    typeShortcuts.take(4).forEach { cat ->
                        FileTypeShortcutTile(
                            category = cat,
                            onClick = { onNavigateToCategory(cat) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    typeShortcuts.drop(4).forEach { cat ->
                        FileTypeShortcutTile(
                            category = cat,
                            onClick = { onNavigateToCategory(cat) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // "Recent Files" List Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Files (${recentFiles.size})",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { onNavigateToCategory(FileCategory.ALL) }) {
                    Text("Browse All", color = pastel.primary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val displayedRecents = recentFiles.take(8)
        if (displayedRecents.isEmpty()) {
            item {
                MixtunGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = pastel.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Realtime Files Loaded Yet",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant media permissions to scan your phone's gallery, videos, and music, or tap below to pick any file.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { openSingleLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = pastel.primary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open a File Now")
                        }
                    }
                }
            }
        } else {
            items(displayedRecents) { file ->
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

        // "Quick Import" Glass Action Buttons
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Quick Import",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // + Open File
                Button(
                    onClick = { openSingleLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_import_file"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pastel.primary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open File", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                // Open Folder
                OutlinedButton(
                    onClick = { openTreeLauncher.launch(null) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_import_folder"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(pastel.primary.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = pastel.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Folder", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Info Sheet
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
                showDeleteDialog = target
            }
        )
    }

    // Delete dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Remove from Mixtun?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Remove '${showDeleteDialog!!.name}' from recent records?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = {
                    val id = showDeleteDialog!!.id
                    showDeleteDialog = null
                    scope.launch { repository.deleteFile(id) }
                }) {
                    Text("Remove", color = MixtunRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = pastel.primary)
                }
            }
        )
    }
}

@Composable
private fun FileTypeShortcutTile(
    category: FileCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MixtunGlassSurface(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MixtunFileIcon(category = category, size = 32.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.label,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
