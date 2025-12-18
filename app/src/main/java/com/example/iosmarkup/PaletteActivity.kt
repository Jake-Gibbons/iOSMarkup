package com.example.iosmarkup

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Collections

class PaletteActivity : AppCompatActivity() {
    private lateinit var adapter: ColorAdapter
    private var colorList = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palette)
        colorList = PaletteManager.getColors(this)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = ColorAdapter(colorList)
        rv.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, s: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                Collections.swap(colorList, s.adapterPosition, t.adapterPosition); adapter.notifyItemMoved(s.adapterPosition, t.adapterPosition); return true
            }
            override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {
                colorList.removeAt(v.adapterPosition); adapter.notifyItemRemoved(v.adapterPosition); PaletteManager.saveColors(this@PaletteActivity, colorList)
            }
            override fun clearView(r: RecyclerView, v: RecyclerView.ViewHolder) { super.clearView(r, v); PaletteManager.saveColors(this@PaletteActivity, colorList) }
        }).attachToRecyclerView(rv)

        findViewById<FloatingActionButton>(R.id.fabAddColor).setOnClickListener {
            // Simply re-use logic or prompt simple add for brevity
            val picker = ColorPickerView(this)
            var c = Color.BLACK
            picker.onColorChanged = { c = it }
            AlertDialog.Builder(this).setView(picker).setPositiveButton("Add") { _,_ -> colorList.add(c); adapter.notifyItemInserted(colorList.size-1); PaletteManager.saveColors(this, colorList) }.show()
        }
    }

    inner class ColorAdapter(private val list: List<Int>) : RecyclerView.Adapter<ColorAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_color, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            h.itemView.findViewById<View>(R.id.colorPreview).background = GradientDrawable().apply { setColor(list[i]); setStroke(2, Color.LTGRAY); shape = GradientDrawable.OVAL }
            h.itemView.findViewById<TextView>(R.id.tvHexCode).text = String.format("#%06X", (0xFFFFFF and list[i]))
        }
        override fun getItemCount() = list.size
    }
}