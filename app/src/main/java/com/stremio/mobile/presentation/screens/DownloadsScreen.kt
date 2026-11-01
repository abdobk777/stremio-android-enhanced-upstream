package com.stremio.mobile.presentation.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.StremioBackgroundBrush
import kotlinx.coroutines.delay

private data class DownloadEntry(
    val id: Long,
    val title: String,
    val description: String,
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int,
)

@Composable
fun DownloadsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    var entries by remember { mutableStateOf(emptyList<DownloadEntry>()) }
    var loading by remember { mutableStateOf(true) }

    fun refresh() {
        val result = mutableListOf<DownloadEntry>()
        manager.query(DownloadManager.Query()).use { cursor ->
            val id = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val title = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val description = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION)
            val status = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val downloaded = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reason = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
            while (cursor.moveToNext()) {
                result += DownloadEntry(
                    cursor.getLong(id),
                    cursor.getString(title).orEmpty().ifBlank { "Video download" },
                    cursor.getString(description).orEmpty(),
                    cursor.getInt(status),
                    cursor.getLong(downloaded),
                    cursor.getLong(total),
                    cursor.getInt(reason),
                )
            }
        }
        entries = result
        loading = false
    }

    LaunchedEffect(manager) {
        while (true) {
            refresh()
            delay(1_000)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(StremioBackgroundBrush).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text("Downloads", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(24.dp))
        }

        if (loading) {
            CircularProgressIndicator()
        } else if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(44.dp))
                Text("No downloads yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text("Choose the download icon next to a supported direct video source.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(entries, key = { it.id }) { entry ->
                    DownloadRow(
                        entry = entry,
                        onOpen = {
                            val uri = manager.getUriForDownloadedFile(entry.id)
                            if (uri == null) {
                                Toast.makeText(context, "This download is not ready yet.", Toast.LENGTH_SHORT).show()
                            } else openDownloadedFile(context, uri)
                        },
                        onRemove = {
                            manager.remove(entry.id)
                            refresh()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(entry: DownloadEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    val complete = entry.status == DownloadManager.STATUS_SUCCESSFUL
    val failed = entry.status == DownloadManager.STATUS_FAILED
    val running = entry.status == DownloadManager.STATUS_RUNNING ||
        entry.status == DownloadManager.STATUS_PENDING ||
        entry.status == DownloadManager.STATUS_PAUSED
    val progress = if (entry.totalBytes > 0) {
        (entry.downloadedBytes.toFloat() / entry.totalBytes).coerceIn(0f, 1f)
    } else 0f
    val statusText = when {
        complete -> "Completed"
        failed -> "Failed · ${downloadReason(entry.reason)}"
        entry.status == DownloadManager.STATUS_PAUSED -> "Paused"
        entry.status == DownloadManager.STATUS_PENDING -> "Waiting"
        running -> "Downloading"
        else -> "Status unavailable"
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.Medium, maxLines = 2)
                if (entry.description.isNotBlank()) Text(entry.description, fontSize = 12.sp, maxLines = 1)
            }
            if (complete) IconButton(onClick = onOpen) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Play downloaded file")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove download")
            }
        }
        if (running) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            if (entry.totalBytes > 0) {
                "${statusText} · ${formatBytes(entry.downloadedBytes)} / ${formatBytes(entry.totalBytes)}"
            } else statusText,
            fontSize = 12.sp,
        )
    }
}

private fun openDownloadedFile(context: Context, uri: Uri) {
    runCatching {
        val mimeType = context.contentResolver.getType(uri) ?: "video/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "No compatible video player was found.", Toast.LENGTH_LONG).show()
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes} B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

private fun downloadReason(reason: Int): String = when (reason) {
    DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage unavailable"
    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
    DownloadManager.ERROR_FILE_ERROR -> "File error"
    DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network data error"
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage"
    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server error"
    DownloadManager.ERROR_UNKNOWN -> "Unknown error"
    else -> "Code ${reason}"
}
