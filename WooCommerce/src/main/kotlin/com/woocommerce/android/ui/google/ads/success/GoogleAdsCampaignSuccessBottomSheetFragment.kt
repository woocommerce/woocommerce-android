package com.woocommerce.android.ui.google.ads.success

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GoogleAdsCampaignSuccessBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            GoogleAdsCampaignSuccessBottomSheet(::onDoneClicked)
        }
    }

    private fun onDoneClicked() {
        dismiss()
    }
}
