package com.example.data.repository

import com.example.data.model.VideoInfo
import com.example.data.network.NetworkClient
import com.example.data.network.TikwmApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class VideoRepository(
    private val apiService: TikwmApiService = NetworkClient.apiService
) {

    suspend fun fetchVideoInfo(rawUrl: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("URL tidak boleh kosong"))
        }

        // Basic URL validation
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("URL harus diawali dengan http:// atau https://"))
        }

        try {
            // Note: Pass trimmed directly or formatted. Retrofit will safely URL-encode the parameter.
            // Also append ?hd=1 or &hd=1 to ensure HD quality is requested
            val targetUrl = if (trimmed.contains("?")) {
                "$trimmed&hd=1"
            } else {
                "$trimmed?hd=1"
            }

            val response = apiService.getVideoInfo(url = targetUrl, hd = 1)

            if (response.code != 0 || response.data == null) {
                val errorMsg = response.msg ?: "Video tidak ditemukan atau link tidak valid"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val data = response.data
            val rawPlay = data.play.orEmpty()
            val rawHdPlay = data.hdplay
            val rawWmPlay = data.wmplay

            // If the play URL is relative (starts with /), prepend base host
            fun normalizeUrl(url: String?): String? {
                if (url.isNullOrBlank()) return null
                return if (url.startsWith("/")) {
                    "https://www.tikwm.com$url"
                } else {
                    url
                }
            }

            val standardPlay = normalizeUrl(rawPlay) ?: ""
            val hdPlay = normalizeUrl(rawHdPlay)
            val wmPlay = normalizeUrl(rawWmPlay)

            if (standardPlay.isBlank() && hdPlay.isNullOrBlank() && wmPlay.isNullOrBlank()) {
                return@withContext Result.failure(Exception("URL unduhan video tidak tersedia dari API"))
            }

            val videoInfo = VideoInfo(
                id = data.id ?: System.currentTimeMillis().toString(),
                title = data.title?.takeIf { it.isNotBlank() } ?: "DownTik Video",
                coverUrl = data.originCover ?: data.cover,
                durationSeconds = data.duration ?: 0L,
                standardPlayUrl = standardPlay,
                hdPlayUrl = hdPlay,
                wmPlayUrl = wmPlay,
                standardSizeBytes = data.size,
                hdSizeBytes = data.hdSize,
                authorNickname = data.author?.nickname,
                authorUsername = data.author?.uniqueId,
                authorAvatarUrl = data.author?.avatar,
                originalSourceUrl = trimmed
            )

            Result.success(videoInfo)
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet Anda."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Waktu koneksi habis. Silakan coba lagi."))
        } catch (e: IOException) {
            Result.failure(Exception("Terjadi masalah jaringan: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Terjadi kesalahan saat memproses video."))
        }
    }
}
