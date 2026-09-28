package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun testSemanticVersionComparison_handlesLargerMinorProperly() {
        // "2.10.0" > "2.9.0" must be considered true
        assertTrue(VersionComparator.isNewerVersion("2.10.0", "2.9.0"))
        assertFalse(VersionComparator.isNewerVersion("2.9.0", "2.10.0"))
    }

    @Test
    fun testSemanticVersionComparison_orderSpecification() {
        // As specified: 1.2.5 < 1.3.0-beta.1 < 1.3.0
        assertTrue(VersionComparator.isNewerVersion("1.3.0-beta.1", "1.2.5"))
        assertTrue(VersionComparator.isNewerVersion("1.3.0", "1.3.0-beta.1"))
        assertTrue(VersionComparator.isNewerVersion("1.3.0", "1.2.5"))

        assertFalse(VersionComparator.isNewerVersion("1.2.5", "1.3.0-beta.1"))
        assertFalse(VersionComparator.isNewerVersion("1.3.0-beta.1", "1.3.0"))
    }

    @Test
    fun testSemanticVersionComparison_currentVersionScenarios() {
        // Scenario 1: installed 1.2.0, selected channel BETA, latest beta 1.2.0-beta.1
        // Must NOT offer update because 1.2.0-beta.1 is not newer than installed 1.2.0
        assertFalse(VersionComparator.isNewerVersion("1.2.0-beta.1", "1.2.0"))

        // Scenario 2: installed 1.1.0, selected channel BETA, latest beta 1.2.0-beta.1
        // MUST offer update (UPDATE AVAILABLE)
        assertTrue(VersionComparator.isNewerVersion("1.2.0-beta.1", "1.1.0"))
    }

    @Test
    fun testCleanVersionTag_handlesVandVDotPrefixes() {
        assertEquals("1.2.5", VersionComparator.cleanVersionTag("v1.2.5"))
        assertEquals("1.1.0-beta.2", VersionComparator.cleanVersionTag("v.1.1.0-beta.2"))
        assertEquals("1.1.0-beta.2", VersionComparator.cleanVersionTag("v1.1.0-beta.2"))
        assertEquals("1.0.0", VersionComparator.cleanVersionTag("v1.0.0"))
        assertEquals("1.0.0", VersionComparator.cleanVersionTag("v.1.0.0"))
    }

    @Test
    fun testChannelDetection_stableAndBeta() {
        val stableRelease = GitHubRelease(
            id = 1,
            tagName = "v1.2.5",
            name = "V1.2.5 — New Release",
            prerelease = false,
            draft = false
        )
        assertEquals(ReleaseChannel.STABLE, ReleaseChannelResolver.detectReleaseChannel(stableRelease))

        val betaRelease = GitHubRelease(
            id = 2,
            tagName = "v.1.1.0-beta.2",
            name = "New beta",
            prerelease = true,
            draft = false
        )
        assertEquals(ReleaseChannel.BETA, ReleaseChannelResolver.detectReleaseChannel(betaRelease))

        // Non-matching tag / invalid releases must be ignored (return null)
        val draftRelease = GitHubRelease(
            id = 3,
            tagName = "v1.2.5",
            prerelease = false,
            draft = true
        )
        assertNull(ReleaseChannelResolver.detectReleaseChannel(draftRelease))

        val invalidTagRelease = GitHubRelease(
            id = 4,
            tagName = "custom-tag-1.0",
            prerelease = false,
            draft = false
        )
        assertNull(ReleaseChannelResolver.detectReleaseChannel(invalidTagRelease))
    }

    @Test
    fun testStableResolver_selectsHighestStable_ignoresBeta() {
        // Releases: v1.0.0, v1.1.0, v1.2.5, v1.3.0-beta.1
        // Expected stable result: v1.2.5
        val releases = listOf(
            GitHubRelease(id = 1, tagName = "v1.0.0", prerelease = false, draft = false),
            GitHubRelease(id = 2, tagName = "v1.1.0", prerelease = false, draft = false),
            GitHubRelease(id = 3, tagName = "v1.2.5", prerelease = false, draft = false),
            GitHubRelease(id = 4, tagName = "v1.3.0-beta.1", prerelease = true, draft = false)
        )

        val eligible = ReleaseChannelResolver.findEligibleRelease(
            releases = releases,
            userChannel = ReleaseChannel.STABLE,
            currentVersion = "1.0.0"
        )

        assertNotNull(eligible)
        assertEquals("v1.2.5", eligible?.tagName)
    }

    @Test
    fun testBetaResolver_selectsNewestBeta_ignoresStable() {
        // Releases: v1.1.0-beta.1, v1.1.0-beta.2, v1.2.0-beta.1, v1.2.0
        // Expected beta result: v1.2.0-beta.1 (stable v1.2.0 must NOT be selected as beta release)
        val releases = listOf(
            GitHubRelease(id = 1, tagName = "v1.1.0-beta.1", prerelease = true, draft = false),
            GitHubRelease(id = 2, tagName = "v1.1.0-beta.2", prerelease = true, draft = false),
            GitHubRelease(id = 3, tagName = "v1.2.0-beta.1", prerelease = true, draft = false),
            GitHubRelease(id = 4, tagName = "v1.2.0", prerelease = false, draft = false)
        )

        val eligible = ReleaseChannelResolver.findEligibleRelease(
            releases = releases,
            userChannel = ReleaseChannel.BETA,
            currentVersion = "1.1.0"
        )

        assertNotNull(eligible)
        assertEquals("v1.2.0-beta.1", eligible?.tagName)
    }

    @Test
    fun testApkAssetSelection_rejectsUnrelatedApk_acceptsDowntikApk() {
        val foreignApk = ReleaseAsset(
            id = 101,
            name = "com.miui.weather2_G-17.0.3.6-HD-170003006_minAPI24.arm64-v8a.nodpi._apkmirror.com.apk",
            contentType = "application/vnd.android.package-archive",
            state = "uploaded",
            browserDownloadUrl = "https://github.com/example/miui.apk"
        )
        val downtikApk = ReleaseAsset(
            id = 102,
            name = "downtik-1.2.5.apk",
            contentType = "application/vnd.android.package-archive",
            state = "uploaded",
            browserDownloadUrl = "https://github.com/antoneeh10/DownTik-Android/releases/download/v1.2.5/downtik-1.2.5.apk"
        )

        val releaseWithBoth = GitHubRelease(
            id = 1,
            tagName = "v1.2.5",
            assets = listOf(foreignApk, downtikApk)
        )

        // Must reject foreign APK even if it is assets[0]
        val resolvedApk = releaseWithBoth.findApkAsset()
        assertNotNull(resolvedApk)
        assertEquals("downtik-1.2.5.apk", resolvedApk?.name)
        assertEquals(
            "https://github.com/antoneeh10/DownTik-Android/releases/download/v1.2.5/downtik-1.2.5.apk",
            resolvedApk?.browserDownloadUrl
        )

        // If release only has the foreign APK, findApkAsset must return null
        val releaseWithForeignOnly = GitHubRelease(
            id = 2,
            tagName = "v1.2.5",
            assets = listOf(foreignApk)
        )
        assertNull(releaseWithForeignOnly.findApkAsset())
    }

    @Test
    fun testBetaAssetSelection_withVDotTagAndBetaApk() {
        val betaApk = ReleaseAsset(
            id = 201,
            name = "downtik-1.1.0-beta.2.apk",
            contentType = "application/vnd.android.package-archive",
            state = "uploaded",
            browserDownloadUrl = "https://github.com/antoneeh10/DownTik-Android/releases/download/v.1.1.0-beta.2/downtik-1.1.0-beta.2.apk"
        )
        val betaRelease = GitHubRelease(
            id = 88,
            tagName = "v.1.1.0-beta.2",
            name = "New beta",
            prerelease = true,
            draft = false,
            body = "hello guys this release stable channel test",
            assets = listOf(betaApk)
        )

        assertEquals(ReleaseChannel.BETA, ReleaseChannelResolver.detectReleaseChannel(betaRelease))
        assertEquals("1.1.0-beta.2", betaRelease.parsedVersion)

        val apk = betaRelease.findApkAsset()
        assertNotNull(apk)
        assertEquals("downtik-1.1.0-beta.2.apk", apk?.name)
    }
}
