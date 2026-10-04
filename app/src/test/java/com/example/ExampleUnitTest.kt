package com.example

import com.example.data.model.FileCategory
import com.example.engine.FilePreviewEngine
import com.example.engine.FileTypeDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFileTypeDetection() {
        assertEquals(FileCategory.VIDEO, FileTypeDetector.detectFromExtension("mp4"))
        assertEquals(FileCategory.VIDEO, FileTypeDetector.detectFromExtension("mkv"))
        assertEquals(FileCategory.IMAGE, FileTypeDetector.detectFromExtension("jpg"))
        assertEquals(FileCategory.IMAGE, FileTypeDetector.detectFromExtension("png"))
        assertEquals(FileCategory.PDF, FileTypeDetector.detectFromExtension("pdf"))
        assertEquals(FileCategory.JSON, FileTypeDetector.detectFromExtension("json"))
        assertEquals(FileCategory.CSV, FileTypeDetector.detectFromExtension("csv"))
        assertEquals(FileCategory.AUDIO, FileTypeDetector.detectFromExtension("mp3"))
        assertEquals(FileCategory.MARKDOWN, FileTypeDetector.detectFromExtension("md"))
        assertEquals(FileCategory.HTML, FileTypeDetector.detectFromExtension("html"))
        assertEquals(FileCategory.ARCHIVE, FileTypeDetector.detectFromExtension("zip"))
        assertEquals(FileCategory.CODE, FileTypeDetector.detectFromExtension("kt"))
        assertEquals(FileCategory.TEXT, FileTypeDetector.detectFromExtension("txt"))
    }

    @Test
    fun testFileSizeFormatting() {
        assertEquals("0 B", FilePreviewEngine.formatFileSize(0L))
        assertEquals("1.0 KB", FilePreviewEngine.formatFileSize(1024L))
        assertEquals("1.0 MB", FilePreviewEngine.formatFileSize(1024L * 1024L))
        assertEquals("1.2 GB", FilePreviewEngine.formatFileSize(1_288_490_188L))
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("00:00", FilePreviewEngine.formatDuration(0L))
        assertEquals("01:30", FilePreviewEngine.formatDuration(90_000L))
        assertEquals("12:43", FilePreviewEngine.formatDuration(763_000L))
        assertEquals("1:05:00", FilePreviewEngine.formatDuration(3900_000L))
    }

    @Test
    fun testCsvParsing() = runBlocking {
        val sampleCsv = "Name,Age,Role\nAlice,30,Engineer\nBob,25,Designer"
        val table = FilePreviewEngine.parseCsv(sampleCsv)
        assertEquals(3, table.headers.size)
        assertEquals("Name", table.headers[0])
        assertEquals("Role", table.headers[2])
        assertEquals(2, table.rows.size)
        assertEquals("Alice", table.rows[0][0])
        assertEquals("Designer", table.rows[1][2])
    }
}
