package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.*

class MixtunConverters {
    @TypeConverter
    fun fromCategory(category: FileCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): FileCategory = try {
        FileCategory.valueOf(value)
    } catch (_: Exception) {
        FileCategory.UNKNOWN
    }
}

@Database(
    entities = [
        FileRecord::class,
        PlaybackState::class,
        ReadingState::class,
        MediaCollection::class,
        MediaCollectionItem::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MixtunConverters::class)
abstract class MixtunDatabase : RoomDatabase() {
    abstract fun mixtunDao(): MixtunDao

    companion object {
        @Volatile
        private var INSTANCE: MixtunDatabase? = null

        fun getInstance(context: Context): MixtunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MixtunDatabase::class.java,
                    "mixtun_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
