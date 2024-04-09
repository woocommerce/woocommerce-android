package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.OpenSupportRequest
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.OpenWebView
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderConnectivityToolFragment : BaseFragment() {
    val viewModel: OrderConnectivityToolViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    OrderConnectivityToolScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is OpenSupportRequest -> openSupportRequestScreen()
                is OpenWebView -> openWebView(it.url)
                is Exit -> findNavController().popBackStack()
            }
        }
        viewModel.startConnectionChecks()
    }

    override fun getFragmentTitle() = ""

    private fun openSupportRequestScreen() {
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = HelpOrigin.CONNECTIVITY_TOOL,
            extraTags = ArrayList()
        ).let { activity?.startActivity(it) }
    }

    private fun openWebView(url: String) { ChromeCustomTabUtils.launchUrl(requireContext(), url) }
}
