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

        @Volatile
        private var INSTANCE_APP_CONTEXT: Context? = null

        fun getDatabase(context: Context): DarktableDatabase {
            val appCtx = context.applicationContext
            val existing = INSTANCE
            if (existing != null && INSTANCE_APP_CONTEXT === appCtx) {
                return existing
            }
            return synchronized(this) {
                val current = INSTANCE
                if (current != null && INSTANCE_APP_CONTEXT === appCtx) {
                    current
                } else {
                    val instance = Room.databaseBuilder(
                        appCtx,
                        DarktableDatabase::class.java,
                        "darktable_database"
                    ).fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                    INSTANCE = instance
                    INSTANCE_APP_CONTEXT = appCtx
                    instance
                }
            }
        }
    }
}
