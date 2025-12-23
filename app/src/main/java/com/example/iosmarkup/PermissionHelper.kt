package com.example.iosmarkup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility for handling storage permissions across different Android versions
 */
object PermissionHelper {

    /**
     * Get the required storage permissions for the current Android version
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12 and below use general storage permissions
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Check if all required storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return getStoragePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if any required storage permissions are missing
     */
    fun needsStoragePermissions(context: Context): Boolean {
        return getStoragePermissions().any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get the primary storage permission to check (for simpler checks)
     * This is useful for single permission checks in UI
     */
    fun getPrimaryStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    /**
     * Check if the primary storage permission is granted
     */
    fun hasPrimaryStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            getPrimaryStoragePermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}
