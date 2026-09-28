package com.example.data.repository

import com.example.data.local.DownloadHistoryDao
import com.example.data.local.DownloadHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val dao: DownloadHistoryDao
) {
    val historyFlow: Flow<List<DownloadHistoryEntity>> = dao.getAllHistory()

    suspend fun addHistory(
        videoId: String,
        title: String,
        coverUrl: String?,
        author: String?,
        downloadUri: String,
        fileName: String,
        filePath: String?,
        fileSizeBytes: Long,
        mediaType: String = "VIDEO", // "VIDEO" or "AUDIO"
        storagePath: String = if (mediaType == "AUDIO") "Download/DownTik/Audio" else "Download/DownTik/Video"
    ): Long = withContext(Dispatchers.IO) {
        dao.insert(
            DownloadHistoryEntity(
                videoId = videoId,
                title = title,
                coverUrl = coverUrl,
                author = author,
                downloadUri = downloadUri,
                fileName = fileName,
                filePath = filePath,
                fileSizeBytes = fileSizeBytes,
                mediaType = mediaType,
                storagePath = storagePath
            )
        )
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
