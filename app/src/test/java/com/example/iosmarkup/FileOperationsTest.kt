package com.example.iosmarkup

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Unit tests for FileOperations
 * Tests the resource cleanup fix using use {} block
 */
class FileOperationsTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var fileOps: FileOperations
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockContentResolver = mockk(relaxed = true)
        mockUri = mockk(relaxed = true)

        every { mockContext.contentResolver } returns mockContentResolver

        fileOps = FileOperations(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `loadImage returns FileNotFound when stream is null`() = runTest {
        // Given: ContentResolver returns null stream
        every { mockContentResolver.openInputStream(any()) } returns null

        // When
        val result = fileOps.loadImage(mockUri)

        // Then
        assertTrue(result is LoadResult.Error.FileNotFound)
    }

    @Test
    fun `loadImage returns InvalidFormat when bitmap decode fails`() = runTest {
        // Given: Stream with invalid image data
        val invalidStream = ByteArrayInputStream("not an image".toByteArray())
        every { mockContentResolver.openInputStream(any()) } returns invalidStream

        // When
        val result = fileOps.loadImage(mockUri)

        // Then
        assertTrue(result is LoadResult.Error.InvalidFormat)
    }

    @Test
    fun `loadImage closes stream even when decode fails`() = runTest {
        // Given: A mock input stream that tracks if it was closed
        val mockStream = mockk<InputStream>(relaxed = true)
        var streamClosed = false

        every { mockContentResolver.openInputStream(any()) } returns mockStream
        every { mockStream.read(any<ByteArray>()) } returns -1
        every { mockStream.close() } answers {
            streamClosed = true
            Unit
        }

        // Mock BitmapFactory to return null (invalid format)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns null

        // When
        fileOps.loadImage(mockUri)

        // Then: Stream should be closed via use {} block
        assertTrue("Stream should be closed", streamClosed)

        unmockkStatic(BitmapFactory::class)
    }

    @Test
    fun `loadImage returns Success with valid bitmap`() = runTest {
        // Given: Valid image data
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val validStream = ByteArrayInputStream(outputStream.toByteArray())

        every { mockContentResolver.openInputStream(any()) } returns validStream

        // When
        val result = fileOps.loadImage(mockUri)

        // Then
        assertTrue(result is LoadResult.Success)
        assertNotNull((result as LoadResult.Success).bitmap)
    }

    @Test
    fun `loadImage handles OutOfMemoryError`() = runTest {
        // Given: Stream that causes OutOfMemoryError
        val mockStream = mockk<InputStream>(relaxed = true)
        every { mockContentResolver.openInputStream(any()) } returns mockStream

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } throws OutOfMemoryError("Test OOM")

        // When
        val result = fileOps.loadImage(mockUri)

        // Then
        assertTrue(result is LoadResult.Error.OutOfMemory)

        unmockkStatic(BitmapFactory::class)
    }

    @Test
    fun `getLoadErrorMessage returns correct message for FileNotFound`() {
        // Given
        val error = LoadResult.Error.FileNotFound

        // When
        val message = fileOps.getLoadErrorMessage(error)

        // Then
        assertEquals("File not found or inaccessible.", message)
    }

    @Test
    fun `getLoadErrorMessage returns correct message for InvalidFormat`() {
        // Given
        val error = LoadResult.Error.InvalidFormat

        // When
        val message = fileOps.getLoadErrorMessage(error)

        // Then
        assertEquals("Invalid image format.", message)
    }

    @Test
    fun `getLoadErrorMessage returns correct message for OutOfMemory`() {
        // Given
        val error = LoadResult.Error.OutOfMemory

        // When
        val message = fileOps.getLoadErrorMessage(error)

        // Then
        assertEquals("Image too large to load.", message)
    }

    @Test
    fun `getLoadErrorMessage returns correct message for Unknown error`() {
        // Given
        val exception = Exception("Test exception")
        val error = LoadResult.Error.Unknown(exception)

        // When
        val message = fileOps.getLoadErrorMessage(error)

        // Then
        assertEquals("Failed to load image.", message)
    }

    @Test
    fun `getSaveErrorMessage returns correct message for NoPermission`() {
        // Given
        val error = SaveResult.Error.NoPermission

        // When
        val message = fileOps.getSaveErrorMessage(error)

        // Then
        assertEquals("Storage permission denied. Please grant permission in settings.", message)
    }

    @Test
    fun `getSaveErrorMessage returns correct message for NoSpace`() {
        // Given
        val error = SaveResult.Error.NoSpace

        // When
        val message = fileOps.getSaveErrorMessage(error)

        // Then
        assertEquals("Not enough storage space available.", message)
    }

    @Test
    fun `getSaveErrorMessage returns correct message for InvalidFormat`() {
        // Given
        val error = SaveResult.Error.InvalidFormat

        // When
        val message = fileOps.getSaveErrorMessage(error)

        // Then
        assertEquals("Invalid image format.", message)
    }

    @Test
    fun `getSaveErrorMessage returns correct message for Unknown error with custom message`() {
        // Given
        val exception = Exception("Test exception")
        val customMessage = "Custom error message"
        val error = SaveResult.Error.Unknown(exception, customMessage)

        // When
        val message = fileOps.getSaveErrorMessage(error)

        // Then
        assertEquals("Failed to save: $customMessage", message)
    }

    @Test
    fun `LoadResult Success contains bitmap`() {
        // Given
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        // When
        val result = LoadResult.Success(bitmap)

        // Then
        assertNotNull(result.bitmap)
        assertEquals(10, result.bitmap.width)
        assertEquals(10, result.bitmap.height)
    }

    @Test
    fun `SaveResult Success contains filename`() {
        // Given
        val filename = "test_image.png"

        // When
        val result = SaveResult.Success(filename)

        // Then
        assertEquals(filename, result.filePath)
    }

    @Test
    fun `FileConstants have correct values`() {
        // Verify file constants
        assertEquals("Markup_", FileConstants.FILENAME_PREFIX)
        assertEquals("png", FileConstants.PNG_EXTENSION)
        assertEquals("jpg", FileConstants.JPEG_EXTENSION)
        assertEquals("image/png", FileConstants.PNG_MIME_TYPE)
        assertEquals("image/jpeg", FileConstants.JPEG_MIME_TYPE)
        assertEquals(90, FileConstants.JPEG_QUALITY)
        assertEquals(100, FileConstants.PNG_QUALITY)
    }

    @Test
    fun `ExportFormat enum has correct values`() {
        // Verify PNG format
        assertEquals("png", ExportFormat.PNG.extension)
        assertEquals("image/png", ExportFormat.PNG.mimeType)

        // Verify JPEG format
        assertEquals("jpg", ExportFormat.JPEG.extension)
        assertEquals("image/jpeg", ExportFormat.JPEG.mimeType)
    }

    @Test
    fun `SaveLocation enum values exist`() {
        // Verify enum values exist
        assertNotNull(SaveLocation.PICTURES)
        assertNotNull(SaveLocation.DOWNLOADS)
    }
}
