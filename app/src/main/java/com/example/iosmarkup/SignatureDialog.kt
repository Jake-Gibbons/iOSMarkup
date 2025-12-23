package com.example.iosmarkup

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class SignatureDialog(context: Context, private val cb: (Bitmap) -> Unit) : Dialog(context) {

    private lateinit var signatureView: SignatureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Sign")

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        signatureView = SignatureView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                SignatureConstants.DEFAULT_HEIGHT
            )
            setBackgroundColor(SignatureConstants.BACKGROUND_COLOR)
        }
        container.addView(signatureView)

        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val clearButton = Button(context).apply {
            text = "Clear"
            setOnClickListener { signatureView.clear() }
        }
        buttonLayout.addView(clearButton)

        val doneButton = Button(context).apply {
            text = "Done"
            setOnClickListener {
                signatureView.getSignatureBitmap()?.let {
                    cb(it)
                    dismiss()
                }
            }
        }
        buttonLayout.addView(doneButton)
        container.addView(buttonLayout)

        setContentView(container)
        window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private class SignatureView(context: Context) : View(context) {
        private val path = Path()
        private val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = SignatureConstants.DEFAULT_STROKE_WIDTH
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(c: Canvas) {
            c.drawPath(path, paint)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> path.moveTo(e.x, e.y)
                MotionEvent.ACTION_MOVE -> path.lineTo(e.x, e.y)
            }
            invalidate()
            return true
        }

        fun clear() {
            path.reset()
            invalidate()
        }

        fun getSignatureBitmap(): Bitmap? {
            if (path.isEmpty) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawPath(path, paint)
            return bitmap
        }
    }
}