package com.example.iosmarkup

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnCustomColor: MaterialButton
    private lateinit var colorContainer: LinearLayout

    // Tools
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnPen: MaterialButton
    private lateinit var btnMarker: MaterialButton
    private lateinit var btnEraser: MaterialButton
    private lateinit var btnText: MaterialButton
    private lateinit var btnSignature: MaterialButton
    private lateinit var btnShapes: MaterialButton

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val action = result.data?.getStringExtra("ACTION")
            if (action == "CLEAR_CANVAS") {
                drawingView.clearCanvas()
                Toast.makeText(this, "Canvas Cleared", Toast.LENGTH_SHORT).show()
            }
        }
        applySettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE)
        val themeMode = prefs.getInt("APP_THEME", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
        if (prefs.getBoolean("USE_MATERIAL_YOU", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        setupUI()
        applySettings()
    }

    override fun onResume() {
        super.onResume()
        applySettings()
        refreshPaletteBar()
    }

    private fun applySettings() {
        val prefs = getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE)

        // 1. Background
        when(prefs.getInt("CANVAS_BG", 0)) {
            0 -> drawingView.setCanvasColor(Color.WHITE)
            1 -> drawingView.setCanvasColor(Color.parseColor("#FFF8E1"))
            2 -> drawingView.setCanvasColor(Color.parseColor("#121212"))
        }

        // 2. Screen On
        if (prefs.getBoolean("KEEP_SCREEN_ON", false)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 3. Grid
        drawingView.setShowGrid(prefs.getBoolean("SHOW_GRID", false))

        // 4. Accent Color
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        if (!prefs.getBoolean("USE_MATERIAL_YOU", false)) {
            val accent = prefs.getInt("ACCENT_COLOR", Color.parseColor("#4F378B"))
            toolbar.setBackgroundColor(accent)
            toolbar.setTitleTextColor(Color.WHITE)
            toolbar.navigationIcon?.setTint(Color.WHITE)
        }
    }

    private fun setupUI() {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val stream = contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    drawingView.setImage(bitmap)
                } catch (e: Exception) { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
            }
        }
        topAppBar.setNavigationOnClickListener { getContent.launch("image/*") }
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_undo -> { drawingView.undo(); true }
                R.id.action_save -> { saveImageToGallery(drawingView.getBitmap()); true }
                R.id.action_settings -> { settingsLauncher.launch(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }

        findViewById<Slider>(R.id.strokeSlider).addOnChangeListener { _, value, _ -> drawingView.setStrokeWidth(value) }

        colorContainer = findViewById(R.id.colorContainer)
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
            popup.menu.add(0, 1, 0, "Rectangle (Outline)").setIcon(R.drawable.ic_tool_shapes)
            popup.menu.add(0, 2, 0, "Rectangle (Filled)").setIcon(R.drawable.ic_tool_shapes)
            popup.menu.add(0, 3, 0, "Oval (Outline)").setIcon(R.drawable.ic_tool_shapes)
            popup.menu.add(0, 4, 0, "Oval (Filled)").setIcon(R.drawable.ic_tool_shapes)
            popup.menu.add(0, 5, 0, "Arrow").setIcon(R.drawable.ic_tool_select)
            popup.menu.add(0, 6, 0, "Line").setIcon(R.drawable.ic_tool_pen)

            try {
                val mPopup = PopupMenu::class.java.getDeclaredField("mPopup").apply { isAccessible = true }.get(popup)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType).invoke(mPopup, true)
            } catch (e: Exception) {}

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

        findViewById<FloatingActionButton>(R.id.fabAdd).visibility = View.GONE
        updateToolUI(btnPen)
    }

    private fun refreshPaletteBar() {
        val count = colorContainer.childCount
        if (count > 1) colorContainer.removeViews(0, count - 1)

        val colors = PaletteManager.getColors(this)
        for (color in colors) {
            val btn = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(8) }
                backgroundTintList = ColorStateList.valueOf(color)
                stateListAnimator = null; cornerRadius = dp(24); insetTop=0; insetBottom=0
                strokeWidth = dp(1); strokeColor = ColorStateList.valueOf(Color.LTGRAY)
                setOnClickListener { drawingView.setColor(color) }
            }
            colorContainer.addView(btn, colorContainer.childCount - 1)
        }
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
        val preview = View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150); setBackgroundColor(Color.BLACK) }
        layout.addView(preview)
        val picker = ColorPickerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600) }
        layout.addView(picker)
        val hexInput = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 20 }
            hint = "#RRGGBB"; textSize = 16f; setSingleLine(); textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        layout.addView(hexInput)

        picker.onColorChanged = { color ->
            preview.setBackgroundColor(color)
            val hex = String.format("#%06X", (0xFFFFFF and color))
            if (hexInput.text.toString() != hex) { hexInput.setTag("ignore"); hexInput.setText(hex); hexInput.setTag(null) }
        }
        picker.setColor(Color.BLACK); hexInput.setText("#000000")

        AlertDialog.Builder(this).setTitle("Custom Color").setView(layout)
            .setPositiveButton("Add") { _, _ ->
                try {
                    val color = Color.parseColor(hexInput.text.toString())
                    val list = PaletteManager.getColors(this)
                    list.add(color)
                    PaletteManager.saveColors(this, list)
                    refreshPaletteBar()
                    drawingView.setColor(color)
                } catch (e: Exception) {}
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val prefs = getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE)
        val saveLoc = prefs.getString("SAVE_LOCATION", "PICTURES")
        val format = prefs.getString("EXPORT_FORMAT", "PNG")
        val compressFormat = if (format == "JPEG") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val ext = if (format == "JPEG") "jpg" else "png"
        val mime = if (format == "JPEG") "image/jpeg" else "image/png"

        val dir = if (saveLoc == "DOWNLOADS") Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Markup_${System.currentTimeMillis()}.$ext")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
        }

        try {
            val uri = if (saveLoc == "DOWNLOADS" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            contentResolver.insert(uri, values)?.let { u ->
                contentResolver.openOutputStream(u)?.use { out -> bitmap.compress(compressFormat, 100, out); Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) { Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show() }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}