package com.example.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "id") val id: Long,
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "prerelease") val prerelease: Boolean = false,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "assets") val assets: List<ReleaseAsset> = emptyList()
) {
    companion object {
        val DOWNTIK_APK_REGEX = Regex("""^downtik-.*\.apk$""", RegexOption.IGNORE_CASE)
        fun isDownTikApk(name: String): Boolean = DOWNTIK_APK_REGEX.matches(name.trim())
    }

    /**
     * Validates and finds the official DownTik APK asset.
     * Criteria:
     * - Name matches pattern: downtik-*.apk (rejects foreign/unrelated APKs)
     * - content_type == "application/vnd.android.package-archive"
     * - state == "uploaded"
     * Never uses tarball_url or zipball_url, and never uses assets[0] without validation.
     */
    fun findApkAsset(): ReleaseAsset? {
        return assets.firstOrNull { asset ->
            val nameMatches = DOWNTIK_APK_REGEX.matches(asset.name.trim())
            val contentTypeMatches = asset.contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
            val stateMatches = asset.state == null || asset.state.equals("uploaded", ignoreCase = true)
            nameMatches && contentTypeMatches && stateMatches
        }
    }

    /**
     * Parsed semantic version string (e.g. "1.1.0-beta.2" from "v.1.1.0-beta.2").
     */
    val parsedVersion: String
        get() = VersionComparator.cleanVersionTag(tagName)

    /**
     * Clean readable version name for UI display.
     */
    val displayVersion: String
        get() = parsedVersion
}

@JsonClass(generateAdapter = true)
data class ReleaseAsset(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "content_type") val contentType: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "digest") val digest: String? = null
) {
    val isApk: Boolean
        get() = GitHubRelease.isDownTikApk(name) &&
            contentType.equals("application/vnd.android.package-archive", ignoreCase = true) &&
            (state == null || state.equals("uploaded", ignoreCase = true))

    val formattedSize: String
        get() {
            if (size <= 0L) return ""
            val mb = size / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }
}
