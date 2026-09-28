package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DownTikStorage {

    const val VIDEO_FOLDER = "Download/DownTik/Video"
    const val AUDIO_FOLDER = "Download/DownTik/Audio"

    fun sanitizeFileName(rawTitle: String, extension: String): String {
        val cleanExt = extension.removePrefix(".")
        val sanitized = rawTitle
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .trim()
            .take(60)

        val baseName = if (sanitized.isBlank() || sanitized.all { it == '_' || it == '-' || it == '.' }) {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            "DownTik_$dateStr"
        } else {
            sanitized
        }

        return if (baseName.endsWith(".$cleanExt", ignoreCase = true)) {
            baseName
        } else {
            "$baseName.$cleanExt"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun openMedia(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Media").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = if (mimeType.startsWith("audio")) "audio/*" else "video/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun openFolder(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Folder").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
