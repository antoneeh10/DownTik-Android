package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val coverUrl: String?,
    val author: String?,
    val downloadUri: String,
    val fileName: String,
    val filePath: String?,
    val fileSizeBytes: Long,
    val mediaType: String = "VIDEO", // "VIDEO" or "AUDIO"
    val storagePath: String = "Download/DownTik/Video",
    val downloadedAt: Long = System.currentTimeMillis()
)
