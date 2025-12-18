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

    sealed class DrawingObject {
        abstract val paint: Paint; val matrix = Matrix()
        fun contains(x: Float, y: Float): Boolean {
            val i = Matrix(); matrix.invert(i); val p = floatArrayOf(x,y); i.mapPoints(p)
            return getRawBounds().contains(p[0], p[1])
        }
        abstract fun getRawBounds(): RectF
        abstract fun drawContent(c: Canvas)
        data class Stroke(val path: Path, val color: Int, val width: Float, val type: ToolType, override val paint: Paint) : DrawingObject() {
            val b = RectF(); init { path.computeBounds(b, true); b.inset(-20f,-20f) }
            override fun getRawBounds() = b
            override fun drawContent(c: Canvas) = c.drawPath(path, paint)
        }
        data class TextItem(val text: String, val x: Float, val y: Float, override val paint: Paint) : DrawingObject() {
            val b = Rect(); init { paint.getTextBounds(text, 0, text.length, b); b.offset(x.toInt(), y.toInt()) }
            override fun getRawBounds() = RectF(b.left-20f, b.top-20f, b.right+20f, b.bottom+20f)
            override fun drawContent(c: Canvas) = c.drawText(text, x, y, paint)
        }
        data class ImageItem(val bitmap: Bitmap, override val paint: Paint) : DrawingObject() {
            override fun getRawBounds() = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            override fun drawContent(c: Canvas) = c.drawBitmap(bitmap, 0f, 0f, null)
        }
        data class ShapeItem(val type: ShapeType, val x1: Float, val y1: Float, val x2: Float, val y2: Float, override val paint: Paint) : DrawingObject() {
            override fun getRawBounds() = RectF(min(x1,x2), min(y1,y2), max(x1,x2), max(y1,y2)).apply{inset(-20f,-20f)}
            override fun drawContent(c: Canvas) = drawShapeLogic(c, type, x1, y1, x2, y2, paint)
        }
    }

    private var currentTool = ToolType.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 10f
    var currentShapeType = ShapeType.RECTANGLE
    var isShapeFilled = false
    private val drawingObjects = ArrayList<DrawingObject>()
    private var selectedObject: DrawingObject? = null
    private val viewMatrix = Matrix(); private val inverseViewMatrix = Matrix()
    private var currentPath = Path()
    private var isDraggingShape = false
    private var shapeStart = PointF(); private var shapeCurrent = PointF()
    private var backgroundBitmap: Bitmap? = null
    private var showGrid = false
    private val gridPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 2f; style = Paint.Style.STROKE }

    private var mode = 0; private var lastTouchX = 0f; private var lastTouchY = 0f; private var startAngle = 0f; private var startDist = 0f; private var midPoint = PointF()
    private val basePaint = Paint().apply { isAntiAlias = true; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val selectPaint = Paint().apply { color = Color.parseColor("#007AFF"); style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(15f,15f), 0f) }

    fun setCanvasColor(c: Int) { setBackgroundColor(c); backgroundBitmap = null; invalidate() }
    fun clearCanvas() { drawingObjects.clear(); currentPath.reset(); backgroundBitmap = null; invalidate() }
    fun setShowGrid(b: Boolean) { showGrid = b; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); canvas.save(); canvas.concat(viewMatrix)
        backgroundBitmap?.let { canvas.drawBitmap(it, (width-it.width)/2f, (height-it.height)/2f, null) }
        if (showGrid) {
            val step = 100f
            for(i in 0..width step 100) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
            for(i in 0..height step 100) canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), gridPaint)
        }
        for (obj in drawingObjects) {
            canvas.save(); canvas.concat(obj.matrix); obj.drawContent(canvas)
            if (obj == selectedObject) { val v = FloatArray(9); viewMatrix.getValues(v); selectPaint.strokeWidth = 3f/v[Matrix.MSCALE_X]; canvas.drawRect(obj.getRawBounds(), selectPaint) }
            canvas.restore()
        }
        if (!currentPath.isEmpty) canvas.drawPath(currentPath, createPaint(currentTool, currentColor, currentStrokeWidth))
        if (isDraggingShape) drawShapeLogic(canvas, currentShapeType, shapeStart.x, shapeStart.y, shapeCurrent.x, shapeCurrent.y, createShapePaint(currentColor, currentStrokeWidth, isShapeFilled))
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val p = floatArrayOf(event.x, event.y); viewMatrix.invert(inverseViewMatrix); inverseViewMatrix.mapPoints(p)
        val cx = p[0]; val cy = p[1]
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = 1; lastTouchX = cx; lastTouchY = cy
                if (currentTool == ToolType.ERASER) {
                    val it = drawingObjects.listIterator(drawingObjects.size)
                    while (it.hasPrevious()) if (it.previous().contains(cx,cy)) { it.remove(); invalidate(); break }
                } else if (currentTool == ToolType.SELECT) {
                    selectedObject = drawingObjects.findLast { it.contains(cx, cy) }
                } else if (currentTool == ToolType.SHAPE) {
                    isDraggingShape = true; shapeStart.set(cx, cy); shapeCurrent.set(cx, cy); selectedObject = null
                } else {
                    selectedObject = null; currentPath.reset(); currentPath.moveTo(cx, cy)
                }
                invalidate()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (currentTool == ToolType.SELECT && selectedObject != null) {
                    mode = 2; startDist = spacing(event); startAngle = rotation(event); midPoint(midPoint, event)
                    val m = floatArrayOf(midPoint.x, midPoint.y); inverseViewMatrix.mapPoints(m); midPoint.set(m[0], m[1])
                } else {
                    mode = 3; startDist = spacing(event); midPoint(midPoint, event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == 1) {
                    val dx = cx - lastTouchX; val dy = cy - lastTouchY
                    if (currentTool == ToolType.SELECT && selectedObject != null) selectedObject?.matrix?.postTranslate(dx, dy)
                    else if (currentTool == ToolType.SHAPE) shapeCurrent.set(cx, cy)
                    else if (currentTool != ToolType.ERASER && currentTool != ToolType.SELECT) currentPath.lineTo(cx, cy)
                    lastTouchX = cx; lastTouchY = cy
                } else if (mode == 2 && selectedObject != null) {
                    val nd = spacing(event); val na = rotation(event); val s = nd/startDist; val r = na-startAngle
                    selectedObject?.matrix?.postScale(s,s,midPoint.x,midPoint.y); selectedObject?.matrix?.postRotate(r,midPoint.x,midPoint.y)
                    startDist = nd; startAngle = na
                } else if (mode == 3) {
                    val nd = spacing(event); val s = nd/startDist; viewMatrix.postScale(s,s,midPoint.x,midPoint.y); startDist = nd
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (currentTool == ToolType.SHAPE && isDraggingShape) {
                    isDraggingShape = false; drawingObjects.add(DrawingObject.ShapeItem(currentShapeType, shapeStart.x, shapeStart.y, shapeCurrent.x, shapeCurrent.y, createShapePaint(currentColor, currentStrokeWidth, isShapeFilled)))
                } else if (currentTool != ToolType.SELECT && currentTool != ToolType.SHAPE && currentTool != ToolType.ERASER && !currentPath.isEmpty) {
                    drawingObjects.add(DrawingObject.Stroke(Path(currentPath), currentColor, currentStrokeWidth, currentTool, createPaint(currentTool, currentColor, currentStrokeWidth))); currentPath.reset()
                }
                mode = 0; invalidate()
            }
        }
        return true
    }

    private fun spacing(e: MotionEvent): Float { val x = e.getX(0)-e.getX(1); val y = e.getY(0)-e.getY(1); return hypot(x.toDouble(), y.toDouble()).toFloat() }
    private fun rotation(e: MotionEvent): Float { val x = e.getX(0)-e.getX(1); val y = e.getY(0)-e.getY(1); return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat() }
    private fun midPoint(p: PointF, e: MotionEvent) { p.set((e.getX(0)+e.getX(1))/2, (e.getY(0)+e.getY(1))/2) }

    companion object {
        fun drawShapeLogic(c: Canvas, type: ShapeType, x1: Float, y1: Float, x2: Float, y2: Float, p: Paint) {
            val l = min(x1,x2); val t = min(y1,y2); val r = max(x1,x2); val b = max(y1,y2)
            when (type) {
                ShapeType.RECTANGLE -> c.drawRect(l,t,r,b,p)
                ShapeType.OVAL -> c.drawOval(l,t,r,b,p)
                ShapeType.LINE -> c.drawLine(x1,y1,x2,y2,p)
                ShapeType.ARROW -> { c.drawLine(x1,y1,x2,y2,p); val a = atan2(y2-y1, x2-x1); val len = 40f
                    c.drawLine(x2, y2, (x2-len*cos(a-PI/6)).toFloat(), (y2-len*sin(a-PI/6)).toFloat(), p)
                    c.drawLine(x2, y2, (x2-len*cos(a+PI/6)).toFloat(), (y2-len*sin(a+PI/6)).toFloat(), p) }
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
        drawingObjects.add(DrawingObject.TextItem(text, p[0], p[1], createPaint(ToolType.TEXT, currentColor, 10f))); invalidate()
    }
    fun addSignature(b: Bitmap) {
        val p = floatArrayOf(width/2f, height/2f); inverseViewMatrix.mapPoints(p)
        val obj = DrawingObject.ImageItem(b, Paint()); obj.matrix.postTranslate(p[0]-b.width/2, p[1]-b.height/2)
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
            ToolType.TEXT -> { p.style = Paint.Style.FILL; p.textSize = width * 5; p.typeface = Typeface.DEFAULT_BOLD }
            else -> {}
        }
        return p
    }
    private fun createShapePaint(color: Int, width: Float, filled: Boolean): Paint = Paint(basePaint).apply { this.color = color; this.strokeWidth = width; style = if (filled) Paint.Style.FILL else Paint.Style.STROKE }
}