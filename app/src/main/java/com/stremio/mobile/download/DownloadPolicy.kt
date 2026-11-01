package com.stremio.mobile.download

import com.stremio.core.types.resource.Stream
import java.net.URI
import java.util.Locale

/**
 * V1 only accepts a direct HTTPS URL whose path has a known single-file video extension.
 * Torrent, external-player, HLS and DASH sources are intentionally excluded.
 */
internal object DownloadPolicy {
    private val supportedExtensions = setOf("mp4", "mkv", "webm", "mov", "avi", "m4v")

    fun directDownloadUrl(stream: Stream): String? {
        val source = stream.source as? Stream.Source.Url ?: return null
        return validateMediaUrlForTest(source.value.url)
    }

    internal fun validateMediaUrlForTest(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val path = uri.path ?: return null
        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)

        if (!uri.scheme.equals("https", ignoreCase = true) ||
            uri.userInfo != null ||
            (uri.port != -1 && uri.port != 443) ||
            host.equals("localhost", ignoreCase = true) ||
            host.endsWith(".localhost", ignoreCase = true) ||
            host.endsWith(".local", ignoreCase = true) ||
            host.contains(":") || // IPv6 literals, including loopback
            host.matches(Regex("""\d{1,3}(?:\.\d{1,3}){3}""")) ||
            extension !in supportedExtensions
        ) {
            return null
        }
        return value
    }

    fun fileName(title: String, url: String, uniqueSuffix: Long): String? {
        val extension = runCatching { URI(url).path.substringAfterLast('.', "").lowercase(Locale.ROOT) }
            .getOrNull()
            ?.takeIf { it in supportedExtensions }
            ?: return null

        val safeTitle = title
            .replace(Regex("""[^\p{L}\p{N}._-]+"""), "_")
            .trim('_', '.', '-')
            .take(80)
            .ifBlank { "video" }
        return "$safeTitle-$uniqueSuffix.$extension"
    }
}
