package com.stremio.mobile.update

import java.net.URI
import java.util.Locale

private val APK_ASSET_NAME = Regex(
    """^StremioMobile-v\d+\.\d+\.\d+-(?:armeabi-v7a|arm64-v8a|x86|x86_64|universal)-release\.apk$""",
    RegexOption.IGNORE_CASE,
)

private val RELEASE_ASSET_PATH = Regex(
    """^/perpetus/stremio-android/releases/download/[^/]+/[^/]+$""",
    RegexOption.IGNORE_CASE,
)

private val SHA256_LINE = Regex("""^([0-9a-fA-F]{64})[ \t]+\*?(.+?)$""")

internal fun isSafeApkAssetName(name: String): Boolean =
    APK_ASSET_NAME.matches(name)

internal fun isTrustedReleaseAssetUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    val host = uri.host ?: return false
    val path = uri.path ?: return false

    return uri.scheme.equals("https", ignoreCase = true) &&
        host.equals("github.com", ignoreCase = true) &&
        uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443) &&
        RELEASE_ASSET_PATH.matches(path)
}

/**
 * GitHub release asset URLs redirect to GitHub-owned asset delivery hosts.
 * Each redirect target is checked before the next request is made.
 */
internal fun isTrustedReleaseDeliveryUrl(value: String): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    val host = uri.host ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null ||
        (uri.port != -1 && uri.port != 443)
    ) {
        return false
    }

    return isTrustedReleaseAssetUrl(value) ||
        host.equals("release-assets.githubusercontent.com", ignoreCase = true) ||
        host.equals("objects.githubusercontent.com", ignoreCase = true)
}

/**
 * Reads a standard sha256sum entry and returns the lowercase digest for the exact asset name.
 * Lines for other files and malformed digests are ignored.
 */
internal fun parseExpectedSha256(contents: String, assetName: String): String? {
    return contents.lineSequence()
        .mapNotNull { line ->
            val match = SHA256_LINE.matchEntire(line.trim()) ?: return@mapNotNull null
            val fileName = match.groupValues[2]
            if (fileName == assetName) match.groupValues[1].lowercase(Locale.US) else null
        }
        .firstOrNull()
}
