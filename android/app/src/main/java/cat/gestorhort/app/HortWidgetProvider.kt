package cat.gestorhort.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class HortWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val summary = prefs.getString(KEY_SUMMARY, "Consulteu el plànol i el calendari de cultius.") ?: ""
        val badge = prefs.getString(KEY_BADGE, "🌱 Hort actiu") ?: "🌱 Hort actiu"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.hort_widget)
            views.setTextViewText(R.id.widget_content, summary)
            views.setTextViewText(R.id.widget_badge, badge)

            // Click on header opens the main app
            views.setOnClickPendingIntent(
                R.id.widget_title,
                createLaunchIntent(context, "https://appassets.androidplatform.net/assets/index.html")
            )

            // Shortcut button: Plànol
            views.setOnClickPendingIntent(
                R.id.widget_btn_grid,
                createLaunchIntent(context, "https://appassets.androidplatform.net/assets/index.html?view=grid")
            )

            // Shortcut button: Calendari
            views.setOnClickPendingIntent(
                R.id.widget_btn_calendar,
                createLaunchIntent(context, "https://appassets.androidplatform.net/assets/index.html?view=calendar")
            )

            // Shortcut button: Planter
            views.setOnClickPendingIntent(
                R.id.widget_btn_plants,
                createLaunchIntent(context, "https://appassets.androidplatform.net/assets/index.html?view=plants")
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createLaunchIntent(context: Context, url: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val PREFS_NAME = "cat.gestorhort.widget_prefs"
        const val KEY_SUMMARY = "summary_text"
        const val KEY_BADGE = "badge_text"

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
