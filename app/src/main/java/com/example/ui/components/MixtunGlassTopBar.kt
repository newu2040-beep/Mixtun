package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MixtunGlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    onBackClick: (() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onFavoriteToggle: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onOpenWithClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
                    .testTag("topbar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MixtunCyan.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = MixtunCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (onSearchClick != null) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
                    .testTag("topbar_search_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isFavorite != null && onFavoriteToggle != null) {
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
                    .testTag("topbar_favorite_button")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Favorited" else "Favorite",
                    tint = if (isFavorite) MixtunRose else TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (onInfoClick != null || onShareClick != null || onOpenWithClick != null || onDeleteClick != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceDark)
                        .testTag("topbar_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More Options",
                        tint = TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MixtunDeepSpace)
                ) {
                    if (onInfoClick != null) {
                        DropdownMenuItem(
                            text = { Text("File Info", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Info, null, tint = MixtunCyan) },
                            onClick = {
                                menuExpanded = false
                                onInfoClick()
                            }
                        )
                    }
                    if (onShareClick != null) {
                        DropdownMenuItem(
                            text = { Text("Share", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, tint = MixtunBlue) },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            }
                        )
                    }
                    if (onOpenWithClick != null) {
                        DropdownMenuItem(
                            text = { Text("Open With", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, null, tint = MixtunViolet) },
                            onClick = {
                                menuExpanded = false
                                onOpenWithClick()
                            }
                        )
                    }
                    if (onDeleteClick != null) {
                        HorizontalDivider(color = GlassBorder)
                        DropdownMenuItem(
                            text = { Text("Delete", color = MixtunRose) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MixtunRose) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
