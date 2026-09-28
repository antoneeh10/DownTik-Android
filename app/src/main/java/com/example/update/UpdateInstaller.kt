package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles invoking the standard Android Package Installer for downloaded APKs.
 * Does not perform silent installation; requires explicit user confirmation via system UI.
 */
object UpdateInstaller {

    fun getApkUri(context: Context, apkFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
    }

    fun installApk(context: Context, apkFile: File) {
        val apkUri = getApkUri(context, apkFile)
        installApk(context, apkUri)
    }

    fun installApk(context: Context, apkUri: Uri) {
        // On Android 8.0+ (Oreo, API 26+), verify if install from unknown sources is permitted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageManager = context.packageManager
            if (!packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
