package com.woocommerce.android.ui.blaze.creation.success

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.AppUrls.BLAZE_CAMPAIGN_CREATION_SURVEY_URL_I1
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignSuccessBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignSuccessBottomSheet(
                ::onDoneClicked,
                ::onFeedbackRequestTapped
            )
        }
    }

    private fun onDoneClicked() {
        dismiss()
    }

    private fun onFeedbackRequestTapped(isPositive: Boolean) {
        ChromeCustomTabUtils.launchUrl(requireContext(), BLAZE_CAMPAIGN_CREATION_SURVEY_URL_I1)
    }
}
