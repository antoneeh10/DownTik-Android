package com.example

import com.example.data.model.VideoInfo
import com.example.downloader.DownTikStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSanitizeFileName_removesIllegalChars() {
        val input = "Video: Awesome/Test? *Cool* <Cat> | \"Meme\""
        val sanitized = DownTikStorage.sanitizeFileName(input, "mp4")
        assertTrue(!sanitized.contains(":"))
        assertTrue(!sanitized.contains("/"))
        assertTrue(!sanitized.contains("?"))
        assertTrue(!sanitized.contains("*"))
        assertTrue(!sanitized.contains("<"))
        assertTrue(!sanitized.contains(">"))
        assertTrue(!sanitized.contains("|"))
        assertTrue(!sanitized.contains("\""))
        assertTrue(sanitized.endsWith(".mp4"))
    }

    @Test
    fun testSanitizeFileName_audioExtension() {
        val input = "Lagu Keren"
        val sanitized = DownTikStorage.sanitizeFileName(input, "m4a")
        assertEquals("Lagu Keren.m4a", sanitized)
    }

    @Test
    fun testSanitizeFileName_emptyFallback() {
        val emptyInput = "   "
        val sanitizedVideo = DownTikStorage.sanitizeFileName(emptyInput, "mp4")
        assertTrue(sanitizedVideo.startsWith("DownTik_"))
        assertTrue(sanitizedVideo.endsWith(".mp4"))

        val sanitizedAudio = DownTikStorage.sanitizeFileName(emptyInput, "m4a")
        assertTrue(sanitizedAudio.startsWith("DownTik_"))
        assertTrue(sanitizedAudio.endsWith(".m4a"))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", DownTikStorage.formatFileSize(0))
        assertEquals("1023 B", DownTikStorage.formatFileSize(1023))
        assertEquals("1.0 KB", DownTikStorage.formatFileSize(1024))
        assertEquals("1.5 MB", DownTikStorage.formatFileSize(1572864))
    }

    @Test
    fun testVideoInfo_bestDownloadUrl() {
        val infoWithHd = VideoInfo(
            id = "123",
            title = "Test Video",
            coverUrl = "http://example.com/cover.jpg",
            durationSeconds = 15,
            standardPlayUrl = "http://example.com/standard.mp4",
            hdPlayUrl = "http://example.com/hd.mp4",
            wmPlayUrl = "http://example.com/wm.mp4",
            standardSizeBytes = 1000,
            hdSizeBytes = 2000,
            authorNickname = "CatFan",
            authorUsername = "catfan",
            authorAvatarUrl = null,
            originalSourceUrl = "https://vt.tiktok.com/ZS429oDnw"
        )

        assertEquals("http://example.com/hd.mp4", infoWithHd.getBestDownloadUrl(preferHd = true))
        assertEquals("http://example.com/standard.mp4", infoWithHd.getBestDownloadUrl(preferHd = false))
    }
}
