package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UPSDAPTermsOfServiceBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val TOS_ACCEPTED_NOTICE_KEY = "tos_accepted_notice_key"
    }

    private val viewModel: UPSDAPTermsOfServiceViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                UPSDAPTermsOfServiceBottomSheet(viewModel)
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
                is MultiLiveEvent.Event.OpenUrl -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
            }
        }
    }
}
