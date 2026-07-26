package com.mustafashakir.peek.data.instagram

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mustafashakir.peek.data.resolver.UrlResolver
import com.mustafashakir.peek.domain.model.LoadProgress
import com.mustafashakir.peek.domain.model.LoadStage
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun interface InstagramPageLoader : UrlResolver<ParsedInstagramMedia> {
    /** Loads one subsequent comments connection page for an already resolved post. */
    suspend fun loadComments(postId: String, cursor: String): ParsedInstagramCommentsPage =
        throw UnsupportedOperationException("Comment pagination is not supported by this loader")
}

class AndroidInstagramPageLoader(
    context: Context,
    private val parser: InstagramHtmlParser = InstagramHtmlParser(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
) : InstagramPageLoader {
    private val applicationContext = context.applicationContext
    private val json = Json { isLenient = true }

    override val resolverId: String = "instagram-webview"

    // Instagram's logged-out WebView flow challenges/blocks /p/ photo posts; only reel routes load reliably.
    override fun supports(url: String): Boolean =
        WEBVIEW_SUPPORTED_PATH.containsMatchIn(Uri.parse(url).path.orEmpty())

    override suspend fun resolve(url: String): ParsedInstagramMedia {
        val listener = coroutineContext[PageLoadProgressElement]?.listener ?: LoadProgressListener {}
        return loadPage(url, listener) { webView -> webView.loadUrl(url) }
    }

    internal suspend fun loadHtmlForTesting(
        html: String,
        baseUrl: String = "https://www.instagram.com/reel/test/",
    ): ParsedInstagramMedia {
        val listener = coroutineContext[PageLoadProgressElement]?.listener ?: LoadProgressListener {}
        return loadPage(baseUrl, listener) { webView ->
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadPage(
        expectedUrl: String,
        onProgress: LoadProgressListener,
        startLoad: (WebView) -> Unit,
    ): ParsedInstagramMedia = withContext(Dispatchers.Main.immediate) {
        withTimeout(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                val webView = WebView(applicationContext)
                var pageFinished = false
                var evaluating = false
                var completed = false
                var pollAttempt = 0

                fun reportProgress(fraction: Float, stage: LoadStage) {
                    onProgress.onProgress(LoadProgress(fraction.coerceIn(0f, 1f), stage))
                }

                reportProgress(0.02f, LoadStage.Connecting)

                fun cleanup() {
                    handler.removeCallbacksAndMessages(null)
                    webView.stopLoading()
                    webView.webViewClient = WebViewClient()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                }

                fun fail(error: Throwable) {
                    if (completed) return
                    completed = true
                    cleanup()
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

                fun succeed(media: ParsedInstagramMedia) {
                    if (completed) return
                    completed = true
                    cleanup()
                    if (continuation.isActive) continuation.resume(media)
                }

                lateinit var poll: Runnable
                poll = Runnable {
                    if (completed || !pageFinished || evaluating) return@Runnable
                    evaluating = true
                    pollAttempt += 1
                    val elapsedFraction = (pollAttempt * pollIntervalMillis).toFloat() / timeoutMillis
                    reportProgress(0.5f + 0.47f * elapsedFraction.coerceAtMost(1f), LoadStage.ExtractingContent)
                    webView.evaluateJavascript(DOCUMENT_HTML_SCRIPT) { encodedHtml ->
                        evaluating = false
                        if (completed) return@evaluateJavascript
                        val html = decodeJavascriptString(encodedHtml)
                        val media = html?.let(parser::parse)
                        if (media != null) {
                            logDomForDebugging(html)
                            reportProgress(1f, LoadStage.ExtractingContent)
                            succeed(media)
                        } else {
                            handler.postDelayed(poll, pollIntervalMillis)
                        }
                    }
                }

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = DESKTOP_USER_AGENT
                    allowFileAccess = false
                    allowContentAccess = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
                webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        if (completed) return
                        reportProgress(0.05f + 0.45f * (newProgress / 100f), LoadStage.FetchingPage)
                    }
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        pageFinished = false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        if (completed) return
                        if (!isAllowedInstagramUrl(url, expectedUrl)) {
                            fail(IOException("Instagram redirected to an unsupported page"))
                            return
                        }
                        pageFinished = true
                        handler.removeCallbacks(poll)
                        handler.post(poll)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        if (!request.isForMainFrame) return false
                        val allowed = isAllowedInstagramUrl(request.url.toString(), expectedUrl)
                        if (!allowed) fail(IOException("Blocked WebView navigation to ${request.url.host}"))
                        return !allowed
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        if (request.isForMainFrame) {
                            fail(IOException("WebView load failed: ${error.description}"))
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                            fail(IOException("Instagram returned HTTP ${errorResponse.statusCode}"))
                        }
                    }

                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: RenderProcessGoneDetail,
                    ): Boolean {
                        fail(IOException("WebView renderer process exited"))
                        return true
                    }
                }

                continuation.invokeOnCancellation {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        if (!completed) {
                            completed = true
                            cleanup()
                        }
                    } else {
                        handler.post {
                            if (!completed) {
                                completed = true
                                cleanup()
                            }
                        }
                    }
                }

                startLoad(webView)
            }
        }
    }

    private fun decodeJavascriptString(value: String): String? = runCatching {
        json.parseToJsonElement(value).jsonPrimitive.contentOrNull
    }.getOrNull()

    private fun logDomForDebugging(html: String) {
        val debuggable = applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) return
        Log.d(LOG_TAG, "DOM_BEGIN length=${html.length}")
        html.chunked(LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
            Log.d(LOG_TAG, "DOM_CHUNK_${index.toString().padStart(4, '0')} $chunk")
        }
        Log.d(LOG_TAG, "DOM_END")
    }

    private fun isAllowedInstagramUrl(candidate: String, expected: String): Boolean {
        if (candidate == "about:blank") return false
        val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return false
        if (uri.scheme != "https") return false
        if (uri.host !in INSTAGRAM_HOSTS) return false
        if (uri.path.orEmpty().startsWith("/accounts/")) return false

        val expectedPath = Uri.parse(expected).path.orEmpty().trimEnd('/')
        val candidatePath = uri.path.orEmpty().trimEnd('/')
        return candidatePath == expectedPath
    }

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
        const val DEFAULT_POLL_INTERVAL_MILLIS = 500L
        private const val DOCUMENT_HTML_SCRIPT =
            "(function() { return document.documentElement ? document.documentElement.innerHTML : null; })();"
        private const val LOG_TAG = "PeekInstagramWebView"
        private const val LOG_CHUNK_SIZE = 3_000
        private val INSTAGRAM_HOSTS = setOf("instagram.com", "www.instagram.com")
        private val WEBVIEW_SUPPORTED_PATH = Regex("/(reel|reels)/")
    }
}
