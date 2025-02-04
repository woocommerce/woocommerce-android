package com.woocommerce.android.ui.blaze.creation.success

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class ShouldShowFeedbackRequest @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) {
    companion object {
        private const val MINIMUM_CAMPAIGNS_FOR_FEEDBACK_REQUEST = 2
    }

    suspend operator fun invoke(): Boolean =
        blazeCampaignsStore.getBlazeCampaigns(selectedSite.get()).size >= MINIMUM_CAMPAIGNS_FOR_FEEDBACK_REQUEST
}
