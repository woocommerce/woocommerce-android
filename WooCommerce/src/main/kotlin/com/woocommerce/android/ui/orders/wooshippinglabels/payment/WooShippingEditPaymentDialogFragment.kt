package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.findNavController
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewFragment
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewViewModel
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooShippingEditPaymentDialogFragment : WCBottomSheetDialogFragment() {
    private val viewModel: WooShippingEditPaymentViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                WooTheme {
                    WooShippingEditPaymentScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is WooShippingEditPaymentViewModel.ShowPaymentMethodAddWebView -> {
                    showWebView(event.url, event.successUrl)
                }
            }
        }
    }

    private fun handleResults() {
        handleNotice(AuthenticatedWebViewFragment.WEBVIEW_RESULT, navHostId = R.id.nav_host_fragment_main) {
            viewModel.onPaymentMethodAdded()
        }
    }

    private fun showWebView(url: String, exitUrl: String) {
        findNavController(R.id.nav_host_fragment_main).navigate(
            NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
                urlToLoad = url,
                urlsToTriggerExit = arrayOf(exitUrl),
                urlComparisonMode = AuthenticatedWebViewViewModel.UrlComparisonMode.PARTIAL
            )
        )
    }
}
