package com.ycngmn.notubetv.ui.screens

import android.app.Activity
import android.view.View
import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.ycngmn.notubetv.R
import com.ycngmn.notubetv.utils.ExitBridge
import com.ycngmn.notubetv.utils.fetchScripts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun YoutubeWV() {

    val context = LocalContext.current
    val scripts = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            scripts.value = fetchScripts()
        }
    }

    val state = rememberWebViewState("https://www.youtube.com/tv")
    val navigator = rememberWebViewNavigator()

    val exitTrigger = remember { mutableStateOf(false) }

    BackHandler {
        navigator.evaluateJavaScript(
            context.resources.openRawResource(R.raw.back_bridge)
                .bufferedReader().use { it.readText() }
        )
    }

    LaunchedEffect(scripts.value) {
        if (scripts.value.isNotEmpty())
            navigator.evaluateJavaScript(scripts.value)
    }

    if (exitTrigger.value) (context as Activity).finish()



    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    WebView(
        captureBackPresses = false,
        modifier = Modifier
            .size(
                with(density) { screenWidthPx.toDp() },
                with(density) { screenHeightPx.toDp() }
            ),
        state = state,
        navigator = navigator,
        onCreated = { webView ->
            // Set up cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            cookieManager.flush()

            state.webSettings.apply {
                // a random short user agent to enforce leanback UI.
                customUserAgentString = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/2.2 Chrome/63.0.3239.84 TV Safari/537.36"
                isJavaScriptEnabled = true

                androidWebSettings.apply {
                    //isDebugInspectorInfoEnabled = true
                    useWideViewPort = true
                    domStorageEnabled = true
                    hideDefaultVideoPoster = true
                    mediaPlaybackRequiresUserGesture = false
                }
            }

            webView.apply {

                addJavascriptInterface(ExitBridge(exitTrigger), "ExitBridge")

                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setInitialScale(25)

                // Hide scrollbars
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.setSupportZoom(true)
                settings.loadWithOverviewMode = true
            }
        }
    )
}