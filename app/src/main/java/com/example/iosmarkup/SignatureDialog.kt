package com.example.iosmarkup

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class SignatureDialog(context: Context, private val onSignatureCaptured: (Bitmap) -> Unit) : Dialog(context) {
    private lateinit var signatureView: SignatureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Sign Here")
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }
        signatureView = SignatureView(context).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500); setBackgroundColor(Color.LTGRAY) }
        val btnLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.END }
        val clear = Button(context).apply { text = "Clear"; setOnClickListener { signatureView.clear() } }
        val done = Button(context).apply { text = "Done"; setOnClickListener { signatureView.getSignatureBitmap()?.let { onSignatureCaptured(it); dismiss() } } }
        btnLayout.addView(clear); btnLayout.addView(done)
        container.addView(signatureView); container.addView(btnLayout)
        setContentView(container)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private class SignatureView(context: Context) : View(context) {
        private val path = Path()
        private val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 8f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
        override fun onDraw(canvas: Canvas) { canvas.drawPath(path, paint) }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> path.moveTo(event.x, event.y)
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> path.lineTo(event.x, event.y)
            }
            invalidate(); return true
        }
        fun clear() { path.reset(); invalidate() }
        fun getSignatureBitmap(): Bitmap? {
            if (path.isEmpty) return null
            val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(b); c.drawPath(path, paint)
            return b
        }
    }
}