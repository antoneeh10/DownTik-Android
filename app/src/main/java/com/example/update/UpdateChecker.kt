package com.example.update

import android.content.Context
import com.example.data.network.NetworkClient
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Checks for updates on GitHub Releases using public API endpoints.
 * Operates without authentication, respects rate-limits with in-memory caching.
 */
class UpdateChecker(private val context: Context) {

    // Cache of releases and fetch timestamp
    private var cachedReleases: List<GitHubRelease>? = null
    private var lastFetchTimeMs: Long = 0L

    private fun logDebug(message: String) {
        try {
            android.util.Log.d("UpdateChecker", message)
        } catch (_: Exception) {}
        println(message)
    }

    fun invalidateCache() {
        cachedReleases = null
        lastFetchTimeMs = 0L
    }

    suspend fun checkUpdate(
        channel: ReleaseChannel,
        installedVersion: String = getCurrentVersionName()
    ): UpdateState = withContext(Dispatchers.IO) {
        logDebug("[UpdateChecker] selectedChannel=${channel.name}")

        if (!UpdateConfig.ENABLED) {
            return@withContext UpdateState.UpToDate
        }

        val releases = try {
            fetchReleases()
        } catch (e: Exception) {
            return@withContext UpdateState.Error(
                message = "Tidak dapat memeriksa pembaruan: ${e.message ?: "Periksa koneksi internet"}"
            )
        }

        if (releases.isEmpty()) {
            logDebug("[UpdateChecker] updateAvailable=false")
            return@withContext UpdateState.UpToDate
        }

        val eligibleRelease = ReleaseChannelResolver.findEligibleRelease(
            releases = releases,
            userChannel = channel,
            currentVersion = installedVersion
        )

        if (eligibleRelease == null) {
            val candidate = releases.firstOrNull { !it.draft } ?: releases.firstOrNull()
            if (candidate != null) {
                val candidateChannel = ReleaseChannelResolver.detectReleaseChannel(candidate) ?: channel
                val candidateApk = candidate.findApkAsset()
                logDebug("[UpdateChecker] releaseTag=${candidate.tagName}")
                logDebug("[UpdateChecker] draft=${candidate.draft}")
                logDebug("[UpdateChecker] prerelease=${candidate.prerelease}")
                logDebug("[UpdateChecker] detectedChannel=${candidateChannel.name}")
                logDebug("[UpdateChecker] parsedVersion=${candidate.parsedVersion}")
                logDebug("[UpdateChecker] apk=${candidateApk?.name ?: "none"}")
            }
            logDebug("[UpdateChecker] updateAvailable=false")
            return@withContext UpdateState.UpToDate
        }

        val apkAsset = eligibleRelease.findApkAsset()
        val detectedChannel = ReleaseChannelResolver.detectReleaseChannel(eligibleRelease) ?: channel
        val parsedVersion = eligibleRelease.parsedVersion

        logDebug("[UpdateChecker] releaseTag=${eligibleRelease.tagName}")
        logDebug("[UpdateChecker] draft=${eligibleRelease.draft}")
        logDebug("[UpdateChecker] prerelease=${eligibleRelease.prerelease}")
        logDebug("[UpdateChecker] detectedChannel=${detectedChannel.name}")
        logDebug("[UpdateChecker] parsedVersion=$parsedVersion")
        logDebug("[UpdateChecker] apk=${apkAsset?.name ?: "none"}")

        if (apkAsset == null) {
            logDebug("[UpdateChecker] updateAvailable=false")
            return@withContext UpdateState.Error(
                message = "APK update asset not found for this release.",
                canRetry = false
            )
        }

        logDebug("[UpdateChecker] updateAvailable=true")

        val formattedDate = formatPublishedDate(eligibleRelease.publishedAt)
        val cleanNotes = eligibleRelease.body?.trim()?.takeIf { it.isNotBlank() }
            ?: "Pembaruan versi $parsedVersion (${detectedChannel.displayName}) siap diunduh."

        UpdateState.UpdateAvailable(
            release = eligibleRelease,
            apkAsset = apkAsset,
            channel = detectedChannel,
            versionName = parsedVersion,
            releaseNotes = cleanNotes,
            publishedDate = formattedDate,
            apkSizeFormatted = apkAsset.formattedSize
        )
    }

    private fun fetchReleases(): List<GitHubRelease> {
        val now = System.currentTimeMillis()
        if (cachedReleases != null && now - lastFetchTimeMs < UpdateConfig.CACHE_EXPIRATION_MS) {
            return cachedReleases!!
        }

        val request = Request.Builder()
            .url(UpdateConfig.RELEASES_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DownTik-Android-App/${getCurrentVersionName()}")
            .build()

        val response = NetworkClient.okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            if (code == 403 || code == 429) {
                cachedReleases?.let { return it }
                throw IllegalStateException("Batas laju permintaan GitHub tercapai. Silakan coba lagi nanti.")
            }
            throw IllegalStateException("Gagal mengambil rilis dari GitHub (HTTP $code)")
        }

        val json = response.body?.string() ?: throw IllegalStateException("Respons GitHub kosong")
        val listType = Types.newParameterizedType(List::class.java, GitHubRelease::class.java)
        val adapter = NetworkClient.moshi.adapter<List<GitHubRelease>>(listType)
        val releases = adapter.fromJson(json) ?: emptyList()

        cachedReleases = releases
        lastFetchTimeMs = now
        return releases
    }

    fun getCurrentVersionName(): String {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private fun formatPublishedDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "Terbaru"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val date = inputFormat.parse(rawDate) ?: return rawDate
            val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
            outputFormat.format(date)
        } catch (_: Exception) {
            rawDate.take(10)
        }
    }

    private fun cleanMarkdownReleaseNotes(body: String?): String {
        if (body.isNullOrBlank()) return "• Peningkatan performa pemutar video dan perbaikan stabilitas."
        return body.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                !line.startsWith("#") &&
                !line.startsWith("---") &&
                !line.startsWith("***") &&
                !line.contains("Full Changelog", ignoreCase = true)
            }
            .take(6)
            .joinToString("\n") { line ->
                var l = line
                if (l.startsWith("- ") || l.startsWith("* ")) {
                    l = "• " + l.drop(2)
                } else if (!l.startsWith("• ")) {
                    l = "• $l"
                }
                l.replace("**", "").replace("*", "").replace("`", "")
            }
    }
}
