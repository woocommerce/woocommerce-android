package com.woocommerce.android.ui.common.webview

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.NavGraphPaymentFlowDirections
import com.woocommerce.android.NavGraphSettingsDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.findNavController
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import javax.inject.Inject

class AuthenticatedWebViewLauncher @Inject constructor(
    private val fragment: Fragment,
    private val canAutoAuthenticateInWebView: CanAutoAuthenticateInWebView
) {
    private val context: Context
        get() = fragment.requireContext()

    fun showAuthenticatedWebView(event: MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView) {
        val navController = findNavController()

        if (canAutoAuthenticateInWebView(event.url) || !event.fallbackToChromeTab) {
            val screenTitle = event.screenTitle?.let { UiHelpers.getTextOfUiString(context, it) }
            val action = navController.getAction(event.url, screenTitle)
            navController.navigate(action)
        } else {
            ChromeCustomTabUtils.launchUrl(context, event.url)
        }
    }

    private fun findNavController(): NavController {
        return when (fragment.activity) {
            // In MainActivity, given the dual-pane layout, we need to explicitly get the main nav controller.
            is MainActivity -> fragment.findNavController(R.id.nav_host_fragment_main)
            else -> fragment.findNavController()
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
