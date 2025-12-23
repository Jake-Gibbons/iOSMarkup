package com.example.iosmarkup

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import io.mockk.*
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PaletteRepository
 * Tests the consolidated palette management that replaced the duplicate PaletteManager
 */
class PaletteRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: PaletteRepository

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        repository = PaletteRepository(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getColors returns default palette when no saved data`() {
        // Given: No saved palette data
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns null

        // When
        val colors = repository.getColors()

        // Then: Should return default palette
        assertEquals(DefaultColors.DEFAULT_PALETTE.size, colors.size)
        assertTrue(colors.containsAll(DefaultColors.DEFAULT_PALETTE))
    }

    @Test
    fun `getColors returns saved palette when data exists`() {
        // Given: Saved palette data
        val savedColors = listOf(Color.RED, Color.BLUE, Color.GREEN)
        val jsonArray = JSONArray(savedColors)
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns jsonArray.toString()

        // When
        val colors = repository.getColors()

        // Then: Should return saved colors
        assertEquals(3, colors.size)
        assertTrue(colors.contains(Color.RED))
        assertTrue(colors.contains(Color.BLUE))
        assertTrue(colors.contains(Color.GREEN))
    }

    @Test
    fun `getColors returns default palette on parse error`() {
        // Given: Corrupted JSON data
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns "invalid json"

        // When
        val colors = repository.getColors()

        // Then: Should fall back to default palette
        assertEquals(DefaultColors.DEFAULT_PALETTE.size, colors.size)
        assertTrue(colors.containsAll(DefaultColors.DEFAULT_PALETTE))
    }

    @Test
    fun `saveColors persists colors to SharedPreferences`() {
        // Given: Colors to save
        val colorsToSave = listOf(Color.RED, Color.BLUE, Color.GREEN)
        val jsonCapture = slot<String>()

        every { mockEditor.putString(PreferenceKeys.PALETTE, capture(jsonCapture)) } returns mockEditor

        // When
        repository.saveColors(colorsToSave)

        // Then: Should save as JSON
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }

        // Verify JSON contains the colors
        val savedJson = jsonCapture.captured
        val jsonArray = JSONArray(savedJson)
        assertEquals(3, jsonArray.length())
        assertEquals(Color.RED, jsonArray.getInt(0))
        assertEquals(Color.BLUE, jsonArray.getInt(1))
        assertEquals(Color.GREEN, jsonArray.getInt(2))
    }

    @Test
    fun `saveColors handles empty list`() {
        // Given: Empty color list
        val emptyList = emptyList<Int>()

        // When
        repository.saveColors(emptyList)

        // Then: Should save empty JSON array
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `addColor adds new color to palette`() {
        // Given: Existing palette
        val existingColors = listOf(Color.RED, Color.BLUE)
        val jsonArray = JSONArray(existingColors)
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns jsonArray.toString()

        // When: Adding a new color
        repository.addColor(Color.GREEN)

        // Then: Should save palette with new color
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `addColor does not add duplicate color`() {
        // Given: Existing palette with RED
        val existingColors = listOf(Color.RED, Color.BLUE)
        val jsonArray = JSONArray(existingColors)
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns jsonArray.toString()

        val capturedJson = slot<String>()
        every { mockEditor.putString(PreferenceKeys.PALETTE, capture(capturedJson)) } returns mockEditor

        // When: Trying to add RED again
        repository.addColor(Color.RED)

        // Then: Should not add duplicate
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }

        // Verify color count didn't increase
        val savedJsonArray = JSONArray(capturedJson.captured)
        assertEquals(2, savedJsonArray.length())
    }

    @Test
    fun `removeColor removes color from palette`() {
        // Given: Existing palette
        val existingColors = listOf(Color.RED, Color.BLUE, Color.GREEN)
        val jsonArray = JSONArray(existingColors)
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns jsonArray.toString()

        val capturedJson = slot<String>()
        every { mockEditor.putString(PreferenceKeys.PALETTE, capture(capturedJson)) } returns mockEditor

        // When: Removing a color
        repository.removeColor(Color.BLUE)

        // Then: Should save palette without removed color
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }

        // Verify color was removed
        val savedJsonArray = JSONArray(capturedJson.captured)
        assertEquals(2, savedJsonArray.length())
        assertEquals(Color.RED, savedJsonArray.getInt(0))
        assertEquals(Color.GREEN, savedJsonArray.getInt(1))
    }

    @Test
    fun `removeColor handles color not in palette`() {
        // Given: Existing palette without YELLOW
        val existingColors = listOf(Color.RED, Color.BLUE)
        val jsonArray = JSONArray(existingColors)
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns jsonArray.toString()

        // When: Trying to remove YELLOW
        repository.removeColor(Color.YELLOW)

        // Then: Should still save (no error)
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `resetToDefaults saves default palette`() {
        // When
        repository.resetToDefaults()

        // Then: Should save default palette
        verify { mockEditor.putString(PreferenceKeys.PALETTE, any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `multiple operations use same SharedPreferences instance`() {
        // Given: Mock to track SharedPreferences access
        every { mockPrefs.getString(PreferenceKeys.PALETTE, null) } returns null

        // When: Multiple operations
        repository.getColors()
        repository.saveColors(listOf(Color.RED))
        repository.addColor(Color.BLUE)

        // Then: Should use same SharedPreferences instance (verify context was called once in setup)
        verify(exactly = 1) { mockContext.getSharedPreferences(PreferenceKeys.SETTINGS_NAME, Context.MODE_PRIVATE) }
    }
}
