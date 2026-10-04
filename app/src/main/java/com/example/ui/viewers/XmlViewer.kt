package com.example.ui.viewers

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.engine.XmlNode
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

@Composable
fun XmlViewer(
    file: FileRecord,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var rootNode by remember { mutableStateOf<XmlNode?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isTreeView by remember { mutableStateOf(true) }

    LaunchedEffect(file.uri) {
        isLoading = true
        val result = FilePreviewEngine.readText(context, Uri.parse(file.uri))
        result.onSuccess { text ->
            rawText = text
            val xmlRes = FilePreviewEngine.parseXml(text)
            xmlRes.onSuccess { node ->
                rootNode = node
            }
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
                    text = "XML • ${FilePreviewEngine.formatFileSize(file.size)}",
                    color = MixtunCyan,
                    fontSize = 12.sp
                )
            }

            TextButton(onClick = { isTreeView = !isTreeView }) {
                Text(if (isTreeView) "Raw" else "Tree", color = MixtunCyan, fontSize = 13.sp)
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

        // XML Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurfaceDark)
                .padding(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MixtunCyan, modifier = Modifier.align(Alignment.Center))
            } else if (!isTreeView || rootNode == null) {
                // Raw View
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = rawText,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Tree View
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        XmlTreeNodeItem(node = rootNode!!, depth = 0)
                    }
                }
            }
        }
    }
}

@Composable
fun XmlTreeNodeItem(node: XmlNode, depth: Int) {
    var isExpanded by remember { mutableStateOf(depth < 2) }
    val hasChildren = node.children.isNotEmpty()

    Column(modifier = Modifier.padding(start = (depth * 14).dp, top = 3.dp, bottom = 3.dp)) {
        Row(
            modifier = Modifier
                .clickable(enabled = hasChildren) { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MixtunCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = "<${node.name}",
                color = MixtunCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Attributes
            node.attributes.forEach { (k, v) ->
                Text(
                    text = " $k=\"$v\"",
                    color = MixtunAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            Text(
                text = if (hasChildren || !node.textContent.isNullOrBlank()) ">" else " />",
                color = MixtunCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            if (!node.textContent.isNullOrBlank()) {
                Text(
                    text = node.textContent,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                if (!hasChildren) {
                    Text(
                        text = "</${node.name}>",
                        color = MixtunCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (hasChildren && isExpanded) {
            node.children.forEach { child ->
                XmlTreeNodeItem(node = child, depth = depth + 1)
            }
            Text(
                text = "</${node.name}>",
                color = MixtunCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = (depth * 14).dp)
            )
        }
    }
}
