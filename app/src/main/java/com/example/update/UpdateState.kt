package com.example.update

import android.net.Uri
import java.io.File

/**
 * UI State representing the lifecycle of the DownTik update process.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpdateAvailable(
        val release: GitHubRelease,
        val apkAsset: ReleaseAsset,
        val channel: ReleaseChannel,
        val versionName: String,
        val releaseNotes: String,
        val publishedDate: String,
        val apkSizeFormatted: String
    ) : UpdateState
    data class Downloading(
        val progress: Float, // 0.0f to 1.0f
        val downloadedBytes: Long,
        val totalBytes: Long,
        val percentageText: String
    ) : UpdateState
    data class ReadyToInstall(
        val apkUri: Uri,
        val apkFile: File,
        val release: GitHubRelease
    ) : UpdateState
    data object UpToDate : UpdateState
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateState
}
