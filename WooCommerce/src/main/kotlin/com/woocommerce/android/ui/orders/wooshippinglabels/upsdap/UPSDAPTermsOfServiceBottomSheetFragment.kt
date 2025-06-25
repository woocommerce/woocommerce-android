package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UPSDAPTermsOfServiceBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val args: UPSDAPTermsOfServiceBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            UPSDAPTermsOfServiceBottomSheet(
                originAddress = args.originAddress,
                onAcceptClicked = { onAcceptClicked() },
                onUrlClicked = { onUrlClicked(it) }
            )
        }
    }

    private fun onAcceptClicked() {
        navigateBackWithNotice(TOS_ACCEPTED_NOTICE_KEY)
    }

    private fun onUrlClicked(url: String) {
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    companion object {
        const val TOS_ACCEPTED_NOTICE_KEY = "tos_accepted_notice_key"
    }
}
