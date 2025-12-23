package com.example.iosmarkup

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Handles all file operations with proper error handling and coroutines
 */
class FileOperations(private val context: Context) {
    
    /**
     * Load an image from URI
     */
    suspend fun loadImage(uri: Uri): LoadResult = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext LoadResult.Error.FileNotFound
            
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            
            if (bitmap == null) {
                LoadResult.Error.InvalidFormat
            } else {
                LoadResult.Success(bitmap)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(LogTags.FILE_OPS, "Out of memory loading image", e)
            LoadResult.Error.OutOfMemory
        } catch (e: IOException) {
            Log.e(LogTags.FILE_OPS, "IO error loading image", e)
            LoadResult.Error.Unknown(e)
        } catch (e: Exception) {
            Log.e(LogTags.FILE_OPS, "Unknown error loading image", e)
            LoadResult.Error.Unknown(e)
        }
    }
    
    /**
     * Save a bitmap to the gallery
     */
    suspend fun saveImageToGallery(
        bitmap: Bitmap,
        format: ExportFormat,
        location: SaveLocation
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val compressFormat = when (format) {
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            }
            
            val quality = when (format) {
                ExportFormat.PNG -> FileConstants.PNG_QUALITY
                ExportFormat.JPEG -> FileConstants.JPEG_QUALITY
            }
            
            val directory = when (location) {
                SaveLocation.PICTURES -> Environment.DIRECTORY_PICTURES
                SaveLocation.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
            }
            
            val filename = "${FileConstants.FILENAME_PREFIX}${System.currentTimeMillis()}.${format.extension}"
            
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                }
            }
            
            val uri = getMediaStoreUri(location)
            val insertedUri = context.contentResolver.insert(uri, values)
                ?: return@withContext SaveResult.Error.NoPermission
            
            context.contentResolver.openOutputStream(insertedUri)?.use { outputStream ->
                val success = bitmap.compress(compressFormat, quality, outputStream)
                if (!success) {
                    // Delete the empty file
                    context.contentResolver.delete(insertedUri, null, null)
                    return@withContext SaveResult.Error.NoSpace
                }
            } ?: return@withContext SaveResult.Error.NoPermission
            
            Log.d(LogTags.FILE_OPS, "Image saved successfully: $filename")
            SaveResult.Success(filename)
            
        } catch (e: SecurityException) {
            Log.e(LogTags.FILE_OPS, "Permission denied saving image", e)
            SaveResult.Error.NoPermission
        } catch (e: IOException) {
            Log.e(LogTags.FILE_OPS, "IO error saving image", e)
            
            // Check if it's a space issue
            val message = e.message?.lowercase() ?: ""
            if (message.contains("space") || message.contains("full")) {
                SaveResult.Error.NoSpace
            } else {
                SaveResult.Error.Unknown(e, "Failed to write file")
            }
        } catch (e: Exception) {
            Log.e(LogTags.FILE_OPS, "Unknown error saving image", e)
            SaveResult.Error.Unknown(e, "Unexpected error occurred")
        }
    }
    
    private fun getMediaStoreUri(location: SaveLocation): Uri {
        return if (location == SaveLocation.DOWNLOADS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }
    
    /**
     * Get a user-friendly error message for a save error
     */
    fun getSaveErrorMessage(error: SaveResult.Error): String {
        return when (error) {
            is SaveResult.Error.NoPermission -> 
                "Storage permission denied. Please grant permission in settings."
            is SaveResult.Error.NoSpace -> 
                "Not enough storage space available."
            is SaveResult.Error.InvalidFormat -> 
                "Invalid image format."
            is SaveResult.Error.Unknown -> 
                "Failed to save: ${error.message}"
        }
    }
    
    /**
     * Get a user-friendly error message for a load error
     */
    fun getLoadErrorMessage(error: LoadResult.Error): String {
        return when (error) {
            is LoadResult.Error.FileNotFound -> 
                "File not found or inaccessible."
            is LoadResult.Error.InvalidFormat -> 
                "Invalid image format."
            is LoadResult.Error.OutOfMemory -> 
                "Image too large to load."
            is LoadResult.Error.Unknown -> 
                "Failed to load image."
        }
    }
}
