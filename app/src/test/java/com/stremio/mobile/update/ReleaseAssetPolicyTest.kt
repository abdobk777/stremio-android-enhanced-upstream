package com.stremio.mobile.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAssetPolicyTest {
    private val digest = "a".repeat(64)

    @Test
    fun `parses exact GNU sha256sum entry`() {
        assertEquals(
            digest,
            parseExpectedSha256("$digest  StremioMobile-v1.2.3-arm64-v8a-release.apk", "StremioMobile-v1.2.3-arm64-v8a-release.apk"),
        )
    }

    @Test
    fun `parses binary mode sha256sum entry`() {
        assertEquals(
            digest,
            parseExpectedSha256("$digest *StremioMobile-v1.2.3-universal-release.apk", "StremioMobile-v1.2.3-universal-release.apk"),
        )
    }

    @Test
    fun `does not accept checksum for a different asset`() {
        assertNull(
            parseExpectedSha256("$digest  another-file.apk", "StremioMobile-v1.2.3-arm64-v8a-release.apk"),
        )
    }

    @Test
    fun `rejects malformed or short digests`() {
        assertNull(parseExpectedSha256("not-a-hash  app.apk", "app.apk"))
        assertNull(parseExpectedSha256("${digest.dropLast(1)}  app.apk", "app.apk"))
        assertNull(parseExpectedSha256("${"g".repeat(64)}  app.apk", "app.apk"))
    }

    @Test
    fun `accepts only expected GitHub release asset URLs`() {
        assertTrue(
            isTrustedReleaseAssetUrl(
                "https://github.com/perpetus/stremio-android/releases/download/v1.2.3/StremioMobile-v1.2.3-universal-release.apk",
            ),
        )
        assertFalse(
            isTrustedReleaseAssetUrl(
                "http://github.com/perpetus/stremio-android/releases/download/v1.2.3/app.apk",
            ),
        )
        assertFalse(
            isTrustedReleaseAssetUrl(
                "https://example.com/perpetus/stremio-android/releases/download/v1.2.3/app.apk",
            ),
        )
        assertFalse(
            isTrustedReleaseAssetUrl(
                "https://github.com/attacker/other/releases/download/v1.2.3/app.apk",
            ),
        )
    }

    @Test
    fun `accepts only known release APK file names`() {
        assertTrue(isSafeApkAssetName("StremioMobile-v1.2.3-arm64-v8a-release.apk"))
        assertTrue(isSafeApkAssetName("StremioMobile-v1.2.3-universal-release.apk"))
        assertFalse(isSafeApkAssetName("../app.apk"))
        assertFalse(isSafeApkAssetName("StremioMobile-v1.2.3-debug.apk"))
    }
}
