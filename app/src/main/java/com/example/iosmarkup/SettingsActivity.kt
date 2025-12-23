package com.example.iosmarkup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Improved SettingsActivity with proper theming and contrast
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnRequestPermission: MaterialButton
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { 
        checkPermissions() 
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        settingsRepo = SettingsRepository(this)
        applyTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        toolbar = findViewById(R.id.settingsToolbar)
        setupToolbar()
        setupThemeSettings()
        setupMaterialYouSettings()
        setupCanvasSettings()
        setupGridSettings()
        setupScreenSettings()
        setupStorageSettings()
        setupExportSettings()
        setupPermissions()
        setupPaletteButton()
        setupClearButton()
        setupAboutInfo()
        
        // Apply toolbar theming
        applyToolbarTheming()
    }
    
    private fun applyTheme() {
        settingsRepo.applyTheme(this)
    }
    
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { 
            finish() 
        }
    }
    
    private fun applyToolbarTheming() {
        if (settingsRepo.isUsingMaterialYou()) {
            // Use Material You colors
            val primaryColor = ContextCompat.getColor(this, R.color.md_theme_primary)
            val onPrimaryColor = ContextCompat.getColor(this, R.color.md_theme_onPrimary)
            
            toolbar.setBackgroundColor(primaryColor)
            toolbar.setTitleTextColor(onPrimaryColor)
            toolbar.navigationIcon?.setTint(onPrimaryColor)
            
            // Update status bar color to match
            window.statusBarColor = primaryColor
        } else {
            // Use custom accent color
            val accentColor = settingsRepo.getAccentColor()
            val textColor = getContrastingTextColor(accentColor)
            
            toolbar.setBackgroundColor(accentColor)
            toolbar.setTitleTextColor(textColor)
            toolbar.navigationIcon?.setTint(textColor)
            
            // Update status bar color to match
            window.statusBarColor = accentColor
        }
    }
    
    private fun getContrastingTextColor(backgroundColor: Int): Int {
        // Calculate luminance
        val red = Color.red(backgroundColor)
        val green = Color.green(backgroundColor)
        val blue = Color.blue(backgroundColor)
        
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
    
    private fun setupThemeSettings() {
        val radioGroupTheme = findViewById<RadioGroup>(R.id.radioGroupTheme)
        
        when (settingsRepo.getTheme()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 
                findViewById<RadioButton>(R.id.radioThemeSystem).isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> 
                findViewById<RadioButton>(R.id.radioThemeLight).isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> 
                findViewById<RadioButton>(R.id.radioThemeDark).isChecked = true
        }
        
        radioGroupTheme.setOnCheckedChangeListener { _, id ->
            val mode = when (id) {
                R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            settingsRepo.setTheme(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
    
    private fun setupMaterialYouSettings() {
        val switchMatYou = findViewById<MaterialSwitch>(R.id.switchMaterialYou)
        val accentContainer = findViewById<LinearLayout>(R.id.accentColorContainer)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            switchMatYou.isEnabled = false
            switchMatYou.text = "Material You (Android 12+ Only)"
        }
        
        switchMatYou.isChecked = settingsRepo.isUsingMaterialYou()
        
        switchMatYou.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setUseMaterialYou(isChecked)
            
            // Update accent color visibility
            accentContainer.alpha = if (isChecked) 0.5f else 1.0f
            
            // Show restart message
            com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "Restart app to apply Material You",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
        }
        
        // Setup accent color buttons
        val currentAccent = settingsRepo.getAccentColor()
        
        for (hex in DefaultColors.ACCENT_COLORS) {
            val color = Color.parseColor(hex)
            val button = createAccentColorButton(color, color == currentAccent)
            accentContainer.addView(button)
        }
        
        // Disable accent colors if Material You is enabled
        accentContainer.alpha = if (switchMatYou.isChecked) 0.5f else 1.0f
    }
    
    private fun createAccentColorButton(color: Int, isSelected: Boolean): MaterialButton {
        return MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(UIConstants.ACCENT_BUTTON_SIZE),
                dp(UIConstants.ACCENT_BUTTON_SIZE)
            ).apply {
                marginEnd = dp(UIConstants.ACCENT_BUTTON_MARGIN)
            }
            
            backgroundTintList = ColorStateList.valueOf(color)
            cornerRadius = dp(UIConstants.ACCENT_BUTTON_CORNER_RADIUS)
            
            // Show selection indicator
            if (isSelected) {
                strokeWidth = dp(4)
                strokeColor = ColorStateList.valueOf(Color.WHITE)
            }
            
            setOnClickListener {
                if (!settingsRepo.isUsingMaterialYou()) {
                    settingsRepo.setAccentColor(color)
                    com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Accent color updated",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Refresh accent buttons
                    refreshAccentButtons()
                    
                    // Update toolbar immediately
                    applyToolbarTheming()
                }
            }
        }
    }
    
    private fun refreshAccentButtons() {
        val accentContainer = findViewById<LinearLayout>(R.id.accentColorContainer)
        accentContainer.removeAllViews()
        
        val currentAccent = settingsRepo.getAccentColor()
        
        for (hex in DefaultColors.ACCENT_COLORS) {
            val color = Color.parseColor(hex)
            val button = createAccentColorButton(color, color == currentAccent)
            accentContainer.addView(button)
        }
    }
    
    private fun setupCanvasSettings() {
        val radioGroupBg = findViewById<RadioGroup>(R.id.radioGroupBackground)
        
        when (settingsRepo.getCanvasBackground()) {
            CanvasBackground.WHITE -> 
                findViewById<RadioButton>(R.id.radioBgWhite).isChecked = true
            CanvasBackground.PAPER -> 
                findViewById<RadioButton>(R.id.radioBgPaper).isChecked = true
            CanvasBackground.DARK -> 
                findViewById<RadioButton>(R.id.radioBgDark).isChecked = true
        }
        
        radioGroupBg.setOnCheckedChangeListener { _, id ->
            val background = when (id) {
                R.id.radioBgWhite -> CanvasBackground.WHITE
                R.id.radioBgPaper -> CanvasBackground.PAPER
                else -> CanvasBackground.DARK
            }
            settingsRepo.setCanvasBackground(background)
        }
    }
    
    private fun setupGridSettings() {
        val switchGrid = findViewById<MaterialSwitch>(R.id.switchGridLines)
        switchGrid.isChecked = settingsRepo.shouldShowGrid()
        switchGrid.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setShowGrid(isChecked)
        }
    }
    
    private fun setupScreenSettings() {
        val switchScreen = findViewById<MaterialSwitch>(R.id.switchKeepScreenOn)
        switchScreen.isChecked = settingsRepo.shouldKeepScreenOn()
        switchScreen.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setKeepScreenOn(isChecked)
        }
    }
    
    private fun setupStorageSettings() {
        val radioGroupStorage = findViewById<RadioGroup>(R.id.radioGroupStorage)
        
        when (settingsRepo.getSaveLocation()) {
            SaveLocation.PICTURES -> 
                findViewById<RadioButton>(R.id.radioSavePictures).isChecked = true
            SaveLocation.DOWNLOADS -> 
                findViewById<RadioButton>(R.id.radioSaveDownloads).isChecked = true
        }
        
        radioGroupStorage.setOnCheckedChangeListener { _, id ->
            val location = when (id) {
                R.id.radioSaveDownloads -> SaveLocation.DOWNLOADS
                else -> SaveLocation.PICTURES
            }
            settingsRepo.setSaveLocation(location)
        }
    }
    
    private fun setupExportSettings() {
        val radioGroupFormat = findViewById<RadioGroup>(R.id.radioGroupFormat)
        
        when (settingsRepo.getExportFormat()) {
            ExportFormat.PNG -> 
                findViewById<RadioButton>(R.id.radioPng).isChecked = true
            ExportFormat.JPEG -> 
                findViewById<RadioButton>(R.id.radioJpeg).isChecked = true
        }
        
        radioGroupFormat.setOnCheckedChangeListener { _, id ->
            val format = when (id) {
                R.id.radioJpeg -> ExportFormat.JPEG
                else -> ExportFormat.PNG
            }
            settingsRepo.setExportFormat(format)
        }
    }
    
    private fun setupPermissions() {
        btnRequestPermission = findViewById(R.id.btnRequestPermission)
        btnRequestPermission.setOnClickListener {
            requestStoragePermissions()
        }
        checkPermissions()
    }
    
    private fun requestStoragePermissions() {
        requestPermissionLauncher.launch(PermissionHelper.getStoragePermissions())
    }

    private fun checkPermissions() {
        val isGranted = PermissionHelper.hasPrimaryStoragePermission(this)
        btnRequestPermission.isEnabled = !isGranted
        btnRequestPermission.text = if (isGranted) "Granted" else "Grant Permission"
    }
    
    private fun setupPaletteButton() {
        findViewById<MaterialButton>(R.id.btnCustomizePalette).setOnClickListener {
            startActivity(Intent(this, PaletteActivity::class.java))
        }
    }
    
    private fun setupClearButton() {
        findViewById<MaterialButton>(R.id.btnClearCanvas).setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra("ACTION", "CLEAR_CANVAS")
            })
            finish()
        }
    }
    
    private fun setupAboutInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            findViewById<TextView>(R.id.tvVersionInfo).text = 
                "Version ${packageInfo.versionName}"
            
            val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            findViewById<TextView>(R.id.tvBuildNumber).text = "Build $buildNumber"
        } catch (e: Exception) {
            // Ignore if we can't get package info
        }
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
