package com.example.update

/**
 * Proper Semantic Versioning parser and comparator.
 * - Guarantees "2.10.0" > "2.9.0" evaluates to true.
 * - Handles pre-release qualifiers (-beta.1, -rc.2, -nightly.20260928).
 * - Stable release is newer than its corresponding pre-release (e.g. 2.1.0 > 2.1.0-beta.1).
 * - Avoids simple string comparison pitfalls.
 */
object VersionComparator {

    data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preReleaseType: String? = null, // "alpha", "beta", "rc", "nightly", or null for stable
        val preReleaseNumber: Long = 0L,
        val raw: String
    ) : Comparable<SemVer> {

        override fun compareTo(other: SemVer): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)

            // Both stable -> equal
            if (preReleaseType == null && other.preReleaseType == null) return 0

            // Stable is strictly NEWER than pre-release of the same core version
            // e.g. 2.1.0 > 2.1.0-beta.1
            if (preReleaseType == null && other.preReleaseType != null) return 1
            if (preReleaseType != null && other.preReleaseType == null) return -1

            // Both are pre-releases: compare type weight
            val thisWeight = getPreReleaseWeight(preReleaseType)
            val otherWeight = getPreReleaseWeight(other.preReleaseType)
            if (thisWeight != otherWeight) {
                return thisWeight.compareTo(otherWeight)
            }

            // Same pre-release type: compare sequential number/date (e.g. beta.2 > beta.1)
            return preReleaseNumber.compareTo(other.preReleaseNumber)
        }

        private fun getPreReleaseWeight(type: String?): Int {
            return when (type?.lowercase()) {
                "alpha" -> 1
                "beta" -> 2
                "rc" -> 3
                "nightly" -> 4
                else -> 0
            }
        }
    }

    /**
     * Strips "v." or "v" prefix (case-insensitive) before semantic version parsing.
     * Handles formats such as:
     * - "v.1.1.0-beta.2" -> "1.1.0-beta.2"
     * - "v1.1.0" -> "1.1.0"
     * - "v.1.1.0" -> "1.1.0"
     * - "v1.1.0-beta.1" -> "1.1.0-beta.1"
     * - "v.1.1.0-beta.1" -> "1.1.0-beta.1"
     * - "v1.1.0-nightly.20260928" -> "1.1.0-nightly.20260928"
     * - "v.1.1.0-nightly.20260928" -> "1.1.0-nightly.20260928"
     */
    fun cleanVersionTag(versionStr: String): String {
        var clean = versionStr.trim()
        if (clean.startsWith("v.", ignoreCase = true)) {
            clean = clean.substring(2)
        } else if (clean.startsWith("v", ignoreCase = true)) {
            clean = clean.substring(1)
        }
        return clean.trim()
    }

    /**
     * Parses a version string into a SemVer object.
     * Examples:
     * - "v2.0.0" -> 2, 0, 0
     * - "v.1.1.0-beta.2" -> 1, 1, 0, "beta", 2
     * - "2.10.0" -> 2, 10, 0
     * - "v2.1.0-beta.1" -> 2, 1, 0, "beta", 1
     * - "v2.1.1-nightly.20260928" -> 2, 1, 1, "nightly", 20260928
     * - "nightly-20260928" -> 0, 0, 0, "nightly", 20260928
     */
    fun parse(versionStr: String): SemVer {
        val clean = cleanVersionTag(versionStr)

        if (clean.startsWith("nightly-", ignoreCase = true)) {
            val dateNum = clean.removePrefix("nightly-").filter { it.isDigit() }.toLongOrNull() ?: 0L
            return SemVer(0, 0, 0, "nightly", dateNum, versionStr)
        }

        val parts = clean.split("-", limit = 2)
        val corePart = parts[0]
        val prePart = parts.getOrNull(1)

        val numbers = corePart.split(".")
        val major = numbers.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = numbers.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = numbers.getOrNull(2)?.toIntOrNull() ?: 0

        var preType: String? = null
        var preNum: Long = 0L

        if (prePart != null) {
            val subParts = prePart.split(".", limit = 2)
            preType = subParts[0]
            preNum = subParts.getOrNull(1)?.toLongOrNull()
                ?: subParts[0].filter { it.isDigit() }.toLongOrNull()
                ?: 0L
        }

        return SemVer(major, minor, patch, preType, preNum, versionStr)
    }

    /**
     * Returns true if targetVersion is strictly newer than currentVersion.
     */
    fun isNewerVersion(targetVersion: String, currentVersion: String): Boolean {
        if (targetVersion.isBlank() || currentVersion.isBlank()) return false
        val target = parse(targetVersion)
        val current = parse(currentVersion)
        return target > current
    }
}
