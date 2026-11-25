package com.carstudio.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppLoader {

    suspend fun load(context: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { info ->
                runCatching {
                    val pkg = info.activityInfo.packageName
                    val label = info.loadLabel(pm).toString()
                    val bitmap = info.loadIcon(pm).toBitmap(96, 96)
                    AppEntry(pkg, label, bitmap.asImageBitmap(), dominantColor(bitmap))
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun dominantColor(bitmap: Bitmap): Long {
        val hsv = FloatArray(3)
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        var count = 0
        val step = (minOf(bitmap.width, bitmap.height) / 12).coerceAtLeast(1)

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 40) {
                    android.graphics.Color.colorToHSV(pixel, hsv)
                    if (hsv[1] > 0.25f && hsv[2] > 0.2f) {
                        red += android.graphics.Color.red(pixel)
                        green += android.graphics.Color.green(pixel)
                        blue += android.graphics.Color.blue(pixel)
                        count++
                    }
                }
                x += step
            }
            y += step
        }

        if (count == 0) return 0xFF5B8CFF

        val average = android.graphics.Color.rgb(
            (red / count).toInt(),
            (green / count).toInt(),
            (blue / count).toInt()
        )
        android.graphics.Color.colorToHSV(average, hsv)
        hsv[1] = hsv[1].coerceAtMost(0.75f)
        hsv[2] = hsv[2].coerceAtLeast(0.7f)
        return android.graphics.Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
    }
}
