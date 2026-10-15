package cat.gestorhort.app

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface

class WebAppInterface(private val context: Context, private val onSaveRequest: (filename: String, mimeType: String, content: String) -> Unit) {

    @JavascriptInterface
    fun isNativeApp(): Boolean = true

    @JavascriptInterface
    fun updateWidget(summaryText: String, badgeText: String) {
        val prefs = context.getSharedPreferences(HortWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(HortWidgetProvider.KEY_SUMMARY, summaryText)
            .putString(HortWidgetProvider.KEY_BADGE, badgeText)
            .apply()
        HortWidgetProvider.updateWidgets(context)
    }

    @JavascriptInterface
    fun shareText(title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    @JavascriptInterface
    fun exportDataNative(filename: String, mimeType: String, content: String) {
        onSaveRequest(filename, mimeType, content)
    }
}
