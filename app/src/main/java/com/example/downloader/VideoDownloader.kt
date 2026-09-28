package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float, // 0.0 to 1.0
        val percentageText: String, // e.g., "45%"
        val sizeText: String // e.g., "4.5 MB / 10.0 MB"
    ) : DownloadState
    data class Completed(
        val uri: Uri,
        val fileName: String,
        val totalBytes: Long
    ) : DownloadState
    data class Failed(val message: String) : DownloadState
}

class VideoDownloader(private val context: Context) {

    private val okHttpClient = NetworkClient.okHttpClient

    fun downloadVideo(
        videoUrl: String,
        title: String
    ): Flow<DownloadState> = flow {
        emit(
            DownloadState.Downloading(
                bytesDownloaded = 0L,
                totalBytes = -1L,
                progress = 0f,
                percentageText = "0%",
                sizeText = "Memulai download..."
            )
        )

        val safeFileName = DownTikStorage.sanitizeFileName(title, "mp4")
        var outputStream: OutputStream? = null
        var targetUri: Uri? = null
        var legacyFile: File? = null

        try {
            val request = Request.Builder()
                .url(videoUrl)
                .addHeader("Referer", "https://www.tiktok.com/")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Failed("Gagal menghubungi server (HTTP ${response.code})"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Failed("Data video kosong"))
                return@flow
            }

            val totalBytes = body.contentLength()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, DownTikStorage.VIDEO_FOLDER)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                targetUri = resolver.insert(collectionUri, contentValues)
                    ?: throw IllegalStateException("Tidak dapat membuat entri penyimpanan video")

                outputStream = resolver.openOutputStream(targetUri)
                    ?: throw IllegalStateException("Tidak dapat membuka stream penyimpanan")
            } else {
                @Suppress("DEPRECATION")
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DownTik/Video"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val destFile = File(downloadDir, safeFileName)
                legacyFile = destFile
                outputStream = FileOutputStream(destFile)
                targetUri = Uri.fromFile(destFile)
            }

            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMs = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateMs > 100 || totalRead == totalBytes) {
                    lastUpdateMs = now
                    val progress = if (totalBytes > 0) {
                        (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val percent = if (totalBytes > 0) "${(progress * 100).toInt()}%" else DownTikStorage.formatFileSize(totalRead)
                    val sizeText = if (totalBytes > 0) {
                        "${DownTikStorage.formatFileSize(totalRead)} / ${DownTikStorage.formatFileSize(totalBytes)}"
                    } else {
                        DownTikStorage.formatFileSize(totalRead)
                    }

                    emit(
                        DownloadState.Downloading(
                            bytesDownloaded = totalRead,
                            totalBytes = totalBytes,
                            progress = progress,
                            percentageText = percent,
                            sizeText = sizeText
                        )
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Finalize MediaStore on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetUri != null) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(targetUri, contentValues, null, null)
            } else if (legacyFile != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(legacyFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }

            val finalUri = targetUri ?: Uri.EMPTY
            emit(
                DownloadState.Completed(
                    uri = finalUri,
                    fileName = safeFileName,
                    totalBytes = totalRead
                )
            )

        } catch (e: Exception) {
            outputStream?.runCatching { close() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetUri != null) {
                context.contentResolver.runCatching { delete(targetUri, null, null) }
            } else if (legacyFile != null && legacyFile.exists()) {
                legacyFile.delete()
            }
            emit(DownloadState.Failed("Gagal mengunduh: ${e.localizedMessage ?: "Terjadi kesalahan"}"))
        }
    }.flowOn(Dispatchers.IO)
}
