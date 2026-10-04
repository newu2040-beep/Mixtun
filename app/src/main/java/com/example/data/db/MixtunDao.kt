package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MixtunDao {

    @Query("SELECT * FROM files ORDER BY lastOpenedAt DESC")
    fun getAllFiles(): Flow<List<FileRecord>>

    @Query("SELECT * FROM files ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 30): Flow<List<FileRecord>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 ORDER BY lastOpenedAt DESC")
    fun getFavoriteFiles(): Flow<List<FileRecord>>

    @Query("SELECT * FROM files WHERE category = :category ORDER BY lastOpenedAt DESC")
    fun getFilesByCategory(category: String): Flow<List<FileRecord>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    fun getFileById(id: String): Flow<FileRecord?>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileByIdDirect(id: String): FileRecord?

    @Query("SELECT * FROM files WHERE uri = :uri LIMIT 1")
    suspend fun getFileByUri(uri: String): FileRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileRecord>)

    @Update
    suspend fun updateFile(file: FileRecord)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("UPDATE files SET isFavorite = :isFavorite WHERE id = :fileId")
    suspend fun setFavorite(fileId: String, isFavorite: Boolean)

    @Query("UPDATE files SET lastOpenedAt = :timestamp WHERE id = :fileId")
    suspend fun updateLastOpened(fileId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM playback_states WHERE fileId = :fileId LIMIT 1")
    fun getPlaybackState(fileId: String): Flow<PlaybackState?>

    @Query("SELECT * FROM playback_states WHERE fileId = :fileId LIMIT 1")
    suspend fun getPlaybackStateDirect(fileId: String): PlaybackState?

    @Query("SELECT * FROM playback_states ORDER BY updatedAt DESC")
    fun getAllPlaybackStates(): Flow<List<PlaybackState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(state: PlaybackState)

    @Query("DELETE FROM playback_states WHERE fileId = :fileId")
    suspend fun deletePlaybackState(fileId: String)

    @Query("SELECT * FROM reading_states WHERE fileId = :fileId LIMIT 1")
    fun getReadingState(fileId: String): Flow<ReadingState?>

    @Query("SELECT * FROM reading_states WHERE fileId = :fileId LIMIT 1")
    suspend fun getReadingStateDirect(fileId: String): ReadingState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingState(state: ReadingState)

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<MediaCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: MediaCollection)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("SELECT f.* FROM files f INNER JOIN collection_items ci ON f.id = ci.fileId WHERE ci.collectionId = :collectionId ORDER BY ci.addedAt DESC")
    fun getFilesInCollection(collectionId: String): Flow<List<FileRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFileToCollection(item: MediaCollectionItem)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND fileId = :fileId")
    suspend fun removeFileFromCollection(collectionId: String, fileId: String)

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' OR extension LIKE '%' || :query || '%' ORDER BY lastOpenedAt DESC")
    fun searchFiles(query: String): Flow<List<FileRecord>>

    @Query("DELETE FROM files WHERE id NOT IN (SELECT id FROM files WHERE isFavorite = 1)")
    suspend fun clearHistory()
}
