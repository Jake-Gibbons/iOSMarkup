package com.example.iosmarkup
import android.content.Context
import android.graphics.Color
import org.json.JSONArray

object PaletteManager {
    fun getColors(ctx: Context): MutableList<Int> {
        val s = ctx.getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE).getString("PALETTE", null) ?: return mutableListOf(Color.BLACK, Color.RED, Color.BLUE, Color.GREEN)
        val l = mutableListOf<Int>(); val a = JSONArray(s)
        for (i in 0 until a.length()) l.add(a.getInt(i))
        return l
    }
    fun saveColors(ctx: Context, l: List<Int>) {
        val a = JSONArray(); l.forEach { a.put(it) }
        ctx.getSharedPreferences("MarkupSettings", Context.MODE_PRIVATE).edit().putString("PALETTE", a.toString()).apply()
    }
}