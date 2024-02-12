package com.woocommerce.android.ui.blaze.creation.success

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class BlazeCampaignSuccessBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignSuccessBottomSheet(::onDoneClicked)
        }
    }

    private fun onDoneClicked() {
        dismiss()
    }
}
