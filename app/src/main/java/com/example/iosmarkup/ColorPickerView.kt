package com.example.iosmarkup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom color picker view with HSV color selection
 * Properly formatted and documented version
 */
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // State
    private var colorPickerState = ColorPickerState()
    
    // Callback
    var onColorChanged: ((Int) -> Unit)? = null
    
    // UI Constants
    private companion object {
        const val HUE_BAR_HEIGHT = 60f
        const val HUE_BAR_MARGIN = 20f
        const val INDICATOR_RADIUS = 15f
        const val INDICATOR_STROKE_WIDTH = 5f
    }
    
    // Paint objects
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw saturation/value box
        drawSaturationValueBox(canvas, width, height)
        
        // Draw hue bar
        drawHueBar(canvas, width, height)
    }
    
    private fun drawSaturationValueBox(canvas: Canvas, width: Float, height: Float) {
        val boxHeight = height - HUE_BAR_HEIGHT - HUE_BAR_MARGIN
        val boxRect = RectF(0f, 0f, width, boxHeight)
        
        // Create the saturation gradient (white to pure color)
        val saturationGradient = LinearGradient(
            0f, 0f, width, 0f,
            Color.WHITE,
            Color.HSVToColor(floatArrayOf(colorPickerState.hue, 1f, 1f)),
            Shader.TileMode.CLAMP
        )
        
        // Create the value gradient (transparent to black)
        val valueGradient = LinearGradient(
            0f, 0f, 0f, boxHeight,
            Color.TRANSPARENT,
            Color.BLACK,
            Shader.TileMode.CLAMP
        )
        
        // Combine gradients
        paint.shader = ComposeShader(
            saturationGradient,
            valueGradient,
            PorterDuff.Mode.SRC_OVER
        )
        canvas.drawRect(boxRect, paint)
        
        // Draw indicator
        drawSVIndicator(canvas, width, boxHeight)
    }
    
    private fun drawSVIndicator(canvas: Canvas, width: Float, boxHeight: Float) {
        val x = colorPickerState.saturation * width
        val y = (1 - colorPickerState.value) * boxHeight
        
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = INDICATOR_STROKE_WIDTH
        
        // Choose indicator color based on background brightness
        paint.color = if (colorPickerState.value < 0.5f) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        
        canvas.drawCircle(x, y, INDICATOR_RADIUS, paint)
    }
    
    private fun drawHueBar(canvas: Canvas, width: Float, height: Float) {
        val barTop = height - HUE_BAR_HEIGHT
        val barRect = RectF(0f, barTop, width, height)
        
        // Create rainbow gradient
        val hueColors = intArrayOf(
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA,
            Color.RED
        )
        
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, width, 0f,
            hueColors,
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(barRect, paint)
        
        // Draw hue indicator
        drawHueIndicator(canvas, width, barRect)
    }
    
    private fun drawHueIndicator(canvas: Canvas, width: Float, barRect: RectF) {
        val indicatorX = (colorPickerState.hue / 360f) * width
        
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = INDICATOR_STROKE_WIDTH
        
        canvas.drawRect(
            indicatorX - 5f,
            barRect.top,
            indicatorX + 5f,
            barRect.bottom,
            paint
        )
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())
        
        val boxHeight = height - HUE_BAR_HEIGHT - HUE_BAR_MARGIN
        val isInHueBar = y > boxHeight + HUE_BAR_MARGIN
        
        if (isInHueBar) {
            // Update hue
            colorPickerState = colorPickerState.copy(
                hue = (x / width) * 360f
            )
        } else {
            // Update saturation and value
            colorPickerState = colorPickerState.copy(
                saturation = x / width,
                value = 1f - (y / boxHeight)
            )
        }
        
        // Notify listener
        onColorChanged?.invoke(colorPickerState.toColor())
        
        // Redraw
        invalidate()
        
        return true
    }
    
    /**
     * Set the current color
     */
    fun setColor(color: Int) {
        colorPickerState = ColorPickerState.fromColor(color)
        invalidate()
    }
    
    /**
     * Get the current color
     */
    fun getColor(): Int = colorPickerState.toColor()
}
