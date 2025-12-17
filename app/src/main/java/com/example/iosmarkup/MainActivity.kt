
package com.example.iosmarkup

import android.app.AlertDialog
import android.content.ContentValues
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnCustomColor: MaterialButton
    
    // Tools
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnPen: MaterialButton
    private lateinit var btnMarker: MaterialButton
    private lateinit var btnEraser: MaterialButton
    private lateinit var btnText: MaterialButton
    private lateinit var btnSignature: MaterialButton
    private lateinit var btnShapes: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        setupUI()
    }

    private fun setupUI() {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val stream = contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    drawingView.setImage(bitmap)
                } catch (_: Exception) { Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show() }
            }
        }
        topAppBar.setNavigationOnClickListener { getContent.launch("image/*") }
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_undo -> { drawingView.undo(); true }
                R.id.action_save -> { saveImageToGallery(drawingView.getBitmap()); true }
                else -> false
            }
        }

        findViewById<Slider>(R.id.strokeSlider).addOnChangeListener { _, value, _ -> drawingView.setStrokeWidth(value) }
        findViewById<View>(R.id.btnColorBlack).setOnClickListener { drawingView.setColor(Color.parseColor("#1C1B1F")) }
        findViewById<View>(R.id.btnColorRed).setOnClickListener { drawingView.setColor(Color.parseColor("#B3261E")) }
        findViewById<View>(R.id.btnColorBlue).setOnClickListener { drawingView.setColor(Color.parseColor("#2196F3")) }
        findViewById<View>(R.id.btnColorGreen).setOnClickListener { drawingView.setColor(Color.parseColor("#4CAF50")) }

        btnCustomColor = findViewById(R.id.btnColorCustom)
        btnCustomColor.setOnClickListener { showColorPicker() }

        btnSelect = findViewById(R.id.btnSelect)
        btnPen = findViewById(R.id.btnPen)
        btnMarker = findViewById(R.id.btnMarker)
        btnEraser = findViewById(R.id.btnEraser)
        btnText = findViewById(R.id.btnText)
        btnSignature = findViewById(R.id.btnSignature)
        btnShapes = findViewById(R.id.btnShapes)

        btnSelect.setOnClickListener { drawingView.setTool(DrawingView.ToolType.SELECT); updateToolUI(btnSelect) }
        btnPen.setOnClickListener { drawingView.setTool(DrawingView.ToolType.PEN); updateToolUI(btnPen) }
        btnMarker.setOnClickListener { drawingView.setTool(DrawingView.ToolType.MARKER); updateToolUI(btnMarker) }
        btnEraser.setOnClickListener { drawingView.setTool(DrawingView.ToolType.ERASER); updateToolUI(btnEraser) }
        btnText.setOnClickListener { addTextFlow(); updateToolUI(btnText) }
        btnSignature.setOnClickListener { SignatureDialog(this) { bmp -> drawingView.addSignature(bmp); drawingView.setTool(DrawingView.ToolType.SELECT); updateToolUI(btnSelect) }.show() }
        
        btnShapes.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Rectangle (Outline)")
            popup.menu.add(0, 2, 0, "Rectangle (Filled)")
            popup.menu.add(0, 3, 0, "Oval (Outline)")
            popup.menu.add(0, 4, 0, "Oval (Filled)")
            popup.menu.add(0, 5, 0, "Arrow")
            popup.menu.add(0, 6, 0, "Line")
            popup.setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    1 -> setShape(DrawingView.ShapeType.RECTANGLE, false)
                    2 -> setShape(DrawingView.ShapeType.RECTANGLE, true)
                    3 -> setShape(DrawingView.ShapeType.OVAL, false)
                    4 -> setShape(DrawingView.ShapeType.OVAL, true)
                    5 -> setShape(DrawingView.ShapeType.ARROW, false)
                    6 -> setShape(DrawingView.ShapeType.LINE, false)
                }
                updateToolUI(btnShapes)
                true
            }
            popup.show()
        }

        updateToolUI(btnPen)
    }

    private fun updateToolUI(activeButton: MaterialButton) {
        val allButtons = listOf(btnSelect, btnPen, btnMarker, btnEraser, btnText, btnSignature, btnShapes)
        val unselectedBg = ContextCompat.getColor(this, R.color.md_theme_surface_variant)
        val unselectedIcon = ContextCompat.getColor(this, R.color.tool_unselected_icon)
        for (btn in allButtons) { btn.backgroundTintList = ColorStateList.valueOf(unselectedBg); btn.iconTint = ColorStateList.valueOf(unselectedIcon) }
        val selectedBg = ContextCompat.getColor(this, R.color.tool_selected_bg)
        val selectedIcon = ContextCompat.getColor(this, R.color.tool_selected_icon)
        activeButton.backgroundTintList = ColorStateList.valueOf(selectedBg)
        activeButton.iconTint = ColorStateList.valueOf(selectedIcon)
    }

    private fun setShape(type: DrawingView.ShapeType, filled: Boolean) {
        drawingView.setTool(DrawingView.ToolType.SHAPE)
        drawingView.currentShapeType = type
        drawingView.isShapeFilled = filled
    }

    private fun addTextFlow() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("Add Text").setView(input)
            .setPositiveButton("Add") { _, _ -> if (input.text.isNotEmpty()) { drawingView.addText(input.text.toString()); drawingView.setTool(DrawingView.ToolType.SELECT); updateToolUI(btnSelect) } }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showColorPicker() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 10) }
        val preview = View(this).apply { layoutParams = LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 150); setBackgroundColor(Color.BLACK) }
        layout.addView(preview)
        val red = createSlider(layout, "Red"); val green = createSlider(layout, "Green"); val blue = createSlider(layout, "Blue")
        val updateColor = { preview.setBackgroundColor(Color.rgb(red.progress, green.progress, blue.progress)) }
        val listener = SimpleSeekListener(updateColor)
        red.setOnSeekBarChangeListener(listener); green.setOnSeekBarChangeListener(listener); blue.setOnSeekBarChangeListener(listener)
        AlertDialog.Builder(this).setTitle("Custom Color").setView(layout)
            .setPositiveButton("Select") { _, _ ->
                val finalColor = Color.rgb(red.progress, green.progress, blue.progress)
                drawingView.setColor(finalColor)
                btnCustomColor.backgroundTintList = ColorStateList.valueOf(finalColor)
                val isDark = (Color.red(finalColor)*0.299 + Color.green(finalColor)*0.587 + Color.blue(finalColor)*0.114) < 186
                btnCustomColor.iconTint = ColorStateList.valueOf(if(isDark) Color.WHITE else Color.BLACK)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun createSlider(container: LinearLayout, label: String): SeekBar {
        container.addView(TextView(this).apply { text = label; setPadding(0, 20, 0, 0) })
        val sb = SeekBar(this).apply { max = 255 }
        container.addView(sb); return sb
    }

    class SimpleSeekListener(val action: () -> Unit) : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) = action()
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "Markup_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) }
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}
