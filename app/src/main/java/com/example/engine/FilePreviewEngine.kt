package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

sealed class JsonNodeType {
    object ObjectType : JsonNodeType()
    object ArrayType : JsonNodeType()
    object StringType : JsonNodeType()
    object NumberType : JsonNodeType()
    object BooleanType : JsonNodeType()
    object NullType : JsonNodeType()
}

data class JsonNode(
    val key: String,
    val value: String,
    val type: JsonNodeType,
    val path: String,
    val children: List<JsonNode> = emptyList()
)

data class CsvTableData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalRowCount: Int,
    val delimiter: Char
)

data class XmlNode(
    val name: String,
    val attributes: Map<String, String>,
    val textContent: String? = null,
    val children: List<XmlNode> = emptyList()
)

data class ArchiveItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long
)

object FilePreviewEngine {

    suspend fun readText(context: Context, uri: Uri, maxChars: Int = 500_000): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val sb = java.lang.StringBuilder()
                val buffer = CharArray(8192)
                var totalChars = 0
                var read: Int
                while (reader.read(buffer).also { read = it } != -1) {
                    val toAppend = minOf(read, maxChars - totalChars)
                    sb.append(buffer, 0, toAppend)
                    totalChars += toAppend
                    if (totalChars >= maxChars) {
                        sb.append("\n\n... [Truncated: File exceeds preview limit] ...")
                        break
                    }
                }
                Result.success(sb.toString())
            } ?: Result.failure(Exception("Cannot open stream for $uri"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseJsonTree(rawJson: String): Result<JsonNode> = withContext(Dispatchers.Default) {
        try {
            val trimmed = rawJson.trim()
            val tokener = JSONTokener(trimmed)
            val root = tokener.nextValue()
            val node = buildJsonNode("root", root, "$")
            Result.success(node)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildJsonNode(key: String, value: Any?, currentPath: String): JsonNode {
        return when (value) {
            is JSONObject -> {
                val children = mutableListOf<JsonNode>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val childVal = value.opt(k)
                    val nextPath = "$currentPath.$k"
                    children.add(buildJsonNode(k, childVal, nextPath))
                }
                JsonNode(
                    key = key,
                    value = "{ ${children.size} items }",
                    type = JsonNodeType.ObjectType,
                    path = currentPath,
                    children = children
                )
            }
            is JSONArray -> {
                val children = mutableListOf<JsonNode>()
                for (i in 0 until value.length()) {
                    val childVal = value.opt(i)
                    val nextPath = "$currentPath[$i]"
                    children.add(buildJsonNode("[$i]", childVal, nextPath))
                }
                JsonNode(
                    key = key,
                    value = "[ ${children.size} items ]",
                    type = JsonNodeType.ArrayType,
                    path = currentPath,
                    children = children
                )
            }
            is String -> JsonNode(key, "\"$value\"", JsonNodeType.StringType, currentPath)
            is Number -> JsonNode(key, value.toString(), JsonNodeType.NumberType, currentPath)
            is Boolean -> JsonNode(key, value.toString(), JsonNodeType.BooleanType, currentPath)
            null, JSONObject.NULL -> JsonNode(key, "null", JsonNodeType.NullType, currentPath)
            else -> JsonNode(key, value.toString(), JsonNodeType.StringType, currentPath)
        }
    }

    suspend fun parseCsv(rawContent: String, preferredDelimiter: Char? = null): CsvTableData = withContext(Dispatchers.Default) {
        val lines = rawContent.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            return@withContext CsvTableData(emptyList(), emptyList(), 0, ',')
        }

        val delimiter = preferredDelimiter ?: detectCsvDelimiter(lines.take(5))
        val parsedRows = lines.map { parseCsvLine(it, delimiter) }
        val headers = parsedRows.firstOrNull() ?: emptyList()
        val dataRows = if (parsedRows.size > 1) parsedRows.drop(1) else emptyList()

        CsvTableData(
            headers = headers,
            rows = dataRows,
            totalRowCount = dataRows.size,
            delimiter = delimiter
        )
    }

    private fun detectCsvDelimiter(sampleLines: List<String>): Char {
        val candidates = listOf(',', ';', '\t', '|')
        val counts = candidates.map { delim ->
            delim to sampleLines.sumOf { line -> line.count { it == delim } }
        }
        return counts.maxByOrNull { it.second }?.first ?: ','
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    suspend fun parseXml(rawXml: String): Result<XmlNode> = withContext(Dispatchers.Default) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(rawXml))

            var rootNode: XmlNode? = null
            val stack = ArrayDeque<MutableXmlNode>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val attrs = mutableMapOf<String, String>()
                        for (i in 0 until parser.attributeCount) {
                            attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                        }
                        val newNode = MutableXmlNode(parser.name, attrs)
                        if (stack.isNotEmpty()) {
                            stack.last().children.add(newNode)
                        }
                        stack.addLast(newNode)
                        if (rootNode == null && stack.size == 1) {
                            // keep track of top
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty() && stack.isNotEmpty()) {
                            stack.last().textContent = text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (stack.isNotEmpty()) {
                            val finished = stack.removeLast()
                            if (stack.isEmpty()) {
                                rootNode = finished.toImmutable()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (rootNode != null) {
                Result.success(rootNode)
            } else {
                Result.failure(Exception("No root XML element found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class MutableXmlNode(
        val name: String,
        val attributes: Map<String, String>,
        var textContent: String? = null,
        val children: MutableList<MutableXmlNode> = mutableListOf()
    ) {
        fun toImmutable(): XmlNode = XmlNode(
            name = name,
            attributes = attributes,
            textContent = textContent,
            children = children.map { it.toImmutable() }
        )
    }

    suspend fun listArchiveEntries(context: Context, uri: Uri): Result<List<ArchiveItem>> = withContext(Dispatchers.IO) {
        try {
            val items = mutableListOf<ArchiveItem>()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        items.add(
                            ArchiveItem(
                                name = entry.name,
                                isDirectory = entry.isDirectory,
                                size = entry.size.coerceAtLeast(0),
                                compressedSize = entry.compressedSize.coerceAtLeast(0)
                            )
                        )
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Result<Pair<Bitmap, Int>> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open PDF file descriptor"))
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val validIndex = pageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            val page = renderer.openPage(validIndex)

            val width = targetWidth
            val height = (targetWidth * page.height / page.width.toFloat()).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            Result.success(Pair(bitmap, totalPages))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val totalSecs = durationMs / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes mins ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> formatDate(timestamp)
        }
    }
}
