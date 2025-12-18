package com.example.iosmarkup
import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.MotionEvent; import android.view.View

class ColorPickerView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    var onColorChanged: ((Int) -> Unit)? = null
    private val hsv = floatArrayOf(0f, 1f, 1f); private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val hueH = 60f
        // Sat/Val Box
        val svRect = RectF(0f, 0f, w, h-hueH-20)
        paint.shader = ComposeShader(LinearGradient(0f,0f,w,0f,Color.WHITE,Color.HSVToColor(floatArrayOf(hsv[0],1f,1f)),Shader.TileMode.CLAMP), LinearGradient(0f,0f,0f,svRect.height(),Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP), PorterDuff.Mode.SRC_OVER)
        canvas.drawRect(svRect, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.color = if (hsv[2]<0.5) Color.WHITE else Color.BLACK; paint.strokeWidth = 5f
        canvas.drawCircle(hsv[1]*w, (1-hsv[2])*svRect.height(), 15f, paint)
        // Hue Bar
        val hueRect = RectF(0f, h-hueH, w, h)
        paint.style = Paint.Style.FILL; paint.shader = LinearGradient(0f,0f,w,0f,intArrayOf(Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED),null,Shader.TileMode.CLAMP)
        canvas.drawRect(hueRect, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.color = Color.WHITE; paint.strokeWidth = 5f
        val hx = (hsv[0]/360f)*w; canvas.drawRect(hx-5, hueRect.top, hx+5, hueRect.bottom, paint)
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x.coerceIn(0f, width.toFloat()); val y = e.y.coerceIn(0f, height.toFloat())
        if (y > height - 80) hsv[0] = (x/width)*360f
        else { hsv[1] = x/width; hsv[2] = 1f - (y/(height-80)) }
        onColorChanged?.invoke(Color.HSVToColor(hsv)); invalidate(); return true
    }
    fun setColor(c: Int) { Color.colorToHSV(c, hsv); invalidate() }
}