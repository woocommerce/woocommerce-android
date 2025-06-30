package com.woocommerce.android.ui.themes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.Screen
import com.woocommerce.android.ui.compose.Screen.ScreenType
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.compose.component.web.WCWebViewSettings
import com.woocommerce.android.ui.compose.component.web.WebViewProgressIndicator
import com.woocommerce.android.ui.compose.rememberScreen
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType
import org.wordpress.android.fluxc.network.UserAgent

@Suppress("ComplexMethod")
@Composable
fun ThemePreviewWebView(
    url: String,
    userAgent: UserAgent,
    modifier: Modifier = Modifier,
    authenticator: WebViewAuthenticator? = null,
    previewType: PreviewType
) {
    val screen = rememberScreen()
    var webViewSettings by remember { mutableStateOf(WCWebViewSettings()) }

    LaunchedEffect(previewType) {
        webViewSettings = webViewSettings.copy(
            initialScale = previewType.initialScale(screen)
        )
    }

    Box(
        modifier = modifier.then(
            if (previewType == PreviewType.MOBILE && screen.type != ScreenType.Small) {
                Modifier.widthIn(max = ScreenType.Small.width.dp)
            } else if (previewType == PreviewType.TABLET && screen.type == ScreenType.Large) {
                Modifier.widthIn(max = ScreenType.Medium.width.dp)
            } else {
                Modifier.fillMaxWidth()
            }
        )
    ) {
        WCWebView(
            url = url,
            userAgent = userAgent,
            authenticator = authenticator,
            settings = webViewSettings,
            progressIndicator = WebViewProgressIndicator.Linear()
        )
    }
}

@Suppress("MagicNumber")
private fun PreviewType.initialScale(screen: Screen): Int {
    return when (screen.type) {
        ScreenType.Small, ScreenType.Medium -> when (this) {
            PreviewType.MOBILE -> 0
            PreviewType.TABLET -> (260 * screen.width.value / ScreenType.Medium.width).toInt()
            PreviewType.DESKTOP -> (260 * screen.width.value / ScreenType.Large.width).toInt()
        }

        ScreenType.Large -> 0
    }
}
