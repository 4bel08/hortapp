package cat.gestorhort.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Native file picker for JSON restore (<input type="file">)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge support with custom status/navigation bar theming
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val rootLayout = findViewById<FrameLayout>(R.id.root_layout)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        // Light system bar icons in dark mode, dark system bar icons in light mode
        insetsController.isAppearanceLightStatusBars = !isDarkMode
        insetsController.isAppearanceLightNavigationBars = !isDarkMode

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        handleIntent(intent)

        // Android back button / gesture handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val targetUrl = intent?.data?.toString() ?: "https://appassets.androidplatform.net/assets/index.html"
        webView.loadUrl(targetUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.web_view)

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Bridge to native features and home screen widget
        webView.addJavascriptInterface(
            WebAppInterface(this) { filename, mimeType, content ->
                shareExportedFile(filename, mimeType, content)
            },
            "AndroidBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (url.startsWith("https://appassets.androidplatform.net/")) {
                    return false
                }
                // Open external links (e.g. Firebase or outside docs) in external browser
                return try {
                    val externalIntent = Intent(Intent.ACTION_VIEW, request.url)
                    startActivity(externalIntent)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val acceptTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }
                val mimeType = if (!acceptTypes.isNullOrEmpty()) acceptTypes.first() else "*/*"

                try {
                    filePickerLauncher.launch(mimeType)
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    return false
                }
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Handle download anchors (e.g. JSON export and CSV export)
        webView.setDownloadListener(DownloadListener { url, _, contentDisposition, mimeType, _ ->
            if (url.startsWith("data:")) {
                handleDataUrlDownload(url)
            } else {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No s'ha pogut descarregar el fitxer", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun handleDataUrlDownload(dataUrl: String) {
        try {
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex == -1) return

            val header = dataUrl.substring(0, commaIndex)
            val dataPart = dataUrl.substring(commaIndex + 1)
            val isBase64 = header.contains(";base64")
            val mimeType = header.removePrefix("data:").split(";").firstOrNull() ?: "text/plain"

            val decodedContent = if (isBase64) {
                String(Base64.decode(dataPart, Base64.DEFAULT), StandardCharsets.UTF_8)
            } else {
                URLDecoder.decode(dataPart, "UTF-8")
            }

            val filename = if (mimeType.contains("json")) "copia_hort.json" else "historial_hort.csv"
            shareExportedFile(filename, mimeType, decodedContent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error en exportar dades", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareExportedFile(filename: String, mimeType: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, filename)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, "Desar o compartir: $filename")
        startActivity(chooser)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
