package com.example.iosmarkup

import android.graphics.Color

/**
 * Application-wide constants to eliminate magic strings and numbers
 */
object PreferenceKeys {
    const val SETTINGS_NAME = "MarkupSettings"
    const val APP_THEME = "APP_THEME"
    const val USE_MATERIAL_YOU = "USE_MATERIAL_YOU"
    const val ACCENT_COLOR = "ACCENT_COLOR"
    const val CANVAS_BG = "CANVAS_BG"
    const val SHOW_GRID = "SHOW_GRID"
    const val KEEP_SCREEN_ON = "KEEP_SCREEN_ON"
    const val SAVE_LOCATION = "SAVE_LOCATION"
    const val EXPORT_FORMAT = "EXPORT_FORMAT"
    const val PALETTE = "PALETTE"
}

object DrawingConstants {
    // Selection and bounds
    const val SELECTION_PADDING = 20f
    const val STROKE_BOUNDS_PADDING = 20f
    const val SELECTION_CIRCLE_RADIUS = 15f
    const val SELECTION_STROKE_WIDTH = 3f
    
    // Arrow drawing
    const val ARROW_LENGTH = 40f
    const val ARROW_ANGLE_DEGREES = 30.0 // PI/6
    
    // Grid
    const val GRID_STEP_SIZE = 100f
    const val GRID_STROKE_WIDTH = 2f
    
    // Canvas
    const val MAX_DRAWING_OBJECTS = 500
    const val DEFAULT_STROKE_WIDTH = 10f
    
    // Touch handling
    const val MODE_NONE = 0
    const val MODE_DRAG = 1
    const val MODE_TRANSFORM = 2
    const val MODE_ZOOM = 3
}

object UIConstants {
    const val COLOR_BUTTON_SIZE = 48
    const val COLOR_BUTTON_MARGIN = 8
    const val COLOR_BUTTON_CORNER_RADIUS = 24
    const val COLOR_BUTTON_STROKE_WIDTH = 1
    
    const val COLOR_PICKER_HEIGHT = 600
    const val COLOR_PREVIEW_HEIGHT = 150
    const val COLOR_PICKER_PADDING = 50
    
    const val ACCENT_BUTTON_SIZE = 100
    const val ACCENT_BUTTON_MARGIN = 16
    const val ACCENT_BUTTON_CORNER_RADIUS = 50
}

object DefaultColors {
    val DEFAULT_PALETTE = listOf(
        Color.BLACK,
        Color.RED,
        Color.BLUE,
        Color.GREEN
    )
    
    val ACCENT_COLORS = listOf(
        "#4F378B",
        "#B3261E",
        "#2196F3",
        "#00796B",
        "#FF9800",
        "#1C1B1F"
    )
}

object FileConstants {
    const val FILENAME_PREFIX = "Markup_"
    const val PNG_EXTENSION = "png"
    const val JPEG_EXTENSION = "jpg"
    const val PNG_MIME_TYPE = "image/png"
    const val JPEG_MIME_TYPE = "image/jpeg"
    const val JPEG_QUALITY = 90
    const val PNG_QUALITY = 100
}

object LogTags {
    const val MAIN_ACTIVITY = "MainActivity"
    const val DRAWING_VIEW = "DrawingView"
    const val SETTINGS = "SettingsActivity"
    const val FILE_OPS = "FileOperations"
}
