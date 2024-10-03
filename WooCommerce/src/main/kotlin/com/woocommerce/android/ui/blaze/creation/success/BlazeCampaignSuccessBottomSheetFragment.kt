package com.woocommerce.android.ui.blaze.creation.success

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.AppUrls.BLAZE_CAMPAIGN_CREATION_SURVEY_URL_I1
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlazeCampaignSuccessBottomSheetFragment : WCBottomSheetDialogFragment() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject
    lateinit var shouldShowFeedbackRequest: ShouldShowFeedbackRequest

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignSuccessBottomSheet(
                ::onDoneClicked,
                ::onFeedbackRequestTapped,
                shouldShowFeedbackRequest
            )
        }
    }

    private fun onDoneClicked() {
        dismiss()
    }

    private fun onFeedbackRequestTapped(isPositive: Boolean) {
        analyticsTracker.track(
            stat = AnalyticsEvent.BLAZE_CAMPAIGN_CREATION_FEEDBACK,
            properties = mapOf(
                AnalyticsTracker.KEY_SOURCE to "satisfied",
                AnalyticsTracker.KEY_IS_USEFUL to isPositive
            )
        )
        ChromeCustomTabUtils.launchUrl(requireContext(), BLAZE_CAMPAIGN_CREATION_SURVEY_URL_I1)
    }
}
