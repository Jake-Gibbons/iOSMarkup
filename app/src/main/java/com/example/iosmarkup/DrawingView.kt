package com.example.iosmarkup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import java.util.Collections

/**
 * Custom drawing view with proper memory management and clean architecture
 */
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    
    // Drawing objects with proper sealed class hierarchy
    sealed class DrawingObject {
        abstract val paint: Paint
        val matrix = Matrix()
        
        /**
         * Check if a point is contained within this object's bounds
         */
        fun contains(x: Float, y: Float): Boolean {
            val inverse = Matrix()
            matrix.invert(inverse)
            val point = floatArrayOf(x, y)
            inverse.mapPoints(point)
            return getRawBounds().contains(point[0], point[1])
        }
        
        abstract fun getRawBounds(): RectF
        abstract fun drawContent(canvas: Canvas)
        abstract fun recycle()
        
        data class Stroke(
            val path: Path,
            val color: Int,
            val width: Float,
            val type: ToolType,
            override val paint: Paint
        ) : DrawingObject() {
            private val bounds = RectF()
            
            init {
                path.computeBounds(bounds, true)
                bounds.inset(
                    -DrawingConstants.STROKE_BOUNDS_PADDING,
                    -DrawingConstants.STROKE_BOUNDS_PADDING
                )
            }
            
            override fun getRawBounds() = bounds
            override fun drawContent(canvas: Canvas) = canvas.drawPath(path, paint)
            override fun recycle() {
                // Paths don't need recycling
            }
        }
        
        data class TextItem(
            val text: String,
            val x: Float,
            val y: Float,
            override val paint: Paint
        ) : DrawingObject() {
            private val bounds = Rect()
            
            init {
                paint.getTextBounds(text, 0, text.length, bounds)
                bounds.offset(x.toInt(), y.toInt())
            }
            
            override fun getRawBounds() = RectF(
                bounds.left - DrawingConstants.SELECTION_PADDING,
                bounds.top - DrawingConstants.SELECTION_PADDING,
                bounds.right + DrawingConstants.SELECTION_PADDING,
                bounds.bottom + DrawingConstants.SELECTION_PADDING
            )
            
            override fun drawContent(canvas: Canvas) = canvas.drawText(text, x, y, paint)
            override fun recycle() {
                // Text doesn't need recycling
            }
        }
        
        data class ImageItem(
            val bitmap: Bitmap,
            override val paint: Paint
        ) : DrawingObject() {
            override fun getRawBounds() = RectF(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat()
            )
            
            override fun drawContent(canvas: Canvas) {
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            }
            
            override fun recycle() {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
        
        data class ShapeItem(
            val type: ShapeType,
            val x1: Float,
            val y1: Float,
            val x2: Float,
            val y2: Float,
            override val paint: Paint
        ) : DrawingObject() {
            override fun getRawBounds() = RectF(
                min(x1, x2),
                min(y1, y2),
                max(x1, x2),
                max(y1, y2)
            ).apply {
                inset(
                    -DrawingConstants.SELECTION_PADDING,
                    -DrawingConstants.SELECTION_PADDING
                )
            }
            
            override fun drawContent(canvas: Canvas) {
                drawShape(canvas, type, x1, y1, x2, y2, paint)
            }
            
            override fun recycle() {
                // Shapes don't need recycling
            }
        }
    }
    
    // State
    private var currentTool = ToolType.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = DrawingConstants.DEFAULT_STROKE_WIDTH
    var currentShapeType = ShapeType.RECTANGLE
    var isShapeFilled = false
    
    // Drawing objects management
    private val drawingObjects = Collections.synchronizedList(ArrayList<DrawingObject>())
    private val undoStack = Collections.synchronizedList(ArrayList<DrawingObject>())
    private val redoStack = Collections.synchronizedList(ArrayList<DrawingObject>())
    
    // Selection
    private var selectedObject: DrawingObject? = null
    
    // Transformation matrices
    private val viewMatrix = Matrix()
    private val inverseViewMatrix = Matrix()
    
    // Current drawing state
    private var currentPath = Path()
    private var isDraggingShape = false
    private val shapeStart = PointF()
    private val shapeCurrent = PointF()
    
    // Background
    private var backgroundBitmap: Bitmap? = null
    private var showGrid = false
    
    // Touch handling
    private var touchMode = DrawingConstants.MODE_NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var startAngle = 0f
    private var startDist = 0f
    private val midPoint = PointF()
    
    // Paint objects (reused for performance)
    private val basePaint = Paint().apply {
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    private val selectPaint = Paint().apply {
        color = Color.parseColor("#007AFF")
        style = Paint.Style.STROKE
        strokeWidth = DrawingConstants.SELECTION_STROKE_WIDTH
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }
    
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = DrawingConstants.GRID_STROKE_WIDTH
        style = Paint.Style.STROKE
    }
    
    // State callback
    var onStateChanged: ((DrawingState) -> Unit)? = null
    
    /**
     * Set canvas background color
     */
    fun setCanvasColor(color: Int) {
        setBackgroundColor(color)
        // Recycle old bitmap
        backgroundBitmap?.recycle()
        backgroundBitmap = null
        invalidate()
    }
    
    /**
     * Clear all drawing objects
     */
    fun clearCanvas() {
        // Recycle all bitmaps
        drawingObjects.forEach { it.recycle() }
        drawingObjects.clear()
        undoStack.clear()
        redoStack.clear()
        currentPath.reset()
        
        backgroundBitmap?.recycle()
        backgroundBitmap = null
        
        selectedObject = null
        notifyStateChanged()
        invalidate()
    }
    
    /**
     * Set grid visibility
     */
    fun setShowGrid(show: Boolean) {
        showGrid = show
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.save()
        canvas.concat(viewMatrix)
        
        // Draw background image
        backgroundBitmap?.let { bitmap ->
            val left = (width - bitmap.width) / 2f
            val top = (height - bitmap.height) / 2f
            canvas.drawBitmap(bitmap, left, top, null)
        }
        
        // Draw grid if enabled
        if (showGrid) {
            drawGrid(canvas)
        }
        
        // Draw all objects
        synchronized(drawingObjects) {
            for (obj in drawingObjects) {
                // TODO: Add culling optimization for off-screen objects
                canvas.save()
                canvas.concat(obj.matrix)
                obj.drawContent(canvas)

                // Draw selection indicator
                if (obj == selectedObject) {
                    drawSelectionIndicator(canvas, obj)
                }

                canvas.restore()
            }
        }
        
        // Draw current path (while drawing)
        if (!currentPath.isEmpty) {
            val paint = createPaint(currentTool, currentColor, currentStrokeWidth)
            canvas.drawPath(currentPath, paint)
        }
        
        // Draw shape being dragged
        if (isDraggingShape) {
            val paint = createShapePaint(currentColor, currentStrokeWidth, isShapeFilled)
            drawShape(
                canvas,
                currentShapeType,
                shapeStart.x, shapeStart.y,
                shapeCurrent.x, shapeCurrent.y,
                paint
            )
        }
        
        canvas.restore()
    }
    
    private fun drawGrid(canvas: Canvas) {
        val step = DrawingConstants.GRID_STEP_SIZE
        
        // Vertical lines
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        
        // Horizontal lines
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }
    
    private fun drawSelectionIndicator(canvas: Canvas, obj: DrawingObject) {
        val values = FloatArray(9)
        viewMatrix.getValues(values)
        
        // Adjust stroke width based on zoom level
        selectPaint.strokeWidth = DrawingConstants.SELECTION_STROKE_WIDTH / values[Matrix.MSCALE_X]
        
        canvas.drawRect(obj.getRawBounds(), selectPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Transform touch coordinates to canvas space
        val point = floatArrayOf(event.x, event.y)
        viewMatrix.invert(inverseViewMatrix)
        inverseViewMatrix.mapPoints(point)
        val canvasX = point[0]
        val canvasY = point[1]
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(canvasX, canvasY)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event, canvasX, canvasY)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> handleTouchUp()
        }
        
        return true
    }
    
    private fun handleTouchDown(x: Float, y: Float) {
        touchMode = DrawingConstants.MODE_DRAG
        lastTouchX = x
        lastTouchY = y
        
        when (currentTool) {
            ToolType.ERASER -> eraseObjectAt(x, y)
            ToolType.SELECT -> selectObjectAt(x, y)
            ToolType.SHAPE -> startShapeDrag(x, y)
            else -> startDrawing(x, y)
        }
        
        invalidate()
    }
    
    private fun eraseObjectAt(x: Float, y: Float) {
        synchronized(drawingObjects) {
            val iterator = drawingObjects.listIterator(drawingObjects.size)
            while (iterator.hasPrevious()) {
                val obj = iterator.previous()
                if (obj.contains(x, y)) {
                    obj.recycle()
                    iterator.remove()
                    notifyStateChanged()
                    break
                }
            }
        }
    }

    private fun selectObjectAt(x: Float, y: Float) {
        synchronized(drawingObjects) {
            selectedObject = drawingObjects.findLast { it.contains(x, y) }
        }
    }
    
    private fun startShapeDrag(x: Float, y: Float) {
        isDraggingShape = true
        shapeStart.set(x, y)
        shapeCurrent.set(x, y)
        selectedObject = null
    }
    
    private fun startDrawing(x: Float, y: Float) {
        selectedObject = null
        currentPath.reset()
        currentPath.moveTo(x, y)
    }
    
    private fun handlePointerDown(event: MotionEvent) {
        if (currentTool == ToolType.SELECT && selectedObject != null) {
            // Transform selected object
            touchMode = DrawingConstants.MODE_TRANSFORM
            startDist = spacing(event)
            startAngle = rotation(event)
            calculateMidPoint(midPoint, event)
            
            val point = floatArrayOf(midPoint.x, midPoint.y)
            inverseViewMatrix.mapPoints(point)
            midPoint.set(point[0], point[1])
        } else {
            // Zoom canvas
            touchMode = DrawingConstants.MODE_ZOOM
            startDist = spacing(event)
            calculateMidPoint(midPoint, event)
        }
    }
    
    private fun handleTouchMove(event: MotionEvent, canvasX: Float, canvasY: Float) {
        when (touchMode) {
            DrawingConstants.MODE_DRAG -> handleDragMove(canvasX, canvasY)
            DrawingConstants.MODE_TRANSFORM -> handleTransformMove(event)
            DrawingConstants.MODE_ZOOM -> handleZoomMove(event)
        }
        
        invalidate()
    }
    
    private fun handleDragMove(x: Float, y: Float) {
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        
        when {
            currentTool == ToolType.SELECT && selectedObject != null -> {
                selectedObject?.matrix?.postTranslate(dx, dy)
            }
            currentTool == ToolType.SHAPE -> {
                shapeCurrent.set(x, y)
            }
            currentTool != ToolType.ERASER && currentTool != ToolType.SELECT -> {
                currentPath.lineTo(x, y)
            }
        }
        
        lastTouchX = x
        lastTouchY = y
    }
    
    private fun handleTransformMove(event: MotionEvent) {
        selectedObject?.let { obj ->
            val newDist = spacing(event)
            val newAngle = rotation(event)
            
            val scale = newDist / startDist
            val rotation = newAngle - startAngle
            
            obj.matrix.postScale(scale, scale, midPoint.x, midPoint.y)
            obj.matrix.postRotate(rotation, midPoint.x, midPoint.y)
            
            startDist = newDist
            startAngle = newAngle
        }
    }
    
    private fun handleZoomMove(event: MotionEvent) {
        val newDist = spacing(event)
        val scale = newDist / startDist
        
        viewMatrix.postScale(scale, scale, midPoint.x, midPoint.y)
        startDist = newDist
    }
    
    private fun handleTouchUp() {
        when {
            currentTool == ToolType.SHAPE && isDraggingShape -> {
                finishShape()
            }
            currentTool != ToolType.SELECT &&
            currentTool != ToolType.SHAPE &&
            currentTool != ToolType.ERASER &&
            !currentPath.isEmpty -> {
                finishStroke()
            }
        }
        
        touchMode = DrawingConstants.MODE_NONE
        invalidate()
    }
    
    private fun finishShape() {
        isDraggingShape = false
        val paint = createShapePaint(currentColor, currentStrokeWidth, isShapeFilled)
        val shape = DrawingObject.ShapeItem(
            currentShapeType,
            shapeStart.x, shapeStart.y,
            shapeCurrent.x, shapeCurrent.y,
            paint
        )
        addDrawingObject(shape)
    }
    
    private fun finishStroke() {
        val paint = createPaint(currentTool, currentColor, currentStrokeWidth)
        val stroke = DrawingObject.Stroke(
            Path(currentPath),
            currentColor,
            currentStrokeWidth,
            currentTool,
            paint
        )
        addDrawingObject(stroke)
        currentPath.reset()
    }
    
    private fun addDrawingObject(obj: DrawingObject) {
        // Limit number of objects to prevent memory issues
        if (drawingObjects.size >= DrawingConstants.MAX_DRAWING_OBJECTS) {
            val removed = drawingObjects.removeFirst()
            removed.recycle()
        }
        
        drawingObjects.add(obj)
        redoStack.clear() // Clear redo stack when new object is added
        notifyStateChanged()
    }
    
    // Touch helper functions
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return hypot(x.toDouble(), y.toDouble()).toFloat()
    }
    
    private fun rotation(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    }
    
    private fun calculateMidPoint(point: PointF, event: MotionEvent) {
        point.set(
            (event.getX(0) + event.getX(1)) / 2,
            (event.getY(0) + event.getY(1)) / 2
        )
    }
    
    // Public API
    fun setTool(tool: ToolType) {
        currentTool = tool
        selectedObject = null
        notifyStateChanged()
        invalidate()
    }
    
    fun setColor(color: Int) {
        currentColor = color
        notifyStateChanged()
    }
    
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width.coerceIn(
            ValidationConstants.MIN_STROKE_WIDTH,
            ValidationConstants.MAX_STROKE_WIDTH
        )
        notifyStateChanged()
    }
    
    fun setImage(bitmap: Bitmap) {
        backgroundBitmap?.recycle()
        backgroundBitmap = bitmap
        invalidate()
    }
    
    fun undo() {
        if (drawingObjects.isNotEmpty()) {
            val removed = drawingObjects.removeLast()
            undoStack.add(removed)
            notifyStateChanged()
            invalidate()
        }
    }
    
    fun redo() {
        if (undoStack.isNotEmpty()) {
            val restored = undoStack.removeLast()
            drawingObjects.add(restored)
            notifyStateChanged()
            invalidate()
        }
    }
    
    fun canUndo(): Boolean = drawingObjects.isNotEmpty()
    fun canRedo(): Boolean = undoStack.isNotEmpty()
    
    fun addText(text: String) {
        val point = floatArrayOf(width / 2f, height / 2f)
        inverseViewMatrix.mapPoints(point)
        
        val paint = createPaint(ToolType.TEXT, currentColor, currentStrokeWidth)
        val textItem = DrawingObject.TextItem(text, point[0], point[1], paint)
        
        addDrawingObject(textItem)
        invalidate()
    }
    
    fun addSignature(bitmap: Bitmap) {
        val point = floatArrayOf(width / 2f, height / 2f)
        inverseViewMatrix.mapPoints(point)
        
        val imageItem = DrawingObject.ImageItem(bitmap, Paint())
        imageItem.matrix.postTranslate(
            point[0] - bitmap.width / 2,
            point[1] - bitmap.height / 2
        )
        
        addDrawingObject(imageItem)
        invalidate()
    }

    /**
     * Creates a bitmap snapshot of the current canvas state.
     *
     * IMPORTANT - Memory Management:
     * The caller is responsible for recycling the returned bitmap when it's no longer needed.
     * The bitmap is a new copy and independent of the canvas state.
     *
     * Usage Example:
     * ```kotlin
     * val bitmap = drawingView.getBitmap()
     * try {
     *     // Use the bitmap
     *     saveToFile(bitmap)
     * } finally {
     *     // Always recycle when done
     *     bitmap.recycle()
     * }
     * ```
     *
     * @return A new bitmap containing the current canvas content (including background and all drawing objects)
     */
    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        
        backgroundBitmap?.let {
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, null)
        }

        synchronized(drawingObjects) {
            for (obj in drawingObjects) {
                canvas.save()
                canvas.concat(obj.matrix)
                obj.drawContent(canvas)
                canvas.restore()
            }
        }

        return bitmap
    }
    
    private fun notifyStateChanged() {
        onStateChanged?.invoke(
            DrawingState(
                currentTool = currentTool,
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                currentShapeType = currentShapeType,
                isShapeFilled = isShapeFilled,
                canUndo = canUndo(),
                canRedo = canRedo(),
                objectCount = drawingObjects.size
            )
        )
    }
    
    // Paint creation
    private fun createPaint(type: ToolType, color: Int, width: Float): Paint {
        return Paint(basePaint).apply {
            this.color = color
            this.strokeWidth = width
            
            when (type) {
                ToolType.PEN -> {
                    style = Paint.Style.STROKE
                }
                ToolType.MARKER -> {
                    style = Paint.Style.STROKE
                    alpha = 100
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                }
                ToolType.TEXT -> {
                    style = Paint.Style.FILL
                    textSize = width * 5
                    typeface = Typeface.DEFAULT_BOLD
                }
                else -> {}
            }
        }
    }
    
    private fun createShapePaint(color: Int, width: Float, filled: Boolean): Paint {
        return Paint(basePaint).apply {
            this.color = color
            this.strokeWidth = width
            style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // Clean up resources
        backgroundBitmap?.recycle()
        backgroundBitmap = null

        synchronized(drawingObjects) {
            drawingObjects.forEach { it.recycle() }
            drawingObjects.clear()
        }
        synchronized(undoStack) {
            undoStack.forEach { it.recycle() }
            undoStack.clear()
        }
        synchronized(redoStack) {
            redoStack.forEach { it.recycle() }
            redoStack.clear()
        }
    }
    
    companion object {
        /**
         * Draw a shape on the canvas
         */
        fun drawShape(
            canvas: Canvas,
            type: ShapeType,
            x1: Float, y1: Float,
            x2: Float, y2: Float,
            paint: Paint
        ) {
            val left = min(x1, x2)
            val top = min(y1, y2)
            val right = max(x1, x2)
            val bottom = max(y1, y2)
            
            when (type) {
                ShapeType.RECTANGLE -> {
                    canvas.drawRect(left, top, right, bottom, paint)
                }
                ShapeType.OVAL -> {
                    canvas.drawOval(left, top, right, bottom, paint)
                }
                ShapeType.LINE -> {
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                ShapeType.ARROW -> {
                    drawArrow(canvas, x1, y1, x2, y2, paint)
                }
            }
        }
        
        private fun drawArrow(
            canvas: Canvas,
            x1: Float, y1: Float,
            x2: Float, y2: Float,
            paint: Paint
        ) {
            // Draw main line
            canvas.drawLine(x1, y1, x2, y2, paint)
            
            // Calculate arrow head
            val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
            val arrowAngle = Math.toRadians(DrawingConstants.ARROW_ANGLE_DEGREES)
            val length = DrawingConstants.ARROW_LENGTH
            
            // Draw arrow head lines
            canvas.drawLine(
                x2, y2,
                (x2 - length * cos(angle - arrowAngle)).toFloat(),
                (y2 - length * sin(angle - arrowAngle)).toFloat(),
                paint
            )
            canvas.drawLine(
                x2, y2,
                (x2 - length * cos(angle + arrowAngle)).toFloat(),
                (y2 - length * sin(angle + arrowAngle)).toFloat(),
                paint
            )
        }
    }
}
