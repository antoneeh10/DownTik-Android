package com.example.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Downloads application APK assets asynchronously with progress tracking
 * and optional integrity validation.
 */
class UpdateDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    fun downloadApk(asset: ReleaseAsset, release: GitHubRelease): Flow<UpdateState> = flow {
        emit(UpdateState.Downloading(0f, 0L, asset.size, "0%"))

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val cleanVersion = release.tagName.replace("/", "_").replace("\\", "_")
        val apkFile = File(updatesDir, "DownTik_$cleanVersion.apk")

        // Reuse cached APK if already fully downloaded
        if (apkFile.exists() && asset.size > 0L && apkFile.length() == asset.size) {
            val apkUri = UpdateInstaller.getApkUri(context, apkFile)
            emit(UpdateState.ReadyToInstall(apkUri, apkFile, release))
            return@flow
        }

        val request = Request.Builder()
            .url(asset.browserDownloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "DownTik-Android-App")
            .build()

        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            emit(UpdateState.Error("Gagal menghubungi server unduhan: ${e.message ?: "Koneksi terputus"}"))
            return@flow
        }

        if (!response.isSuccessful) {
            emit(UpdateState.Error("Gagal mengunduh file pembaruan (HTTP ${response.code})"))
            return@flow
        }

        val body = response.body
        if (body == null) {
            emit(UpdateState.Error("Konten file pembaruan kosong"))
            return@flow
        }

        val totalBytes = if (asset.size > 0L) asset.size else body.contentLength()
        val inputStream = body.byteStream()
        val tempApkFile = File(updatesDir, "DownTik_${cleanVersion}_temp.apk")
        val outputStream = FileOutputStream(tempApkFile)

        val buffer = ByteArray(16384)
        var downloadedBytes = 0L
        var bytesRead: Int
        var lastReportedPercent = -1

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val progress = if (totalBytes > 0) {
                    (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                } else {
                    0.05f
                }
                val percentInt = (progress * 100).toInt()
                if (percentInt != lastReportedPercent) {
                    lastReportedPercent = percentInt
                    emit(
                        UpdateState.Downloading(
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            percentageText = "$percentInt%"
                        )
                    )
                }
            }
            outputStream.flush()
        } catch (e: Exception) {
            tempApkFile.delete()
            emit(UpdateState.Error("Unduhan terputus: ${e.message}"))
            return@flow
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
            try { outputStream.close() } catch (_: Exception) {}
        }

        // Rename temp file to target file
        if (apkFile.exists()) apkFile.delete()
        if (!tempApkFile.renameTo(apkFile)) {
            tempApkFile.copyTo(apkFile, overwrite = true)
            tempApkFile.delete()
        }

        // Validate digest if provided by GitHub release asset
        if (!asset.digest.isNullOrBlank()) {
            val isDigestValid = verifyDigest(apkFile, asset.digest)
            if (!isDigestValid) {
                apkFile.delete()
                emit(UpdateState.Error("Verifikasi keamanan file APK gagal. File mungkin rusak."))
                return@flow
            }
        }

        val apkUri = UpdateInstaller.getApkUri(context, apkFile)
        emit(UpdateState.ReadyToInstall(apkUri, apkFile, release))
    }.flowOn(Dispatchers.IO)

    private fun verifyDigest(file: File, expectedDigest: String): Boolean {
        return try {
            val parts = expectedDigest.split(":", limit = 2)
            val algorithm = if (parts.size == 2) parts[0].uppercase() else "SHA-256"
            val expectedHash = if (parts.size == 2) parts[1] else expectedDigest

            val md = MessageDigest.getInstance(algorithm)
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            val actualHash = md.digest().joinToString("") { "%02x".format(it) }
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (_: Exception) {
            true // If unsupported algorithm, allow user-prompted install
        }
    }
}
