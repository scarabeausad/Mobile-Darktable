package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoEntity::class, StyleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DarktableDatabase : RoomDatabase() {
    abstract fun darktableDao(): DarktableDao

    companion object {
        @Volatile
        private var INSTANCE: DarktableDatabase? = null

        fun getDatabase(context: Context): DarktableDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DarktableDatabase::class.java,
                    "darktable_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
