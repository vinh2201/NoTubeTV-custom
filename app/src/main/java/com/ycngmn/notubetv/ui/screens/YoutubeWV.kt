package com.ycngmn.notubetv.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.ycngmn.notubetv.R
import com.ycngmn.notubetv.ui.components.UpdateDialog
import com.ycngmn.notubetv.utils.ExitBridge
import com.ycngmn.notubetv.utils.NetworkBridge
import com.ycngmn.notubetv.utils.ReleaseData
import com.ycngmn.notubetv.utils.fetchScripts
import com.ycngmn.notubetv.utils.getUpdate
import com.ycngmn.notubetv.utils.permissionHandlerChrome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun YoutubeWV() {

    val context = LocalContext.current
    val config = LocalConfiguration.current

    val state = rememberWebViewState("https://www.youtube.com/tv")
    val navigator = rememberWebViewNavigator()

    val scriptData = remember { mutableStateOf<String?>(null) }
    val updateData = remember { mutableStateOf<ReleaseData?>(null) }

    val exitTrigger = remember { mutableStateOf(false) }

    // Translate native back-presses to 'escape' button press
    BackHandler {
        navigator.evaluateJavaScript(
            context.resources.openRawResource(R.raw.back_bridge)
                .bufferedReader().use { it.readText() }
        )
    }

    // Fetch scripts and updates at launch from Github
    LaunchedEffect(Unit) {
        val fetchedScripts = withContext(Dispatchers.IO) { fetchScripts() }
        scriptData.value = fetchedScripts

        withContext(Dispatchers.IO) {
            getUpdate(context, navigator) { update ->
                if (update != null) updateData.value = update
            }
        }
    }
    // Apply the fetched scripts to the webview, once the loading is complete.
    LaunchedEffect(scriptData.value, state.loadingState) {
        val script = scriptData.value
        if (script != null && state.loadingState == LoadingState.Finished)
            navigator.evaluateJavaScript(script)
    }

    // If any update found, show the dialog.
    if (updateData.value != null) UpdateDialog(updateData.value!!, navigator)
    // If exit button is pressed, 'finish the activity' aka 'exit the app'.
    if (exitTrigger.value) (context as Activity).finish()


    WebView(
        modifier = Modifier.size(
            config.screenWidthDp.dp,
            config.screenHeightDp.dp
        ),
        state = state,
        navigator = navigator,
        platformWebViewParams = permissionHandlerChrome(context),
        captureBackPresses = false,
        onCreated = { webView ->
            // Set up cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            cookieManager.flush()

            state.webSettings.apply {
                // This user agent provides native like experience .
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

                // Bridges the exit button click on the website to handle it natively.
                addJavascriptInterface(ExitBridge(exitTrigger), "ExitBridge")

                /*
                Youtube's content security policy doesn't allow calling fetch on
                3rd party websites (eg. SponsorBlock api). This bridge counters that
                handling the requests on the native side. */
                addJavascriptInterface(NetworkBridge(navigator), "NetworkBridge")

                // Enables hardware acceleration
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                // Set the zoom to 35% to fit the screen. Side-effect of viewport spoofing.
                setInitialScale(35)

                // Hide scrollbars
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
            }
        }
    )
}