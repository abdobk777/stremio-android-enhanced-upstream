package com.stremio.mobile.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadPolicyTest {
    @Test
    fun `accepts secure direct video file URLs`() {
        assertEquals(
            "https://cdn.example/video.mp4?token=abc",
            DownloadPolicy.validateMediaUrlForTest("https://cdn.example/video.mp4?token=abc"),
        )
        assertEquals(
            "https://cdn.example/movie.mkv",
            DownloadPolicy.validateMediaUrlForTest("https://cdn.example/movie.mkv"),
        )
    }

    @Test
    fun `rejects insecure or manifest URLs`() {
        assertNull(DownloadPolicy.validateMediaUrlForTest("http://cdn.example/video.mp4"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://cdn.example/playlist.m3u8"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://cdn.example/manifest.mpd"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://cdn.example/movie.torrent"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://localhost/video.mp4"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://127.0.0.1/video.mp4"))
        assertNull(DownloadPolicy.validateMediaUrlForTest("https://[::1]/video.mp4"))
    }

    @Test
    fun `sanitizes suggested local file names`() {
        assertEquals(
            "A_Movie_2026-42.mp4",
            DownloadPolicy.fileName("A Movie/2026", "https://cdn.example/video.mp4", 42L),
        )
        assertNull(DownloadPolicy.fileName("A Movie", "https://cdn.example/playlist.m3u8", 42L))
    }
}
