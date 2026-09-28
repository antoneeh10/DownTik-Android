package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {

    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAllHistory(): Flow<List<DownloadHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadHistoryEntity): Long

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
