package cat.gestorhort.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import org.json.JSONObject

class HortWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val widgetData = loadWidgetData(context)
        val pendingIntent = createSummaryIntent(context)

        val smallViews = buildSmallViews(context, widgetData, pendingIntent)
        val mediumViews = buildMediumViews(context, widgetData, pendingIntent)
        val largeViews = buildLargeViews(context, widgetData, pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val viewMapping = mapOf(
                SizeF(110f, 110f) to smallViews,
                SizeF(240f, 110f) to mediumViews,
                SizeF(240f, 200f) to largeViews
            )
            appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(viewMapping))
        } else {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 120)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120)

            val chosenViews = when {
                minWidth >= 240 && minHeight >= 180 -> largeViews
                minWidth >= 240 -> mediumViews
                else -> smallViews
            }
            appWidgetManager.updateAppWidget(appWidgetId, chosenViews)
        }
    }

    // Small (2x2): Expressive Rotated Stadium Pill
    private fun buildSmallViews(context: Context, data: WidgetData, pendingIntent: PendingIntent): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_small_square)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        val pillBitmap = drawRotatedPillBitmap(context, data.total)
        views.setImageViewBitmap(R.id.widget_small_pill_img, pillBitmap)
        return views
    }

    // Medium (3x2 or 4x2): Left Hero Pill + Top 3 Categories (Count only, no %)
    private fun buildMediumViews(context: Context, data: WidgetData, pendingIntent: PendingIntent): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_medium_wide)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        views.setTextViewText(R.id.widget_med_total_count, data.total.toString())

        val rowIds = listOf(
            Triple(R.id.cat_row_1, R.id.cat_icon_1, Triple(R.id.cat_label_1, R.id.cat_progress_1, R.id.cat_count_1)),
            Triple(R.id.cat_row_2, R.id.cat_icon_2, Triple(R.id.cat_label_2, R.id.cat_progress_2, R.id.cat_count_2)),
            Triple(R.id.cat_row_3, R.id.cat_icon_3, Triple(R.id.cat_label_3, R.id.cat_progress_3, R.id.cat_count_3))
        )

        for (i in 0 until 3) {
            val (rowId, iconId, textIds) = rowIds[i]
            val (labelId, progressId, countId) = textIds

            if (i < data.categories.size) {
                val cat = data.categories[i]
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(labelId, cat.label)
                views.setImageViewResource(iconId, getCategoryDrawable(cat.icon))
                views.setProgressBar(progressId, 100, cat.pct, false)
                views.setTextViewText(countId, cat.count.toString()) // Count only, as requested
            } else {
                views.setViewVisibility(rowId, View.GONE)
            }
        }

        return views
    }

    // Large (4x3 or 4x4): Hero Pill + Top 5 Categories with % breakdown
    private fun buildLargeViews(context: Context, data: WidgetData, pendingIntent: PendingIntent): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_large)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        views.setTextViewText(R.id.widget_large_total_count, data.total.toString())

        val rowIds = listOf(
            Triple(R.id.large_cat_row_1, R.id.large_cat_icon_1, Triple(R.id.large_cat_label_1, R.id.large_cat_progress_1, R.id.large_cat_count_1)),
            Triple(R.id.large_cat_row_2, R.id.large_cat_icon_2, Triple(R.id.large_cat_label_2, R.id.large_cat_progress_2, R.id.large_cat_count_2)),
            Triple(R.id.large_cat_row_3, R.id.large_cat_icon_3, Triple(R.id.large_cat_label_3, R.id.large_cat_progress_3, R.id.large_cat_count_3)),
            Triple(R.id.large_cat_row_4, R.id.large_cat_icon_4, Triple(R.id.large_cat_label_4, R.id.large_cat_progress_4, R.id.large_cat_count_4)),
            Triple(R.id.large_cat_row_5, R.id.large_cat_icon_5, Triple(R.id.large_cat_label_5, R.id.large_cat_progress_5, R.id.large_cat_count_5))
        )

        for (i in 0 until 5) {
            val (rowId, iconId, textIds) = rowIds[i]
            val (labelId, progressId, countId) = textIds

            if (i < data.categories.size) {
                val cat = data.categories[i]
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(labelId, cat.label)
                views.setImageViewResource(iconId, getCategoryDrawable(cat.icon))
                views.setProgressBar(progressId, 100, cat.pct, false)
                views.setTextViewText(countId, "${cat.count} (${cat.pct}%)")
            } else {
                views.setViewVisibility(rowId, View.GONE)
            }
        }

        return views
    }

    // Draws the iconic Material 3 Expressive -35° rotated stadium pill with plant count & logo
    private fun drawRotatedPillBitmap(context: Context, total: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (240 * density).toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = sizePx / 2f
        val cy = sizePx / 2f

        canvas.save()
        // Rotate at -35 degrees around the center
        canvas.rotate(-35f, cx, cy)

        val pillWidth = 160f * density
        val pillHeight = 88f * density
        val pillRadius = pillHeight / 2f

        val pillRect = RectF(
            cx - pillWidth / 2f,
            cy - pillHeight / 2f,
            cx + pillWidth / 2f,
            cy + pillHeight / 2f
        )

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.m3_widget_pill_bg)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillPaint)

        // Draw large bold number
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.m3_widget_pill_text)
            textSize = 38f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val totalText = total.toString()
        val textBounds = Rect()
        textPaint.getTextBounds(totalText, 0, totalText.length, textBounds)

        // Draw number slightly above center of pill
        val textY = cy - 2f * density
        val textX = cx - 14f * density
        canvas.drawText(totalText, textX, textY, textPaint)

        // Draw app logo inside the pill next to/below the number
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_shortcut_logo)
        if (logoBitmap != null) {
            val logoSize = (28f * density).toInt()
            val logoX = (cx + (pillWidth / 2f) - logoSize - 18f * density).toInt()
            val logoY = (cy + (pillHeight / 2f) - logoSize - 12f * density).toInt()
            val destRect = Rect(logoX, logoY, logoX + logoSize, logoY + logoSize)
            canvas.drawBitmap(logoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }

        canvas.restore()
        return bitmap
    }

    private fun getCategoryDrawable(icon: String): Int {
        return when (icon) {
            "eco" -> R.drawable.ic_cat_eco
            "nutrition" -> R.drawable.ic_cat_nutrition
            "grass" -> R.drawable.ic_cat_grass
            "grain" -> R.drawable.ic_cat_grain
            "local_florist" -> R.drawable.ic_cat_local_florist
            "spa" -> R.drawable.ic_cat_spa
            else -> R.drawable.ic_cat_eco
        }
    }

    private fun createSummaryIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("https://appassets.androidplatform.net/assets/index.html?view=summary")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            "widget_summary_click".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadWidgetData(context: Context): WidgetData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_WIDGET_DATA_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val obj = JSONObject(jsonStr)
                val total = obj.optInt("total", 0)
                val cats = mutableListOf<CategoryData>()
                val arr = obj.optJSONArray("categories")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        cats.add(
                            CategoryData(
                                id = c.optString("id", "none"),
                                label = c.optString("label", "-"),
                                icon = c.optString("icon", "eco"),
                                count = c.optInt("count", 0),
                                pct = c.optInt("pct", 0)
                            )
                        )
                    }
                }
                return WidgetData(total, cats)
            } catch (e: Exception) {
                // fall through to defaults
            }
        }

        // Default initial preview data
        return WidgetData(
            total = 24,
            categories = listOf(
                CategoryData("fulla", "Fulla", "eco", 12, 50),
                CategoryData("fruit", "Fruit", "nutrition", 8, 33),
                CategoryData("arrel", "Arrel i bulb", "grass", 4, 17)
            )
        )
    }

    data class WidgetData(val total: Int, val categories: List<CategoryData>)
    data class CategoryData(val id: String, val label: String, val icon: String, val count: Int, val pct: Int)

    companion object {
        const val PREFS_NAME = "cat.gestorhort.widget_prefs"
        const val KEY_WIDGET_DATA_JSON = "widget_data_json"

        fun updateWidgets(context: Context) {
            val intent = Intent(context, HortWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, HortWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
