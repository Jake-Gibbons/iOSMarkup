package com.example.iosmarkup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

/**
 * Improved MainActivity with proper architecture, error handling, and memory management
 * Compatible with existing menu resources
 */
class MainActivity : AppCompatActivity() {

    // Repositories
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var paletteRepo: PaletteRepository
    private lateinit var fileOps: FileOperations

    // Views
    private lateinit var drawingView: DrawingView
    private lateinit var btnCustomColor: MaterialButton
    private lateinit var colorContainer: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var strokeSlider: Slider

    // Tool buttons
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnPen: MaterialButton
    private lateinit var btnMarker: MaterialButton
    private lateinit var btnEraser: MaterialButton
    private lateinit var btnText: MaterialButton
    private lateinit var btnSignature: MaterialButton
    private lateinit var btnShapes: MaterialButton

    // Activity result launchers
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val action = result.data?.getStringExtra("ACTION")
            if (action == "CLEAR_CANVAS") {
                drawingView.clearCanvas()
                showToast("Canvas Cleared")
            }
        }
        applySettings()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadImage(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            showToast("Permissions granted")
        } else {
            showToast("Permissions required to save images")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize repositories
        settingsRepo = SettingsRepository(this)
        paletteRepo = PaletteRepository(this)
        fileOps = FileOperations(this)

        // Apply theme before super.onCreate
        applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        initializeViews()

        // Setup UI
        setupToolbar()
        setupDrawingView()
        setupTools()
        setupColorPalette()
        setupStrokeSlider()

        // Apply settings
        applySettings()

        // Request permissions if needed
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        applySettings()
        refreshPaletteBar()
    }

    private fun applyTheme() {
        val theme = settingsRepo.getTheme()
        AppCompatDelegate.setDefaultNightMode(theme)

        if (settingsRepo.isUsingMaterialYou() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }

    private fun initializeViews() {
        drawingView = findViewById(R.id.drawingView)
        toolbar = findViewById(R.id.topAppBar)
        strokeSlider = findViewById(R.id.strokeSlider)
        colorContainer = findViewById(R.id.colorContainer)
        btnCustomColor = findViewById(R.id.btnColorCustom)

        btnSelect = findViewById(R.id.btnSelect)
        btnPen = findViewById(R.id.btnPen)
        btnMarker = findViewById(R.id.btnMarker)
        btnEraser = findViewById(R.id.btnEraser)
        btnText = findViewById(R.id.btnText)
        btnSignature = findViewById(R.id.btnSignature)
        btnShapes = findViewById(R.id.btnShapes)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_undo -> {
                    drawingView.undo()
                    true
                }
                R.id.action_redo -> {
                    // Only handle redo if it exists in menu
                    drawingView.redo()
                    true
                }
                R.id.action_save -> {
                    saveImage()
                    true
                }
                R.id.action_settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDrawingView() {
        drawingView.onStateChanged = { state ->
            // Update UI based on drawing state
            updateUndoRedoButtons(state.canUndo, state.canRedo)
        }
    }

    private fun setupTools() {
        btnSelect.setOnClickListener {
            drawingView.setTool(ToolType.SELECT)
            updateToolUI(btnSelect)
        }

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

        btnText.setOnClickListener {
            showAddTextDialog()
            updateToolUI(btnText)
        }

        btnSignature.setOnClickListener {
            showSignatureDialog()
        }

        btnShapes.setOnClickListener { view ->
            showShapesMenu(view)
        }

        // Start with pen tool selected
        updateToolUI(btnPen)
    }

    private fun setupColorPalette() {
        btnCustomColor.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun setupStrokeSlider() {
        strokeSlider.addOnChangeListener { _, value, _ ->
            drawingView.setStrokeWidth(value)
        }
    }

    private fun applySettings() {
        // Background
        val background = settingsRepo.getCanvasBackground()
        drawingView.setCanvasColor(background.colorValue)

        // Screen on
        if (settingsRepo.shouldKeepScreenOn()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Grid
        drawingView.setShowGrid(settingsRepo.shouldShowGrid())

        // Accent color
        if (!settingsRepo.isUsingMaterialYou()) {
            val accentColor = settingsRepo.getAccentColor()
            toolbar.setBackgroundColor(accentColor)
            toolbar.setTitleTextColor(Color.WHITE)
            toolbar.navigationIcon?.setTint(Color.WHITE)
        }
    }

    private fun refreshPaletteBar() {
        // Remove all color buttons except the custom button
        val childCount = colorContainer.childCount
        if (childCount > 1) {
            colorContainer.removeViews(0, childCount - 1)
        }

        val colors = paletteRepo.getColors()
        for (color in colors) {
            val button = createColorButton(color)
            // Add before the custom color button
            colorContainer.addView(button, colorContainer.childCount - 1)
        }
    }

    private fun createColorButton(color: Int): MaterialButton {
        return MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(UIConstants.COLOR_BUTTON_SIZE),
                dp(UIConstants.COLOR_BUTTON_SIZE)
            ).apply {
                marginEnd = dp(UIConstants.COLOR_BUTTON_MARGIN)
            }

            backgroundTintList = ColorStateList.valueOf(color)
            stateListAnimator = null
            cornerRadius = dp(UIConstants.COLOR_BUTTON_CORNER_RADIUS)
            insetTop = 0
            insetBottom = 0
            strokeWidth = dp(UIConstants.COLOR_BUTTON_STROKE_WIDTH)
            strokeColor = ColorStateList.valueOf(Color.LTGRAY)

            setOnClickListener {
                drawingView.setColor(color)
            }

            setOnLongClickListener {
                showRemoveColorDialog(color)
                true
            }
        }
    }

    private fun updateToolUI(activeButton: MaterialButton) {
        val allButtons = listOf(
            btnSelect, btnPen, btnMarker,
            btnEraser, btnText, btnSignature, btnShapes
        )

        val unselectedBg = ContextCompat.getColor(this, R.color.md_theme_surface_variant)
        val unselectedIcon = ContextCompat.getColor(this, R.color.tool_unselected_icon)
        val selectedBg = ContextCompat.getColor(this, R.color.tool_selected_bg)
        val selectedIcon = ContextCompat.getColor(this, R.color.tool_selected_icon)

        for (button in allButtons) {
            button.backgroundTintList = ColorStateList.valueOf(unselectedBg)
            button.iconTint = ColorStateList.valueOf(unselectedIcon)
        }

        activeButton.backgroundTintList = ColorStateList.valueOf(selectedBg)
        activeButton.iconTint = ColorStateList.valueOf(selectedIcon)
    }

    private fun updateUndoRedoButtons(canUndo: Boolean, canRedo: Boolean) {
        // Safely update undo button
        toolbar.menu.findItem(R.id.action_undo)?.isEnabled = canUndo

        // Safely update redo button (only if it exists)
        toolbar.menu.findItem(R.id.action_redo)?.let { redoItem ->
            redoItem.isEnabled = canRedo
        }
    }

    private fun showShapesMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)

        popup.menu.apply {
            add(0, 1, 0, "Rectangle (Outline)")
            add(0, 2, 0, "Rectangle (Filled)")
            add(0, 3, 0, "Oval (Outline)")
            add(0, 4, 0, "Oval (Filled)")
            add(0, 5, 0, "Arrow")
            add(0, 6, 0, "Line")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> setShape(ShapeType.RECTANGLE, false)
                2 -> setShape(ShapeType.RECTANGLE, true)
                3 -> setShape(ShapeType.OVAL, false)
                4 -> setShape(ShapeType.OVAL, true)
                5 -> setShape(ShapeType.ARROW, false)
                6 -> setShape(ShapeType.LINE, false)
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

    private fun showAddTextDialog() {
        val input = EditText(this).apply {
            hint = "Enter text"
            maxLines = 3
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    if (text.length > 100) {
                        showToast("Text too long (max 100 characters)")
                    } else {
                        drawingView.addText(text)
                        drawingView.setTool(ToolType.SELECT)
                        updateToolUI(btnSelect)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSignatureDialog() {
        SignatureDialog(this) { bitmap ->
            drawingView.addSignature(bitmap)
            drawingView.setTool(ToolType.SELECT)
            updateToolUI(btnSelect)
        }.show()
    }

    private fun showColorPickerDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(UIConstants.COLOR_PICKER_PADDING),
                dp(40),
                dp(UIConstants.COLOR_PICKER_PADDING),
                dp(10)
            )
        }

        // Color preview
        val preview = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UIConstants.COLOR_PREVIEW_HEIGHT
            )
            setBackgroundColor(Color.BLACK)
        }
        layout.addView(preview)

        // Color picker
        val picker = ColorPickerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UIConstants.COLOR_PICKER_HEIGHT
            )
        }
        layout.addView(picker)

        // Hex input
        val hexInput = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
            hint = "#RRGGBB"
            textSize = 16f
            setSingleLine()
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        layout.addView(hexInput)

        // Setup color picker callbacks
        picker.onColorChanged = { color ->
            preview.setBackgroundColor(color)
            val hex = String.format("#%06X", (0xFFFFFF and color))
            if (hexInput.text.toString() != hex && hexInput.tag == null) {
                hexInput.setText(hex)
            }
        }

        picker.setColor(Color.BLACK)
        hexInput.setText("#000000")

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom Color")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                try {
                    val colorStr = hexInput.text.toString()
                    val color = Color.parseColor(colorStr)
                    paletteRepo.addColor(color)
                    refreshPaletteBar()
                    drawingView.setColor(color)
                    showToast("Color added to palette")
                } catch (e: IllegalArgumentException) {
                    showToast("Invalid color format")
                    Log.e(LogTags.MAIN_ACTIVITY, "Invalid color format", e)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveColorDialog(color: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Color")
            .setMessage("Remove this color from the palette?")
            .setPositiveButton("Remove") { _, _ ->
                paletteRepo.removeColor(color)
                refreshPaletteBar()
                showToast("Color removed")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadImage(uri: android.net.Uri) {
        lifecycleScope.launch {
            when (val result = fileOps.loadImage(uri)) {
                is LoadResult.Success -> {
                    drawingView.setImage(result.bitmap)
                    showToast("Image loaded")
                }
                is LoadResult.Error -> {
                    val message = fileOps.getLoadErrorMessage(result)
                    showToast(message)
                }
            }
        }
    }

    private fun saveImage() {
        lifecycleScope.launch {
            val bitmap = drawingView.getBitmap()
            val format = settingsRepo.getExportFormat()
            val location = settingsRepo.getSaveLocation()

            showToast("Saving...")

            when (val result = fileOps.saveImageToGallery(bitmap, format, location)) {
                is SaveResult.Success -> {
                    showToast("Saved: ${result.filePath}")
                }
                is SaveResult.Error -> {
                    val message = fileOps.getSaveErrorMessage(result)
                    showToast(message)

                    // If permission error, offer to request permissions
                    if (result is SaveResult.Error.NoPermission) {
                        showPermissionRationaleDialog()
                    }
                }
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            // Don't request on first launch, wait until user tries to save
            Log.d(LogTags.MAIN_ACTIVITY, "Permissions not granted yet")
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Permission Required")
            .setMessage("Storage permission is required to save your drawings. Would you like to grant permission?")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        permissionLauncher.launch(permissions)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
