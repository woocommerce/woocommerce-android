package com.woocommerce.android.ui.woopos.common.composeui.component.authenticatedwebview

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce
import java.net.URLDecoder
import java.net.URLEncoder

private const val WEBVIEW_ROUTE = "webview"
private const val WEBVIEW_URL_KEY = "url"
private const val WEBVIEW_TITLE_KEY = "title"

fun NavController.navigateToWebViewScreen(url: String, title: String = "") {
    val encodedUrl = URLEncoder.encode(url, "UTF-8")
    val encodedTitle = URLEncoder.encode(title, "UTF-8")
    navigateOnce("$WEBVIEW_ROUTE/$encodedUrl?$WEBVIEW_TITLE_KEY=$encodedTitle")
}

fun NavGraphBuilder.webViewScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    composable(
        route = "$WEBVIEW_ROUTE/{$WEBVIEW_URL_KEY}?$WEBVIEW_TITLE_KEY={$WEBVIEW_TITLE_KEY}",
        arguments = listOf(
            navArgument(WEBVIEW_URL_KEY) { type = NavType.StringType },
            navArgument(WEBVIEW_TITLE_KEY) {
                type = NavType.StringType
                defaultValue = ""
            }
        ),
        enterTransition = {
            slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        },
    ) { entry ->
        val url = URLDecoder.decode(
            entry.arguments?.getString(WEBVIEW_URL_KEY).orEmpty(),
            "UTF-8"
        )
        val title = URLDecoder.decode(
            entry.arguments?.getString(WEBVIEW_TITLE_KEY).orEmpty(),
            "UTF-8"
        )
        val viewModel: WooPosAuthenticatedWebViewViewModel = hiltViewModel()
        WooPosAuthenticatedWebView(
            url = url,
            title = title,
            authenticator = viewModel.webViewAuthenticator,
            userAgent = viewModel.userAgent,
            onBackClick = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        )
    }
}
