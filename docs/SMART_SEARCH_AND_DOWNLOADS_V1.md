# Smart Search and Offline Downloads — V1

## Smart Search

The regular search request remains owned by Stremio Core. A separate suggestion-only matcher compares the entered text with titles already present in locally loaded data:

- The user's library
- Continue Watching
- Loaded home-board shelves
- The currently loaded Discover shelf

The matcher normalizes case, punctuation and diacritics and ranks candidates using character edit distance and token overlap. It only displays a clickable suggestion in the no-results state. Selecting the suggestion submits that title through the existing Stremio search path.

### Explicit limitations

- The local suggestion list is not a full index of all installed add-ons or remote catalogs.
- Suggestions depend on content already loaded in the app.
- A suggestion is not itself a search result and is never substituted silently.

## Offline Downloads

V1 exposes a download action only when a stream is a direct `Stream.Source.Url` with an HTTPS URL and a recognized single-file video extension:

- MP4
- MKV
- WebM
- MOV
- AVI
- M4V

The download is queued with Android DownloadManager and saved under the app-specific external Downloads directory. Android supplies the background transfer and system notification.

### Deliberately excluded

- Torrent / Tramvai streams
- External-player sources
- HLS playlists (`.m3u8`)
- DASH manifests (`.mpd`)
- HTTP URLs and obvious local/IP-literal destinations

### Not included in V1

- In-app download history or queue screen
- In-app pause, resume, cancel, and delete controls
- Playing downloaded files from an in-app offline library
- HLS/DASH segment packaging or torrent persistence
- Explicit file-size limits, checksum validation, or server MIME verification for media downloads

## Updater integrity

The app update flow now requires a named SHA-256 checksum entry for the selected APK and rejects missing or malformed checksum data. The APK and checksum URLs must be HTTPS release-asset URLs under the configured upstream repository path.

The checksum is downloaded from the same GitHub release as the APK. It is useful for detecting corruption or mismatched release assets, but it is not an independent signature of the publisher. APK signing-certificate continuity / an independently signed update manifest remains future work.

## Verification status

The helper logic has been locally checked with focused assertions. A full Android Gradle build, instrumentation test, and device download test have not yet been completed. Do not treat V1 as production-verified until the PR checks and device tests pass.
