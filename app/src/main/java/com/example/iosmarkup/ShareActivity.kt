package com.example.iosmarkup

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

/**
 * ShareActivity - Lightweight overlay editor for quick image markup
 * Appears when users share an image to the app
 * Can be configured to open as overlay or full activity
 */
class ShareActivity : AppCompatActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var paletteRepo: PaletteRepository
    private lateinit var fileOps: FileOperations

    private lateinit var drawingView: DrawingView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var strokeSlider: Slider
    private lateinit var colorContainer: LinearLayout
    
    private lateinit var btnPen: MaterialButton
    private lateinit var btnMarker: MaterialButton
    private lateinit var btnEraser: MaterialButton
    private lateinit var btnShapes: MaterialButton

    private var sharedImageUri: Uri? = null
    private var useOverlayMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsRepo = SettingsRepository(this)
        paletteRepo = PaletteRepository(this)
        fileOps = FileOperations(this)

        // Check if overlay mode is enabled in settings
        useOverlayMode = getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE)
            .getBoolean("quick_share_overlay", false)

        if (useOverlayMode) {
            setupOverlayMode()
        }

        // Apply theme
        settingsRepo.applyTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        initializeViews()
        setupToolbar()
        setupDrawingView()
        setupTools()
        setupColorPalette()
        setupStrokeSlider()

        // Handle incoming share intent
        handleShareIntent()
    }

    private fun setupOverlayMode() {
        // Make activity appear as overlay
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
    }

    private fun initializeViews() {
        drawingView = findViewById(R.id.drawingView)
        toolbar = findViewById(R.id.topAppBar)
        strokeSlider = findViewById(R.id.strokeSlider)
        colorContainer = findViewById(R.id.colorContainer)
        
        btnPen = findViewById(R.id.btnPen)
        btnMarker = findViewById(R.id.btnMarker)
        btnEraser = findViewById(R.id.btnEraser)
        btnShapes = findViewById(R.id.btnShapes)
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_close)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_done -> {
                    saveAndShare()
                    true
                }
                R.id.action_undo -> {
                    drawingView.undo()
                    true
                }
                R.id.action_open_full -> {
                    openInFullApp()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDrawingView() {
        drawingView.onStateChanged = { state ->
            // Update UI based on state
            toolbar.menu.findItem(R.id.action_done)?.isEnabled = (state.objectCount > 0)
        }
    }

    private fun setupTools() {
        btnPen.setOnClickListener {
            drawingView.setTool(ToolType.PEN)
            updateToolUI(btnPen)
        }

        btnMarker.setOnClickListener {
            drawingView.setTool(ToolType.MARKER)
            updateToolUI(btnMarker)
        }

        btnEraser.setOnClickListener {
            drawingView.setTool(ToolType.ERASER)
            updateToolUI(btnEraser)
        }

        btnShapes.setOnClickListener { view ->
            showShapesMenu(view)
        }

        // Start with pen tool
        updateToolUI(btnPen)
    }

    private fun setupColorPalette() {
        colorContainer.removeAllViews()

        // Get palette colors
        val colors = paletteRepo.getColors()
        
        colors.forEach { color ->
            val button = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dp(40),
                    dp(40)
                ).apply {
                    marginEnd = dp(8)
                }
                backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                strokeWidth = 0
                insetTop = 0
                insetBottom = 0
                minimumWidth = 0
                minimumHeight = 0
                setOnClickListener {
                    drawingView.setColor(color)
                    updateColorUI(this)
                }
            }
            colorContainer.addView(button)
        }
    }

    private fun setupStrokeSlider() {
        strokeSlider.addOnChangeListener { _, value, _ ->
            drawingView.setStrokeWidth(value)
        }
        drawingView.setStrokeWidth(strokeSlider.value)
    }

    private fun updateToolUI(selectedButton: MaterialButton) {
        listOf(btnPen, btnMarker, btnEraser, btnShapes).forEach { btn ->
            btn.backgroundTintList = if (btn == selectedButton) {
                android.content.res.ColorStateList.valueOf(
                    getColor(com.google.android.material.R.color.material_dynamic_primary30)
                )
            } else {
                null
            }
        }
    }

    private fun updateColorUI(selectedButton: MaterialButton) {
        for (i in 0 until colorContainer.childCount) {
            val child = colorContainer.getChildAt(i)
            if (child is MaterialButton) {
                child.strokeWidth = if (child == selectedButton) dp(3) else 0
            }
        }
    }

    private fun showShapesMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.shapes_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.shape_rectangle -> setShape(ShapeType.RECTANGLE, false)
                R.id.shape_rectangle_filled -> setShape(ShapeType.RECTANGLE, true)
                R.id.shape_oval -> setShape(ShapeType.OVAL, false)
                R.id.shape_oval_filled -> setShape(ShapeType.OVAL, true)
                R.id.shape_arrow -> setShape(ShapeType.ARROW, false)
                R.id.shape_line -> setShape(ShapeType.LINE, false)
            }
            updateToolUI(btnShapes)
            true
        }

        popup.show()
    }

    private fun setShape(type: ShapeType, filled: Boolean) {
        drawingView.setTool(ToolType.SHAPE)
        drawingView.currentShapeType = type
        drawingView.isShapeFilled = filled
    }

    private fun handleShareIntent() {
        when {
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                sharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                loadSharedImage()
            }
            intent?.action == Intent.ACTION_VIEW && intent.data != null -> {
                sharedImageUri = intent.data
                loadSharedImage()
            }
            else -> {
                showToast("No image to markup")
                finish()
            }
        }
    }

    private fun loadSharedImage() {
        sharedImageUri?.let { uri ->
            lifecycleScope.launch {
                when (val result = fileOps.loadImage(uri)) {
                    is LoadResult.Success -> {
                        drawingView.setImage(result.bitmap)
                    }
                    is LoadResult.Error -> {
                        val message = fileOps.getLoadErrorMessage(result)
                        showToast(message)
                        finish()
                    }
                }
            }
        }
    }

    private fun saveAndShare() {
        lifecycleScope.launch {
            val bitmap = drawingView.getBitmap()
            try {
                val format = settingsRepo.getExportFormat()
                val location = settingsRepo.getSaveLocation()
                
                when (val result = fileOps.saveImageToGallery(bitmap, format, location)) {
                    is SaveResult.Success -> {
                        showToast("Saved: ${result.filePath}")
                        finish()
                    }
                    is SaveResult.Error -> {
                        val message = fileOps.getSaveErrorMessage(result)
                        showToast(message)
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun openInFullApp() {
        // Open MainActivity with current image
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, sharedImageUri)
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
