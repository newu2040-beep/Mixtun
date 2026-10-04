package com.example.ui.viewers

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileRecord
import com.example.engine.FilePreviewEngine
import com.example.engine.JsonNode
import com.example.engine.JsonNodeType
import com.example.ui.components.MixtunFileIcon
import com.example.ui.components.MixtunGlassSurface
import com.example.ui.theme.*

enum class JsonViewMode {
    TREE, RAW
}

@Composable
fun JsonViewer(
    file: FileRecord,
    onBack: () -> Unit,
    onShowInfo: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current

    var viewMode by remember { mutableStateOf(JsonViewMode.TREE) }
    var rawText by remember { mutableStateOf("") }
    var rootNode by remember { mutableStateOf<JsonNode?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(file.uri) {
        isLoading = true
        val result = FilePreviewEngine.readText(context, Uri.parse(file.uri))
        result.onSuccess { text ->
            rawText = text
            val treeResult = FilePreviewEngine.parseJsonTree(text)
            treeResult.onSuccess { node ->
                rootNode = node
                isLoading = false
            }.onFailure {
                // If tree fails, fallback to raw mode
                viewMode = JsonViewMode.RAW
                isLoading = false
            }
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                onClick = onShowInfo,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = TextPrimary)
            }
        }

        // Search Bar (if active)
        AnimatedVisibility(visible = isSearchActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter keys or values...", color = TextTertiary) },
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

        // Tree vs Raw Mode Selector (matching mockup screen 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MixtunGlassSurface(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = GlassSurfaceDark,
                borderColor = GlassBorder.copy(alpha = 0.2f),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp)
                ) {
                    ModePill(
                        label = "Tree",
                        selected = viewMode == JsonViewMode.TREE,
                        onClick = { viewMode = JsonViewMode.TREE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ModePill(
                        label = "Raw",
                        selected = viewMode == JsonViewMode.RAW,
                        onClick = { viewMode = JsonViewMode.RAW }
                    )
                }
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MixtunCyan)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $errorMessage", color = MixtunRose)
                }
            } else if (viewMode == JsonViewMode.TREE && rootNode != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassSurfaceDark.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    item {
                        Text(
                            text = "{",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }

                    items(rootNode!!.children) { child ->
                        JsonTreeNodeItem(
                            node = child,
                            depth = 1,
                            searchQuery = searchQuery,
                            onCopyPath = { path ->
                                clipboard.setText(AnnotatedString(path))
                                Toast.makeText(context, "Path copied: $path", Toast.LENGTH_SHORT).show()
                            },
                            onCopyValue = { valStr ->
                                clipboard.setText(AnnotatedString(valStr))
                                Toast.makeText(context, "Value copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        Text(
                            text = "}",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // RAW view
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassSurfaceDark.copy(alpha = 0.7f))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = rawText,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Bottom Action Container (Screen 4 mockup)
        MixtunGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassSurfaceDark.copy(alpha = 0.9f),
            borderColor = GlassBorderCyan.copy(alpha = 0.25f),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top row with file details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MixtunFileIcon(category = file.category, size = 36.dp, iconSize = 20.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "JSON • ${FilePreviewEngine.formatFileSize(file.size)} • ${FilePreviewEngine.formatDate(file.modifiedAt)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action buttons (Copy, Share, Favorite, Open With)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ActionTile(
                        icon = Icons.Outlined.ContentCopy,
                        label = "Copy",
                        onClick = {
                            clipboard.setText(AnnotatedString(rawText))
                            Toast.makeText(context, "Full JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ActionTile(icon = Icons.Outlined.Share, label = "Share", onClick = onShare)
                    ActionTile(
                        icon = if (file.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        label = "Favorite",
                        iconTint = if (file.isFavorite) MixtunRose else TextPrimary,
                        onClick = onFavoriteToggle
                    )
                    ActionTile(icon = Icons.Outlined.OpenInNew, label = "Open With", onClick = onOpenWith)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // File Info pill button
                Button(
                    onClick = onShowInfo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLighter)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MixtunCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("File Info", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun JsonTreeNodeItem(
    node: JsonNode,
    depth: Int,
    searchQuery: String,
    onCopyPath: (String) -> Unit,
    onCopyValue: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(depth < 2) }
    val isContainer = node.type is JsonNodeType.ObjectType || node.type is JsonNodeType.ArrayType

    val valueColor = when (node.type) {
        is JsonNodeType.StringType -> Color(0xFF81C784) // green
        is JsonNodeType.NumberType -> Color(0xFFFFB74D) // amber/yellow
        is JsonNodeType.BooleanType -> Color(0xFFBA68C8) // violet
        is JsonNodeType.NullType -> Color(0xFFE57373) // red
        else -> MixtunCyan
    }

    val matchesSearch = searchQuery.isNotEmpty() &&
            (node.key.contains(searchQuery, ignoreCase = true) || node.value.contains(searchQuery, ignoreCase = true))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp, top = 3.dp, bottom = 3.dp)
            .background(if (matchesSearch) MixtunCyan.copy(alpha = 0.15f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isContainer) {
                        isExpanded = !isExpanded
                    } else {
                        onCopyValue(node.value)
                    }
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isContainer) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MixtunCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }

            Text(
                text = node.key,
                color = MixtunCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = ": ",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            Text(
                text = node.value,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        if (isContainer && isExpanded) {
            node.children.forEach { child ->
                JsonTreeNodeItem(
                    node = child,
                    depth = depth + 1,
                    searchQuery = searchQuery,
                    onCopyPath = onCopyPath,
                    onCopyValue = onCopyValue
                )
            }
        }
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MixtunCyan.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) MixtunCyan else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}
