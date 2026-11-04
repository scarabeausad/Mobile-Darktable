package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DarktableDao {
    @Query("SELECT * FROM photos ORDER BY captureDate DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos()

    // Styles
    @Query("SELECT * FROM styles ORDER BY isBuiltIn DESC, name ASC")
    fun getAllStyles(): Flow<List<StyleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStyle(style: StyleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStyles(styles: List<StyleEntity>)

    @Delete
    suspend fun deleteStyle(style: StyleEntity)
}
