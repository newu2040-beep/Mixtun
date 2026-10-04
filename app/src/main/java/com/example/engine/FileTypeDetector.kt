package com.example.engine

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.data.model.FileCategory
import java.io.InputStream
import java.util.Locale

object FileTypeDetector {

    fun detect(uri: Uri, contentResolver: ContentResolver?, fileName: String? = null): FileCategory {
        // 1. Try file name extension
        val name = fileName ?: uri.lastPathSegment ?: ""
        val extension = getExtension(name).lowercase(Locale.ROOT)
        val fromExt = detectFromExtension(extension)
        if (fromExt != FileCategory.UNKNOWN) {
            return fromExt
        }

        // 2. Try MIME type from ContentResolver
        val mimeType = try {
            contentResolver?.getType(uri)
        } catch (_: Exception) {
            null
        }

        if (!mimeType.isNullOrBlank()) {
            val fromMime = detectFromMimeType(mimeType)
            if (fromMime != FileCategory.UNKNOWN) {
                return fromMime
            }
        }

        // 3. Fallback to magic bytes check if stream is readable
        if (contentResolver != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val fromBytes = detectFromStream(stream)
                    if (fromBytes != FileCategory.UNKNOWN) {
                        return fromBytes
                    }
                }
            } catch (_: Exception) {
                // Ignore stream read issues
            }
        }

        return FileCategory.UNKNOWN
    }

    fun detectFromExtension(extension: String): FileCategory {
        return when (extension.lowercase(Locale.ROOT)) {
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "m4v", "flv", "wmv" -> FileCategory.VIDEO
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg" -> FileCategory.IMAGE
            "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "wma", "mid" -> FileCategory.AUDIO
            "pdf" -> FileCategory.PDF
            "json", "jsonl" -> FileCategory.JSON
            "csv", "tsv" -> FileCategory.CSV
            "xml", "plist" -> FileCategory.XML
            "md", "markdown", "mdown" -> FileCategory.MARKDOWN
            "html", "htm" -> FileCategory.HTML
            "zip", "7z", "tar", "gz", "bz2", "rar" -> FileCategory.ARCHIVE
            "kt", "java", "py", "js", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp", "rs", "sql", "yaml", "yml", "toml", "css", "scss", "sh", "bash", "go", "php", "rb", "swift" -> FileCategory.CODE
            "txt", "log", "ini", "conf", "properties", "env", "cfg", "asc" -> FileCategory.TEXT
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp" -> FileCategory.DOCUMENT
            else -> FileCategory.UNKNOWN
        }
    }

    fun detectFromMimeType(mimeType: String): FileCategory {
        val lower = mimeType.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("video/") -> FileCategory.VIDEO
            lower.startsWith("image/") -> FileCategory.IMAGE
            lower.startsWith("audio/") -> FileCategory.AUDIO
            lower == "application/pdf" -> FileCategory.PDF
            lower == "application/json" || lower.endsWith("+json") -> FileCategory.JSON
            lower == "text/csv" || lower == "text/tab-separated-values" -> FileCategory.CSV
            lower == "text/xml" || lower == "application/xml" || lower.endsWith("+xml") -> FileCategory.XML
            lower == "text/markdown" || lower == "text/x-markdown" -> FileCategory.MARKDOWN
            lower == "text/html" -> FileCategory.HTML
            lower == "application/zip" || lower == "application/x-tar" || lower == "application/x-7z-compressed" -> FileCategory.ARCHIVE
            lower.startsWith("text/") -> FileCategory.TEXT
            else -> FileCategory.UNKNOWN
        }
    }

    fun detectFromStream(stream: InputStream): FileCategory {
        val header = ByteArray(64)
        val read = stream.read(header)
        if (read < 4) return FileCategory.UNKNOWN

        // PDF: %PDF
        if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
            return FileCategory.PDF
        }
        // PNG: 89 50 4E 47
        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
            return FileCategory.IMAGE
        }
        // JPEG: FF D8 FF
        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return FileCategory.IMAGE
        }
        // GIF: GIF8
        if (header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte() && header[3] == '8'.code.toByte()) {
            return FileCategory.IMAGE
        }
        // ZIP: PK (50 4B 03 04)
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && (header[2] == 0x03.toByte() || header[2] == 0x05.toByte())) {
            return FileCategory.ARCHIVE
        }
        // Check for JSON or text start
        val preview = String(header, 0, minOf(read, 32)).trim()
        if (preview.startsWith("{") || preview.startsWith("[")) {
            return FileCategory.JSON
        }
        if (preview.startsWith("<?xml") || preview.startsWith("<html")) {
            return if (preview.startsWith("<html")) FileCategory.HTML else FileCategory.XML
        }

        return FileCategory.UNKNOWN
    }

    fun getExtension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot >= 0 && dot < name.length - 1) {
            name.substring(dot + 1)
        } else {
            ""
        }
    }
}
