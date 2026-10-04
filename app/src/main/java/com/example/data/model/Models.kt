package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileCategory(val label: String) {
    ALL("All"),
    VIDEO("Videos"),
    IMAGE("Photos"),
    AUDIO("Audio"),
    PDF("PDF"),
    DOCUMENT("Documents"),
    TEXT("Text"),
    JSON("JSON"),
    CSV("CSV"),
    XML("XML"),
    MARKDOWN("Markdown"),
    HTML("HTML"),
    ARCHIVE("Archives"),
    CODE("Code"),
    UNKNOWN("Other")
}

@Entity(tableName = "files")
data class FileRecord(
    @PrimaryKey
    val id: String,
    val uri: String,
    val name: String,
    val mimeType: String,
    val extension: String,
    val size: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val category: FileCategory = FileCategory.UNKNOWN,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val pageCount: Int? = null,
    val thumbnailUri: String? = null
)

@Entity(tableName = "playback_states")
data class PlaybackState(
    @PrimaryKey
    val fileId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "reading_states")
data class ReadingState(
    @PrimaryKey
    val fileId: String,
    val page: Int = 1,
    val totalPages: Int = 1,
    val scrollOffset: Int = 0,
    val lineNumber: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class MediaCollection(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String = "#00E5FF",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collection_items", primaryKeys = ["collectionId", "fileId"])
data class MediaCollectionItem(
    val collectionId: String,
    val fileId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    val key: String,
    val value: String
)
