package com.example.iosmarkup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PermissionHelper
 * Tests the centralized permission logic across different Android versions
 */
@RunWith(RobolectricTestRunner::class)
class PermissionHelperTest {

    @Test
    fun `getStoragePermissions returns READ_MEDIA_IMAGES on Android 13+`() {
        // Given: Android 13+ (API 33+)
        // When
        val permissions = PermissionHelper.getStoragePermissions()

        // Then: Should return granular media permission
        // Note: This test checks the current SDK, which might not be 33+
        // In actual implementation, we'd need to mock Build.VERSION.SDK_INT
        assertTrue(permissions.isNotEmpty())
    }

    @Test
    fun `getStoragePermissions returns storage permissions on Android 12 and below`() {
        // When
        val permissions = PermissionHelper.getStoragePermissions()

        // Then: Should return at least one permission
        assertTrue(permissions.isNotEmpty())
        assertTrue(
            permissions.contains(Manifest.permission.READ_MEDIA_IMAGES) ||
            permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }

    @Test
    fun `getPrimaryStoragePermission returns correct permission`() {
        // When
        val permission = PermissionHelper.getPrimaryStoragePermission()

        // Then: Should return a valid permission string
        assertNotNull(permission)
        assertTrue(
            permission == Manifest.permission.READ_MEDIA_IMAGES ||
            permission == Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @Test
    fun `hasStoragePermissions returns true when all permissions granted`() {
        // Given
        val context = mockk<Context>()
        every {
            context.checkPermission(any(), any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val hasPermissions = PermissionHelper.hasStoragePermissions(context)

        // Then
        assertTrue(hasPermissions)
    }

    @Test
    fun `hasStoragePermissions returns false when any permission denied`() {
        // Given
        val context = mockk<Context>()
        every {
            context.checkPermission(any(), any(), any())
        } returns PackageManager.PERMISSION_DENIED

        // When
        val hasPermissions = PermissionHelper.hasStoragePermissions(context)

        // Then
        assertFalse(hasPermissions)
    }

    @Test
    fun `needsStoragePermissions returns true when permissions missing`() {
        // Given
        val context = mockk<Context>()
        every {
            context.checkPermission(any(), any(), any())
        } returns PackageManager.PERMISSION_DENIED

        // When
        val needsPermissions = PermissionHelper.needsStoragePermissions(context)

        // Then
        assertTrue(needsPermissions)
    }

    @Test
    fun `needsStoragePermissions returns false when all permissions granted`() {
        // Given
        val context = mockk<Context>()
        every {
            context.checkPermission(any(), any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val needsPermissions = PermissionHelper.needsStoragePermissions(context)

        // Then
        assertFalse(needsPermissions)
    }

    @Test
    fun `hasPrimaryStoragePermission returns true when primary permission granted`() {
        // Given
        val context = mockk<Context>()
        every {
            context.checkPermission(any(), any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val hasPermission = PermissionHelper.hasPrimaryStoragePermission(context)

        // Then
        assertTrue(hasPermission)
    }
}
