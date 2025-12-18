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
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnRequestPermission: MaterialButton
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val prefs = getSharedPreferences("MarkupSettings", MODE_PRIVATE)

        findViewById<MaterialToolbar>(R.id.settingsToolbar).setNavigationOnClickListener { finish() }

        // Themes
        val radioGroupTheme = findViewById<RadioGroup>(R.id.radioGroupTheme)
        when (prefs.getInt("APP_THEME", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> findViewById<RadioButton>(R.id.radioThemeSystem).isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> findViewById<RadioButton>(R.id.radioThemeLight).isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> findViewById<RadioButton>(R.id.radioThemeDark).isChecked = true
        }
        radioGroupTheme.setOnCheckedChangeListener { _, id ->
            val mode = when(id) { R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO; R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
            prefs.edit { putInt("APP_THEME", mode) }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Material You / Accent
        val switchMatYou = findViewById<MaterialSwitch>(R.id.switchMaterialYou)
        val accentContainer = findViewById<LinearLayout>(R.id.accentColorContainer)
        if (Build.VERSION.SDK_INT < 31) { switchMatYou.isEnabled = false; switchMatYou.text = "Material You (Android 12+ Only)" }
        switchMatYou.isChecked = prefs.getBoolean("USE_MATERIAL_YOU", false)
        switchMatYou.setOnCheckedChangeListener { _, c -> prefs.edit { putBoolean("USE_MATERIAL_YOU", c) }; Toast.makeText(this, "Restart to apply", Toast.LENGTH_SHORT).show() }

        val colors = listOf("#4F378B", "#B3261E", "#2196F3", "#00796B", "#FF9800", "#1C1B1F")
        for (hex in colors) {
            val c = hex.toColorInt()
            val btn = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
                backgroundTintList = ColorStateList.valueOf(c); cornerRadius = 50
                setOnClickListener { if (!switchMatYou.isChecked) { prefs.edit { putInt("ACCENT_COLOR", c) } } }
            }
            accentContainer.addView(btn)
        }

        // Canvas BG
        val rgBg = findViewById<RadioGroup>(R.id.radioGroupBackground)
        when(prefs.getInt("CANVAS_BG", 0)) {
            0 -> findViewById<RadioButton>(R.id.radioBgWhite).isChecked = true
            1 -> findViewById<RadioButton>(R.id.radioBgPaper).isChecked = true
            2 -> findViewById<RadioButton>(R.id.radioBgDark).isChecked = true
        }
        rgBg.setOnCheckedChangeListener { _, id ->
            val v = when(id) { R.id.radioBgWhite -> 0; R.id.radioBgPaper -> 1; else -> 2 }
            prefs.edit { putInt("CANVAS_BG", v) }
        }

        // Grid
        val swGrid = findViewById<MaterialSwitch>(R.id.switchGridLines)
        swGrid.isChecked = prefs.getBoolean("SHOW_GRID", false)
        swGrid.setOnCheckedChangeListener { _, c -> prefs.edit { putBoolean("SHOW_GRID", c) } }

        // Screen On
        val swScreen = findViewById<MaterialSwitch>(R.id.switchKeepScreenOn)
        swScreen.isChecked = prefs.getBoolean("KEEP_SCREEN_ON", false)
        swScreen.setOnCheckedChangeListener { _, c -> prefs.edit { putBoolean("KEEP_SCREEN_ON", c) } }

        // Storage
        val rgStore = findViewById<RadioGroup>(R.id.radioGroupStorage)
        if (prefs.getString("SAVE_LOCATION", "PICTURES") == "DOWNLOADS") findViewById<RadioButton>(R.id.radioSaveDownloads).isChecked = true else findViewById<RadioButton>(R.id.radioSavePictures).isChecked = true
        rgStore.setOnCheckedChangeListener { _, id -> prefs.edit { putString("SAVE_LOCATION", if (id == R.id.radioSaveDownloads) "DOWNLOADS" else "PICTURES") } }

        // Format
        val rgFmt = findViewById<RadioGroup>(R.id.radioGroupFormat)
        if (prefs.getString("EXPORT_FORMAT", "PNG") == "JPEG") findViewById<RadioButton>(R.id.radioJpeg).isChecked = true else findViewById<RadioButton>(R.id.radioPng).isChecked = true
        rgFmt.setOnCheckedChangeListener { _, id -> prefs.edit { putString("EXPORT_FORMAT", if (id == R.id.radioJpeg) "JPEG" else "PNG") } }

        // Permissions
        btnRequestPermission = findViewById(R.id.btnRequestPermission)
        btnRequestPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            else requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
        checkPermissions()

        // Palette
        findViewById<MaterialButton>(R.id.btnCustomizePalette).setOnClickListener { startActivity(Intent(this, PaletteActivity::class.java)) }

        // Clear
        findViewById<MaterialButton>(R.id.btnClearCanvas).setOnClickListener { setResult(RESULT_OK, Intent().apply { putExtra("ACTION", "CLEAR_CANVAS") }); finish() }

        // About
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            findViewById<TextView>(R.id.tvVersionInfo).text = "Version ${pInfo.versionName}"
            findViewById<TextView>(R.id.tvBuildNumber).text = "Build ${if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else pInfo.versionCode}"
        } catch (e: Exception) {}
    }

    private fun checkPermissions() {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            btnRequestPermission.isEnabled = false; btnRequestPermission.text = "Granted"
        }
    }
}