package com.example.iosmarkup

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

/**
 * Type-safe data models to replace primitive obsession
 */

data class Point(val x: Float, val y: Float) {
    fun toPointF() = PointF(x, y)
}

data class Size(val width: Int, val height: Int)

data class StrokeStyle(
    val width: Float,
    val color: Int,
    val alpha: Int = 255
)

sealed class SaveResult {
    data class Success(val filePath: String) : SaveResult()
    
    sealed class Error : SaveResult() {
        object NoPermission : Error()
        object NoSpace : Error()
        object InvalidFormat : Error()
        data class Unknown(val exception: Exception, val message: String) : Error()
    }
}

sealed class LoadResult {
    data class Success(val bitmap: Bitmap) : LoadResult()
    
    sealed class Error : LoadResult() {
        object FileNotFound : Error()
        object InvalidFormat : Error()
        object OutOfMemory : Error()
        data class Unknown(val exception: Exception) : Error()
    }
}

data class ColorPickerState(
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val value: Float = 1f
) {
    fun toColor(): Int {
        val hsv = floatArrayOf(hue, saturation, value)
        return android.graphics.Color.HSVToColor(hsv)
    }
    
    companion object {
        fun fromColor(color: Int): ColorPickerState {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            return ColorPickerState(hsv[0], hsv[1], hsv[2])
        }
    }
}

/**
 * Represents the state of the drawing canvas
 */
data class DrawingState(
    val currentTool: ToolType = ToolType.PEN,
    val currentColor: Int = android.graphics.Color.BLACK,
    val currentStrokeWidth: Float = DrawingConstants.DEFAULT_STROKE_WIDTH,
    val currentShapeType: ShapeType = ShapeType.RECTANGLE,
    val isShapeFilled: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val objectCount: Int = 0
)

enum class ToolType {
    PEN, MARKER, ERASER, TEXT, SELECT, SHAPE
}

enum class ShapeType {
    RECTANGLE, OVAL, LINE, ARROW
}

enum class CanvasBackground(val colorValue: Int) {
    WHITE(android.graphics.Color.WHITE),
    PAPER(android.graphics.Color.parseColor("#FFF8E1")),
    DARK(android.graphics.Color.parseColor("#121212"))
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

enum class SaveLocation(val directory: String) {
    PICTURES("Pictures"),
    DOWNLOADS("Downloads")
}
