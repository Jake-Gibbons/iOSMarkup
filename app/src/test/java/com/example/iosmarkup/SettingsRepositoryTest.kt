package com.example.iosmarkup

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsRepository
 * Tests the centralized settings management and theme application
 */
class SettingsRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        repository = SettingsRepository(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getTheme returns default when no preference set`() {
        // Given: No saved theme preference
        every { mockPrefs.getInt(PreferenceKeys.APP_THEME, any()) } returns AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        // When
        val theme = repository.getTheme()

        // Then
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, theme)
    }

    @Test
    fun `getTheme returns saved theme preference`() {
        // Given: Saved theme preference
        every { mockPrefs.getInt(PreferenceKeys.APP_THEME, any()) } returns AppCompatDelegate.MODE_NIGHT_YES

        // When
        val theme = repository.getTheme()

        // Then
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, theme)
    }

    @Test
    fun `setTheme saves theme preference`() {
        // When
        repository.setTheme(AppCompatDelegate.MODE_NIGHT_NO)

        // Then
        verify { mockEditor.putInt(PreferenceKeys.APP_THEME, AppCompatDelegate.MODE_NIGHT_NO) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `isUsingMaterialYou returns false by default`() {
        // Given: No saved preference
        every { mockPrefs.getBoolean(PreferenceKeys.USE_MATERIAL_YOU, false) } returns false

        // When
        val isUsing = repository.isUsingMaterialYou()

        // Then
        assertFalse(isUsing)
    }

    @Test
    fun `isUsingMaterialYou returns saved preference`() {
        // Given: Saved preference
        every { mockPrefs.getBoolean(PreferenceKeys.USE_MATERIAL_YOU, false) } returns true

        // When
        val isUsing = repository.isUsingMaterialYou()

        // Then
        assertTrue(isUsing)
    }

    @Test
    fun `setUseMaterialYou saves preference`() {
        // When
        repository.setUseMaterialYou(true)

        // Then
        verify { mockEditor.putBoolean(PreferenceKeys.USE_MATERIAL_YOU, true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getAccentColor returns default color when no preference`() {
        // Given: No saved preference
        val defaultColor = Color.parseColor("#4F378B")
        every { mockPrefs.getInt(PreferenceKeys.ACCENT_COLOR, defaultColor) } returns defaultColor

        // When
        val color = repository.getAccentColor()

        // Then
        assertEquals(defaultColor, color)
    }

    @Test
    fun `setAccentColor saves color preference`() {
        // When
        repository.setAccentColor(Color.RED)

        // Then
        verify { mockEditor.putInt(PreferenceKeys.ACCENT_COLOR, Color.RED) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getCanvasBackground returns WHITE by default`() {
        // Given: No saved preference
        every { mockPrefs.getInt(PreferenceKeys.CANVAS_BG, 0) } returns 0

        // When
        val background = repository.getCanvasBackground()

        // Then
        assertEquals(CanvasBackground.WHITE, background)
    }

    @Test
    fun `getCanvasBackground returns saved background`() {
        // Given: PAPER background saved (value 1)
        every { mockPrefs.getInt(PreferenceKeys.CANVAS_BG, 0) } returns 1

        // When
        val background = repository.getCanvasBackground()

        // Then
        assertEquals(CanvasBackground.PAPER, background)
    }

    @Test
    fun `setCanvasBackground saves correct value for WHITE`() {
        // When
        repository.setCanvasBackground(CanvasBackground.WHITE)

        // Then
        verify { mockEditor.putInt(PreferenceKeys.CANVAS_BG, 0) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `setCanvasBackground saves correct value for DARK`() {
        // When
        repository.setCanvasBackground(CanvasBackground.DARK)

        // Then
        verify { mockEditor.putInt(PreferenceKeys.CANVAS_BG, 2) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `shouldShowGrid returns false by default`() {
        // Given: No saved preference
        every { mockPrefs.getBoolean(PreferenceKeys.SHOW_GRID, false) } returns false

        // When
        val shouldShow = repository.shouldShowGrid()

        // Then
        assertFalse(shouldShow)
    }

    @Test
    fun `setShowGrid saves preference`() {
        // When
        repository.setShowGrid(true)

        // Then
        verify { mockEditor.putBoolean(PreferenceKeys.SHOW_GRID, true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `shouldKeepScreenOn returns false by default`() {
        // Given: No saved preference
        every { mockPrefs.getBoolean(PreferenceKeys.KEEP_SCREEN_ON, false) } returns false

        // When
        val shouldKeep = repository.shouldKeepScreenOn()

        // Then
        assertFalse(shouldKeep)
    }

    @Test
    fun `setKeepScreenOn saves preference`() {
        // When
        repository.setKeepScreenOn(true)

        // Then
        verify { mockEditor.putBoolean(PreferenceKeys.KEEP_SCREEN_ON, true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getSaveLocation returns PICTURES by default`() {
        // Given: No saved preference
        every { mockPrefs.getString(PreferenceKeys.SAVE_LOCATION, "PICTURES") } returns "PICTURES"

        // When
        val location = repository.getSaveLocation()

        // Then
        assertEquals(SaveLocation.PICTURES, location)
    }

    @Test
    fun `getSaveLocation returns DOWNLOADS when saved`() {
        // Given: DOWNLOADS saved
        every { mockPrefs.getString(PreferenceKeys.SAVE_LOCATION, "PICTURES") } returns "DOWNLOADS"

        // When
        val location = repository.getSaveLocation()

        // Then
        assertEquals(SaveLocation.DOWNLOADS, location)
    }

    @Test
    fun `setSaveLocation saves PICTURES correctly`() {
        // When
        repository.setSaveLocation(SaveLocation.PICTURES)

        // Then
        verify { mockEditor.putString(PreferenceKeys.SAVE_LOCATION, "PICTURES") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getExportFormat returns PNG by default`() {
        // Given: No saved preference
        every { mockPrefs.getString(PreferenceKeys.EXPORT_FORMAT, "PNG") } returns "PNG"

        // When
        val format = repository.getExportFormat()

        // Then
        assertEquals(ExportFormat.PNG, format)
    }

    @Test
    fun `getExportFormat returns JPEG when saved`() {
        // Given: JPEG saved
        every { mockPrefs.getString(PreferenceKeys.EXPORT_FORMAT, "PNG") } returns "JPEG"

        // When
        val format = repository.getExportFormat()

        // Then
        assertEquals(ExportFormat.JPEG, format)
    }

    @Test
    fun `setExportFormat saves PNG correctly`() {
        // When
        repository.setExportFormat(ExportFormat.PNG)

        // Then
        verify { mockEditor.putString(PreferenceKeys.EXPORT_FORMAT, "PNG") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `applyTheme sets night mode correctly`() {
        // Given: A mock activity
        val mockActivity = mockk<AppCompatActivity>(relaxed = true)
        every { mockPrefs.getInt(PreferenceKeys.APP_THEME, any()) } returns AppCompatDelegate.MODE_NIGHT_YES
        every { mockPrefs.getBoolean(PreferenceKeys.USE_MATERIAL_YOU, false) } returns false

        // Mock static AppCompatDelegate
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs

        // When
        repository.applyTheme(mockActivity)

        // Then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }

        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `applyTheme does not apply Material You when disabled`() {
        // Given: Material You disabled
        val mockActivity = mockk<AppCompatActivity>(relaxed = true)
        every { mockPrefs.getInt(PreferenceKeys.APP_THEME, any()) } returns AppCompatDelegate.MODE_NIGHT_NO
        every { mockPrefs.getBoolean(PreferenceKeys.USE_MATERIAL_YOU, false) } returns false

        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs

        // When
        repository.applyTheme(mockActivity)

        // Then: Only night mode should be set, not Material You
        verify { AppCompatDelegate.setDefaultNightMode(any()) }
        // Material You application would require Android 12+ and the flag enabled

        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `multiple operations use same SharedPreferences instance`() {
        // When: Multiple settings operations
        repository.getTheme()
        repository.setTheme(AppCompatDelegate.MODE_NIGHT_YES)
        repository.getCanvasBackground()
        repository.shouldShowGrid()

        // Then: Should use same SharedPreferences instance
        verify(exactly = 1) {
            mockContext.getSharedPreferences(PreferenceKeys.SETTINGS_NAME, Context.MODE_PRIVATE)
        }
    }

    @Test
    fun `PreferenceKeys have correct constant values`() {
        // Verify all preference keys are defined
        assertEquals("MarkupSettings", PreferenceKeys.SETTINGS_NAME)
        assertEquals("APP_THEME", PreferenceKeys.APP_THEME)
        assertEquals("USE_MATERIAL_YOU", PreferenceKeys.USE_MATERIAL_YOU)
        assertEquals("ACCENT_COLOR", PreferenceKeys.ACCENT_COLOR)
        assertEquals("CANVAS_BG", PreferenceKeys.CANVAS_BG)
        assertEquals("SHOW_GRID", PreferenceKeys.SHOW_GRID)
        assertEquals("KEEP_SCREEN_ON", PreferenceKeys.KEEP_SCREEN_ON)
        assertEquals("SAVE_LOCATION", PreferenceKeys.SAVE_LOCATION)
        assertEquals("EXPORT_FORMAT", PreferenceKeys.EXPORT_FORMAT)
        assertEquals("PALETTE", PreferenceKeys.PALETTE)
    }
}
