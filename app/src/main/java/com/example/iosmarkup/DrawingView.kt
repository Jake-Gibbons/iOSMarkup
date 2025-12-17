package com.example.iosmarkup

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    enum class ToolType { PEN, MARKER, ERASER, TEXT, SELECT, SHAPE }
    enum class ShapeType { RECTANGLE, OVAL, LINE, ARROW }

    // --- Object Model ---
    sealed class DrawingObject {
        abstract val paint: Paint
        val matrix = Matrix()

        fun contains(x: Float, y: Float): Boolean {
            val inverted = Matrix()
            matrix.invert(inverted)
            val point = floatArrayOf(x, y)
            inverted.mapPoints(point)
            return getRawBounds().contains(point[0], point[1])
        }

        abstract fun getRawBounds(): RectF
        abstract fun drawContent(canvas: Canvas)

        data class Stroke(val path: Path, val color: Int, val strokeWidth: Float, val type: ToolType, override val paint: Paint) : DrawingObject() {
            private val boundsCache = RectF()
            init { path.computeBounds(boundsCache, true); boundsCache.inset(-20f, -20f) }
            override fun getRawBounds(): RectF = boundsCache
            override fun drawContent(canvas: Canvas) = canvas.drawPath(path, paint)
        }

        data class TextItem(val text: String, val x: Float, val y: Float, override val paint: Paint) : DrawingObject() {
            private val boundsCache = Rect()
            init {
                paint.getTextBounds(text, 0, text.length, boundsCache)
                boundsCache.offset(x.toInt(), y.toInt())
            }
            override fun getRawBounds(): RectF = RectF(boundsCache.left - 20f, boundsCache.top - 20f, boundsCache.right + 20f, boundsCache.bottom + 20f)
            override fun drawContent(canvas: Canvas) = canvas.drawText(text, x, y, paint)
        }

        data class ImageItem(val bitmap: Bitmap, override val paint: Paint) : DrawingObject() {
            override fun getRawBounds(): RectF = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            override fun drawContent(canvas: Canvas) = canvas.drawBitmap(bitmap, 0f, 0f, null)
        }

        data class ShapeItem(val shapeType: ShapeType, val startX: Float, val startY: Float, val endX: Float, val endY: Float, override val paint: Paint) : DrawingObject() {
            override fun getRawBounds(): RectF = RectF(min(startX, endX), min(startY, endY), max(startX, endX), max(startY, endY)).apply { inset(-20f, -20f) }
            override fun drawContent(canvas: Canvas) = drawShapeLogic(canvas, shapeType, startX, startY, endX, endY, paint)
        }
    }

    private var currentTool = ToolType.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 10f
    var currentShapeType = ShapeType.RECTANGLE
    var isShapeFilled = false

    private val drawingObjects = ArrayList<DrawingObject>()
    private var selectedObject: DrawingObject? = null

    private val viewMatrix = Matrix()
    private val inverseViewMatrix = Matrix()

    private var currentPath = Path()
    private var isDraggingShape = false
    private var shapeStart = PointF()
    private var shapeCurrent = PointF()
    private var backgroundBitmap: Bitmap? = null

    // Multi-touch
    private var mode = 0
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var startAngle = 0f
    private var startDist = 0f
    private var midPoint = PointF()

    private val basePaint = Paint().apply { isAntiAlias = true; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val selectionPaint = Paint().apply { color = Color.parseColor("#007AFF"); style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(viewMatrix)

        backgroundBitmap?.let { canvas.drawBitmap(it, (width - it.width) / 2f, (height - it.height) / 2f, null) }

        for (obj in drawingObjects) {
            canvas.save()
            canvas.concat(obj.matrix)
            obj.drawContent(canvas)
            if (obj == selectedObject) {
                val values = FloatArray(9); viewMatrix.getValues(values)
                selectionPaint.strokeWidth = 3f / values[Matrix.MSCALE_X]
                canvas.drawRect(obj.getRawBounds(), selectionPaint)
            }
            canvas.restore()
        }

        if (!currentPath.isEmpty) canvas.drawPath(currentPath, createPaint(currentTool, currentColor, currentStrokeWidth))

        if (isDraggingShape) {
            val previewPaint = createShapePaint(currentColor, currentStrokeWidth, isShapeFilled)
            drawShapeLogic(canvas, currentShapeType, shapeStart.x, shapeStart.y, shapeCurrent.x, shapeCurrent.y, previewPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchPoint = floatArrayOf(event.x, event.y)
        viewMatrix.invert(inverseViewMatrix)
        inverseViewMatrix.mapPoints(touchPoint)
        val cx = touchPoint[0]; val cy = touchPoint[1]

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = 1; lastTouchX = cx; lastTouchY = cy

                if (currentTool == ToolType.ERASER) {
                    val iterator = drawingObjects.listIterator(drawingObjects.size)
                    while (iterator.hasPrevious()) {
                        val obj = iterator.previous()
                        if (obj.contains(cx, cy)) {
                            iterator.remove(); invalidate(); break
                        }
                    }
                }
                else if (currentTool == ToolType.SELECT) {
                    selectedObject = drawingObjects.findLast { it.contains(cx, cy) }
                }
                else if (currentTool == ToolType.SHAPE) {
                    isDraggingShape = true; shapeStart.set(cx, cy); shapeCurrent.set(cx, cy); selectedObject = null
                }
                else {
                    selectedObject = null; currentPath.reset(); currentPath.moveTo(cx, cy)
                }
                invalidate()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (currentTool == ToolType.SELECT && selectedObject != null) {
                    mode = 2; startDist = spacing(event); startAngle = rotation(event); midPoint(midPoint, event)
                    val midArr = floatArrayOf(midPoint.x, midPoint.y); inverseViewMatrix.mapPoints(midArr); midPoint.set(midArr[0], midArr[1])
                } else {
                    mode = 3; startDist = spacing(event); midPoint(midPoint, event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == 1) {
                    val dx = cx - lastTouchX; val dy = cy - lastTouchY
                    if (currentTool == ToolType.ERASER) { /* Drag to erase logic */ }
                    else if (currentTool == ToolType.SELECT && selectedObject != null) { selectedObject?.matrix?.postTranslate(dx, dy) }
                    else if (currentTool == ToolType.SHAPE) { shapeCurrent.set(cx, cy) }
                    else if (currentTool != ToolType.SELECT) { currentPath.lineTo(cx, cy) }
                    lastTouchX = cx; lastTouchY = cy
                } else if (mode == 2 && selectedObject != null) {
                    val newDist = spacing(event); val newAngle = rotation(event)
                    val scale = newDist / startDist; val rot = newAngle - startAngle
                    selectedObject?.matrix?.postScale(scale, scale, midPoint.x, midPoint.y)
                    selectedObject?.matrix?.postRotate(rot, midPoint.x, midPoint.y)
                    startDist = newDist; startAngle = newAngle
                } else if (mode == 3) {
                    val newDist = spacing(event); val scale = newDist / startDist
                    viewMatrix.postScale(scale, scale, midPoint.x, midPoint.y)
                    startDist = newDist
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (currentTool == ToolType.SHAPE && isDraggingShape) {
                    isDraggingShape = false
                    val paint = createShapePaint(currentColor, currentStrokeWidth, isShapeFilled)
                    drawingObjects.add(DrawingObject.ShapeItem(currentShapeType, shapeStart.x, shapeStart.y, shapeCurrent.x, shapeCurrent.y, paint))
                } else if (currentTool != ToolType.SELECT && currentTool != ToolType.SHAPE && currentTool != ToolType.ERASER && !currentPath.isEmpty) {
                    val paint = createPaint(currentTool, currentColor, currentStrokeWidth)
                    drawingObjects.add(DrawingObject.Stroke(Path(currentPath), currentColor, currentStrokeWidth, currentTool, paint))
                    currentPath.reset()
                }
                mode = 0; invalidate()
            }
        }
        return true
    }

    private fun spacing(e: MotionEvent): Float { val x = e.getX(0)-e.getX(1); val y = e.getY(0)-e.getY(1); return hypot(x.toDouble(), y.toDouble()).toFloat() }
    private fun rotation(e: MotionEvent): Float { val dx = e.getX(0)-e.getX(1); val dy = e.getY(0)-e.getY(1); return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() }
    private fun midPoint(p: PointF, e: MotionEvent) { p.set((e.getX(0)+e.getX(1))/2, (e.getY(0)+e.getY(1))/2) }

    companion object {
        fun drawShapeLogic(c: Canvas, type: ShapeType, x1: Float, y1: Float, x2: Float, y2: Float, p: Paint) {
            val l = min(x1, x2); val t = min(y1, y2); val r = max(x1, x2); val b = max(y1, y2)
            when (type) {
                ShapeType.RECTANGLE -> c.drawRect(l, t, r, b, p)
                ShapeType.OVAL -> c.drawOval(l, t, r, b, p)
                ShapeType.LINE -> c.drawLine(x1, y1, x2, y2, p)
                ShapeType.ARROW -> {
                    c.drawLine(x1, y1, x2, y2, p)
                    val angle = atan2(y2 - y1, x2 - x1); val len = 40f
                    c.drawLine(x2, y2, (x2 - len * cos(angle - PI/6)).toFloat(), (y2 - len * sin(angle - PI/6)).toFloat(), p)
                    c.drawLine(x2, y2, (x2 - len * cos(angle + PI/6)).toFloat(), (y2 - len * sin(angle + PI/6)).toFloat(), p)
                }
            }
        }
    }

    fun setTool(t: ToolType) { currentTool = t; selectedObject = null; invalidate() }
    fun setColor(c: Int) { currentColor = c }
    fun setStrokeWidth(w: Float) { currentStrokeWidth = w }
    fun setImage(b: Bitmap) { backgroundBitmap = b; invalidate() }
    fun undo() { if (drawingObjects.isNotEmpty()) drawingObjects.removeLast(); invalidate() }

    fun addText(text: String) {
        val p = floatArrayOf(width/2f, height/2f); inverseViewMatrix.mapPoints(p)
        drawingObjects.add(DrawingObject.TextItem(text, p[0], p[1], createPaint(ToolType.TEXT, currentColor, 10f)))
        invalidate()
    }
    fun addSignature(b: Bitmap) {
        val p = floatArrayOf(width/2f, height/2f); inverseViewMatrix.mapPoints(p)
        val obj = DrawingObject.ImageItem(b, Paint())
        obj.matrix.postTranslate(p[0]-b.width/2, p[1]-b.height/2)
        drawingObjects.add(obj); invalidate()
    }
    fun getBitmap(): Bitmap {
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); val c = Canvas(b); c.drawColor(Color.WHITE)
        backgroundBitmap?.let { c.drawBitmap(it, (width-it.width)/2f, (height-it.height)/2f, null) }
        for (obj in drawingObjects) { c.save(); c.concat(obj.matrix); obj.drawContent(c); c.restore() }
        return b
    }

    private fun createPaint(type: ToolType, color: Int, width: Float): Paint {
        val p = Paint(basePaint).apply { this.color = color; this.strokeWidth = width }
        when (type) {
            ToolType.PEN -> p.style = Paint.Style.STROKE
            ToolType.MARKER -> { p.style = Paint.Style.STROKE; p.alpha = 100; p.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN) }
            ToolType.ERASER -> { /* Handled via object delete */ }
            ToolType.TEXT -> { p.style = Paint.Style.FILL; p.textSize = width * 5; p.typeface = Typeface.DEFAULT_BOLD }
            else -> {}
        }
        return p
    }
    private fun createShapePaint(color: Int, width: Float, filled: Boolean): Paint {
        return Paint(basePaint).apply { this.color = color; this.strokeWidth = width; this.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE }
    }
}