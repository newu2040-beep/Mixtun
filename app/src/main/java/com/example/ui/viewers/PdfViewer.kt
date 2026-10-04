package com.example.ui.viewers

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.data.repository.FileRepository
import com.example.engine.FilePreviewEngine
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfViewer(
    file: FileRecord,
    repository: FileRepository,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(file.pageCount ?: 1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Load initial saved reading state
    LaunchedEffect(file.id) {
        val saved = repository.getFileByIdDirect(file.id)
        val readingState = repository.getReadingStateDirect(file.id)
        if (readingState != null && readingState.page > 0) {
            currentPageIndex = readingState.page - 1
        }
    }

    // Load page bitmap whenever currentPageIndex changes
    LaunchedEffect(currentPageIndex, file.uri) {
        isLoading = true
        errorMessage = null
        try {
            val uri = Uri.parse(file.uri)
            val result = FilePreviewEngine.renderPdfPage(context, uri, currentPageIndex)
            result.onSuccess { (bitmap, total) ->
                currentBitmap = bitmap
                totalPages = total
                isLoading = false
                // Save reading progress
                repository.saveReadingState(file.id, currentPageIndex + 1, total)
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to render PDF page"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = e.message
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
                    text = "Page ${currentPageIndex + 1} of $totalPages",
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

        // PDF Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MixtunCyan)
            } else if (errorMessage != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MixtunRose,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage ?: "Error", color = TextSecondary, fontSize = 14.sp)
                }
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }
        }

        // Floating Page Navigation Capsule
        MixtunGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceDark,
            borderColor = GlassBorderCyan.copy(alpha = 0.3f),
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentPageIndex > 0) {
                            currentPageIndex--
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    enabled = currentPageIndex > 0,
                    modifier = Modifier.testTag("pdf_prev_page")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0) TextPrimary else TextTertiary
                    )
                }

                Text(
                    text = "${currentPageIndex + 1} / $totalPages",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = {
                        if (currentPageIndex < totalPages - 1) {
                            currentPageIndex++
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    enabled = currentPageIndex < totalPages - 1,
                    modifier = Modifier.testTag("pdf_next_page")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < totalPages - 1) TextPrimary else TextTertiary
                    )
                }
            }
        }
    }
}
