package com.example.update

/**
 * Resolves release channel classification and selects eligible releases
 * according to the final DownTik specification (STABLE and BETA only).
 *
 * Rules:
 * STABLE:
 *   - draft == false
 *   - prerelease == false
 *   - tag matches: ^v\.?\d+\.\d+\.\d+$
 *
 * BETA:
 *   - draft == false
 *   - prerelease == true
 *   - tag matches: ^v\.?\d+\.\d+\.\d+-beta\.\d+$
 *
 * Any release that does not meet the specified format must be ignored.
 */
object ReleaseChannelResolver {

    val STABLE_TAG_REGEX = Regex("""^v\.?\d+\.\d+\.\d+$""", RegexOption.IGNORE_CASE)
    val BETA_TAG_REGEX = Regex("""^v\.?\d+\.\d+\.\d+-beta\.\d+$""", RegexOption.IGNORE_CASE)

    /**
     * Determines whether a GitHub release belongs to STABLE or BETA.
     * Returns null if release does not match the strict channel rules (ignored by update resolver).
     */
    fun detectReleaseChannel(release: GitHubRelease): ReleaseChannel? {
        if (release.draft) return null
        val tag = release.tagName.trim()

        if (!release.prerelease && STABLE_TAG_REGEX.matches(tag)) {
            return ReleaseChannel.STABLE
        }

        if (release.prerelease && BETA_TAG_REGEX.matches(tag)) {
            return ReleaseChannel.BETA
        }

        return null
    }

    /**
     * Finds the newest eligible release for the user's selected channel that is
     * strictly newer than the currently installed app version.
     *
     * Stable Resolver:
     * - draft == false
     * - prerelease == false
     * - tag matches stable regex
     * - parse version, sort descending, pick highest
     * - beta releases MUST NOT be selected
     *
     * Beta Resolver:
     * - draft == false
     * - prerelease == true
     * - tag matches beta regex
     * - parse version, sort descending, pick newest beta
     * - stable releases MUST NOT be selected as beta release
     *
     * Comparison with installed version:
     * - Only returns release if newest release is strictly newer than current installed version.
     */
    fun findEligibleRelease(
        releases: List<GitHubRelease>,
        userChannel: ReleaseChannel,
        currentVersion: String
    ): GitHubRelease? {
        val candidateReleases = when (userChannel) {
            ReleaseChannel.STABLE -> {
                releases.filter { release ->
                    !release.draft &&
                    !release.prerelease &&
                    STABLE_TAG_REGEX.matches(release.tagName.trim())
                }
            }

            ReleaseChannel.BETA -> {
                releases.filter { release ->
                    !release.draft &&
                    release.prerelease &&
                    BETA_TAG_REGEX.matches(release.tagName.trim())
                }
            }
        }

        // Sort descending by semantic version
        val sortedReleases = candidateReleases.sortedWith { r1, r2 ->
            VersionComparator.parse(r2.tagName).compareTo(VersionComparator.parse(r1.tagName))
        }

        // Take highest / newest on selected channel
        val newestRelease = sortedReleases.firstOrNull() ?: return null

        // Compare with current installed version
        if (!VersionComparator.isNewerVersion(newestRelease.tagName, currentVersion)) {
            return null
        }

        return newestRelease
    }
}
