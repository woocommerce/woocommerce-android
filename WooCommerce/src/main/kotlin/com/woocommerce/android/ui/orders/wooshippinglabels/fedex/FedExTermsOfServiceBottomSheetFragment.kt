package com.woocommerce.android.ui.orders.wooshippinglabels.fedex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FedExTermsOfServiceBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val viewModel: FedExTermsOfServiceViewModel by viewModels()
    private val snackbarHostState = SnackbarHostState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    FedExTermsOfServiceBottomSheet(viewModel, snackbarHostState)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithNotice(TOS_ACCEPTED_NOTICE_KEY)
                is MultiLiveEvent.Event.LaunchUrlInChromeTab -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }

                is MultiLiveEvent.Event.ShowSnackbar -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        snackbarHostState.showSnackbar(message = getString(event.message))
                    }
                }
            }
        }
    }

    companion object {
        const val TOS_ACCEPTED_NOTICE_KEY = "tos_accepted_notice_key"
    }
}
