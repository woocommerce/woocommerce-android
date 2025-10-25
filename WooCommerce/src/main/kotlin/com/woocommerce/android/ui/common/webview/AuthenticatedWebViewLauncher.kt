package com.woocommerce.android.ui.common.webview

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.NavGraphPaymentFlowDirections
import com.woocommerce.android.NavGraphSettingsDirections
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import javax.inject.Inject

class AuthenticatedWebViewLauncher @Inject constructor(
    private val canAutoAuthenticateInWebView: CanAutoAuthenticateInWebView
) {
    fun NavController.showAuthenticatedWebView(event: MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView) {
        if (canAutoAuthenticateInWebView(event.url) || !event.fallbackToChromeTab) {
            val screenTitle = event.screenTitle?.let { UiHelpers.getTextOfUiString(context, it) }
            val action = getAction(event.url, screenTitle)
            navigate(action)
        } else {
            ChromeCustomTabUtils.launchUrl(context, event.url)
        }
    }

    private fun NavController.getAction(url: String, screenTitle: String?): NavDirections {
        return when (graph.id) {
            R.id.nav_graph_main -> {
                NavGraphMainDirections
                    .actionGlobalAuthenticatedWebViewFragment(urlToLoad = url, title = screenTitle)
            }

            R.id.nav_graph_settings -> {
                NavGraphSettingsDirections
                    .actionGlobalAuthenticatedWebViewFragment(urlToLoad = url, title = screenTitle)
            }

            R.id.nav_graph_payment_flow -> {
                NavGraphPaymentFlowDirections
                    .actionGlobalAuthenticatedWebViewFragment(urlToLoad = url, title = screenTitle)
            }

            else -> error("Unsupported navigation graph for authenticated web view.")
        }
    }
}
