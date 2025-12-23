package com.example.iosmarkup

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.color.DynamicColors
import org.json.JSONArray

/**
 * Repository pattern for settings management
 * Centralizes all SharedPreferences access
 */
class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferenceKeys.SETTINGS_NAME,
        Context.MODE_PRIVATE
    )
    
    // Theme Settings
    fun getTheme(): Int = prefs.getInt(
        PreferenceKeys.APP_THEME,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )
    
    fun setTheme(theme: Int) {
        prefs.edit { putInt(PreferenceKeys.APP_THEME, theme) }
    }
    
    fun isUsingMaterialYou(): Boolean = prefs.getBoolean(
        PreferenceKeys.USE_MATERIAL_YOU,
        false
    )
    
    fun setUseMaterialYou(use: Boolean) {
        prefs.edit { putBoolean(PreferenceKeys.USE_MATERIAL_YOU, use) }
    }
    
    fun getAccentColor(): Int = prefs.getInt(
        PreferenceKeys.ACCENT_COLOR,
        Color.parseColor("#4F378B")
    )
    
    fun setAccentColor(color: Int) {
        prefs.edit { putInt(PreferenceKeys.ACCENT_COLOR, color) }
    }

    /**
     * Apply theme to the given activity
     * Handles both night mode and Material You dynamic colors
     */
    fun applyTheme(activity: AppCompatActivity) {
        val theme = getTheme()
        AppCompatDelegate.setDefaultNightMode(theme)

        if (isUsingMaterialYou() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }
    }

    // Canvas Settings
    fun getCanvasBackground(): CanvasBackground {
        val value = prefs.getInt(PreferenceKeys.CANVAS_BG, 0)
        return when (value) {
            0 -> CanvasBackground.WHITE
            1 -> CanvasBackground.PAPER
            2 -> CanvasBackground.DARK
            else -> CanvasBackground.WHITE
        }
    }
    
    fun setCanvasBackground(background: CanvasBackground) {
        val value = when (background) {
            CanvasBackground.WHITE -> 0
            CanvasBackground.PAPER -> 1
            CanvasBackground.DARK -> 2
        }
        prefs.edit { putInt(PreferenceKeys.CANVAS_BG, value) }
    }
    
    fun shouldShowGrid(): Boolean = prefs.getBoolean(
        PreferenceKeys.SHOW_GRID,
        false
    )
    
    fun setShowGrid(show: Boolean) {
        prefs.edit { putBoolean(PreferenceKeys.SHOW_GRID, show) }
    }
    
    fun shouldKeepScreenOn(): Boolean = prefs.getBoolean(
        PreferenceKeys.KEEP_SCREEN_ON,
        false
    )
    
    fun setKeepScreenOn(keep: Boolean) {
        prefs.edit { putBoolean(PreferenceKeys.KEEP_SCREEN_ON, keep) }
    }
    
    // Export Settings
    fun getSaveLocation(): SaveLocation {
        val location = prefs.getString(PreferenceKeys.SAVE_LOCATION, "PICTURES")
        return when (location) {
            "DOWNLOADS" -> SaveLocation.DOWNLOADS
            else -> SaveLocation.PICTURES
        }
    }
    
    fun setSaveLocation(location: SaveLocation) {
        val value = when (location) {
            SaveLocation.PICTURES -> "PICTURES"
            SaveLocation.DOWNLOADS -> "DOWNLOADS"
        }
        prefs.edit { putString(PreferenceKeys.SAVE_LOCATION, value) }
    }
    
    fun getExportFormat(): ExportFormat {
        val format = prefs.getString(PreferenceKeys.EXPORT_FORMAT, "PNG")
        return when (format) {
            "JPEG" -> ExportFormat.JPEG
            else -> ExportFormat.PNG
        }
    }
    
    fun setExportFormat(format: ExportFormat) {
        val value = when (format) {
            ExportFormat.PNG -> "PNG"
            ExportFormat.JPEG -> "JPEG"
        }
        prefs.edit { putString(PreferenceKeys.EXPORT_FORMAT, value) }
    }
}

/**
 * Repository for color palette management
 */
class PaletteRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferenceKeys.SETTINGS_NAME,
        Context.MODE_PRIVATE
    )
    
    fun getColors(): MutableList<Int> {
        val json = prefs.getString(PreferenceKeys.PALETTE, null)
            ?: return DefaultColors.DEFAULT_PALETTE.toMutableList()
        
        return try {
            val colors = mutableListOf<Int>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                colors.add(jsonArray.getInt(i))
            }
            colors
        } catch (e: Exception) {
            android.util.Log.e("PaletteRepository", "Failed to parse palette", e)
            DefaultColors.DEFAULT_PALETTE.toMutableList()
        }
    }
    
    fun saveColors(colors: List<Int>) {
        try {
            val jsonArray = JSONArray()
            colors.forEach { jsonArray.put(it) }
            prefs.edit { putString(PreferenceKeys.PALETTE, jsonArray.toString()) }
        } catch (e: Exception) {
            android.util.Log.e("PaletteRepository", "Failed to save palette", e)
        }
    }
    
    fun addColor(color: Int) {
        val colors = getColors()
        if (!colors.contains(color)) {
            colors.add(color)
            saveColors(colors)
        }
    }
    
    fun removeColor(color: Int) {
        val colors = getColors()
        colors.remove(color)
        saveColors(colors)
    }
    
    fun resetToDefaults() {
        saveColors(DefaultColors.DEFAULT_PALETTE)
    }
}
