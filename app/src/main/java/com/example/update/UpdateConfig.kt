package com.example.update

/**
 * Single source of truth configuration for DownTik application updates.
 * Connected to official DownTik Android GitHub repository:
 * https://github.com/antoneeh10/DownTik-Android
 */
object UpdateConfig {
    const val GITHUB_OWNER = "antoneeh10"
    const val GITHUB_REPOSITORY = "DownTik-Android"
    const val REPOSITORY_URL = "https://github.com/antoneeh10/DownTik-Android"
    const val RELEASES_API_URL = "https://api.github.com/repos/antoneeh10/DownTik-Android/releases"
    const val ENABLED = true
    val DEFAULT_CHANNEL = ReleaseChannel.STABLE

    // Cache duration (5 minutes) to prevent redundant GitHub API requests & rate-limiting
    const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
}
