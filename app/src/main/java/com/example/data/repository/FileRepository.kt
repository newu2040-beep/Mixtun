package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.db.MixtunDao
import com.example.data.model.*
import com.example.engine.FileTypeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FileRepository(
    private val context: Context,
    private val dao: MixtunDao
) {
    val allFiles: Flow<List<FileRecord>> = dao.getAllFiles()
    val recentFiles: Flow<List<FileRecord>> = dao.getRecentFiles(30)
    val favoriteFiles: Flow<List<FileRecord>> = dao.getFavoriteFiles()
    val allPlaybackStates: Flow<List<PlaybackState>> = dao.getAllPlaybackStates()
    val collections: Flow<List<MediaCollection>> = dao.getAllCollections()

    fun getFilesByCategory(category: FileCategory): Flow<List<FileRecord>> =
        dao.getFilesByCategory(category.name)

    fun getFileById(id: String): Flow<FileRecord?> = dao.getFileById(id)

    suspend fun getFileByIdDirect(id: String): FileRecord? = dao.getFileByIdDirect(id)

    fun getPlaybackState(fileId: String): Flow<PlaybackState?> = dao.getPlaybackState(fileId)

    suspend fun getPlaybackStateDirect(fileId: String): PlaybackState? = dao.getPlaybackStateDirect(fileId)

    suspend fun savePlaybackState(fileId: String, positionMs: Long, durationMs: Long, completed: Boolean = false) {
        dao.savePlaybackState(
            PlaybackState(
                fileId = fileId,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
                completed = completed
            )
        )
    }

    fun getReadingState(fileId: String): Flow<ReadingState?> = dao.getReadingState(fileId)

    suspend fun getReadingStateDirect(fileId: String): ReadingState? = dao.getReadingStateDirect(fileId)

    suspend fun saveReadingState(fileId: String, page: Int, totalPages: Int, scrollOffset: Int = 0, lineNumber: Int = 1) {
        dao.saveReadingState(
            ReadingState(
                fileId = fileId,
                page = page,
                totalPages = totalPages,
                scrollOffset = scrollOffset,
                lineNumber = lineNumber,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setFavorite(fileId: String, isFavorite: Boolean) {
        dao.setFavorite(fileId, isFavorite)
    }

    suspend fun updateLastOpened(fileId: String) {
        dao.updateLastOpened(fileId, System.currentTimeMillis())
    }

    suspend fun deleteFile(fileId: String) {
        dao.deleteFileById(fileId)
        dao.deletePlaybackState(fileId)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    fun searchFiles(query: String): Flow<List<FileRecord>> = dao.searchFiles(query)

    suspend fun importUri(uri: Uri): FileRecord? = withContext(Dispatchers.IO) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Not a persistable SAF URI or already granted
            }

            var displayName: String = uri.lastPathSegment ?: "Unnamed"
            var fileSize: Long = 0L

            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        displayName = it.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                        fileSize = it.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = FileTypeDetector.getExtension(displayName)
            val category = FileTypeDetector.detect(uri, context.contentResolver, displayName)

            val existing = dao.getFileByUri(uri.toString())
            if (existing != null) {
                dao.updateLastOpened(existing.id)
                return@withContext existing
            }

            val newRecord = FileRecord(
                id = UUID.randomUUID().toString(),
                uri = uri.toString(),
                name = displayName,
                mimeType = mimeType,
                extension = extension,
                size = fileSize,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                lastOpenedAt = System.currentTimeMillis(),
                category = category
            )

            dao.insertFile(newRecord)
            newRecord
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Purge any old demo dummy data
    suspend fun purgeDemoData() = withContext(Dispatchers.IO) {
        dao.deleteFileById("demo_video_travel")
        dao.deleteFileById("demo_photo_vacation")
        dao.deleteFileById("demo_pdf_project")
        dao.deleteFileById("demo_data_json")
        dao.deleteFileById("demo_text_notes")
        dao.deleteFileById("demo_csv_metrics")
        dao.deleteFileById("demo_css_styles")
        dao.deleteFileById("demo_md_readme")
        dao.deleteFileById("demo_html_preview")
        dao.deleteFileById("demo_zip_archive")
        dao.deleteFileById("demo_audio_song")
        dao.deleteFileById("demo_video_sunset")
    }

    // Realtime device scanner: scans real photos, videos, music, and documents on the user's phone
    suspend fun scanDeviceMedia(): Int = withContext(Dispatchers.IO) {
        var scannedCount = 0
        val filesToInsert = mutableListOf<FileRecord>()

        try {
            // 1. Scan Real Images
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC LIMIT 100"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Image_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val modified = cursor.getLong(dateCol) * 1000
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    filesToInsert.add(
                        FileRecord(
                            id = "media_img_$id",
                            uri = contentUri.toString(),
                            name = name,
                            mimeType = mime,
                            extension = FileTypeDetector.getExtension(name),
                            size = size,
                            modifiedAt = modified,
                            lastOpenedAt = modified,
                            category = FileCategory.IMAGE,
                            width = if (width > 0) width else null,
                            height = if (height > 0) height else null
                        )
                    )
                }
            }

            // 2. Scan Real Videos
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC LIMIT 50"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val modified = cursor.getLong(dateCol) * 1000
                    val duration = cursor.getLong(durCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    filesToInsert.add(
                        FileRecord(
                            id = "media_vid_$id",
                            uri = contentUri.toString(),
                            name = name,
                            mimeType = mime,
                            extension = FileTypeDetector.getExtension(name),
                            size = size,
                            modifiedAt = modified,
                            lastOpenedAt = modified,
                            category = FileCategory.VIDEO,
                            durationMs = if (duration > 0) duration else null,
                            width = if (width > 0) width else null,
                            height = if (height > 0) height else null
                        )
                    )
                }
            }

            // 3. Scan Real Audio
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DURATION
            )
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC LIMIT 50"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Audio_$id"
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val modified = cursor.getLong(dateCol) * 1000
                    val duration = cursor.getLong(durCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    filesToInsert.add(
                        FileRecord(
                            id = "media_aud_$id",
                            uri = contentUri.toString(),
                            name = name,
                            mimeType = mime,
                            extension = FileTypeDetector.getExtension(name),
                            size = size,
                            modifiedAt = modified,
                            lastOpenedAt = modified,
                            category = FileCategory.AUDIO,
                            durationMs = if (duration > 0) duration else null
                        )
                    )
                }
            }

            // 4. Scan Real Documents, PDFs, Text, Downloads
            val filesUri = MediaStore.Files.getContentUri("external")
            val filesProjection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            context.contentResolver.query(
                filesUri,
                filesProjection,
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("%pdf%", "%text%", "%json%", "%.%"),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val modified = cursor.getLong(dateCol) * 1000
                    val contentUri = ContentUris.withAppendedId(filesUri, id)
                    val category = FileTypeDetector.detect(contentUri, context.contentResolver, name)

                    if (category != FileCategory.UNKNOWN) {
                        filesToInsert.add(
                            FileRecord(
                                id = "media_file_$id",
                                uri = contentUri.toString(),
                                name = name,
                                mimeType = mime,
                                extension = FileTypeDetector.getExtension(name),
                                size = size,
                                modifiedAt = modified,
                                lastOpenedAt = modified,
                                category = category
                            )
                        )
                    }
                }
            }

            if (filesToInsert.isNotEmpty()) {
                dao.insertFiles(filesToInsert)
                scannedCount = filesToInsert.size
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scannedCount
    }
}
