package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import javax.inject.Inject

@HiltViewModel
class MyStoreBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMostRecentBlazeCampaign: ObserveMostRecentBlazeCampaign
) : ScopedViewModel(savedStateHandle) {
    val blazeCampaignState = observeMostRecentBlazeCampaign().map {
        when (it) {
            null -> prepareUiForNoCampaign()
            else -> prepareUiForCampaign(it)
        }
    }.asLiveData()

    private fun prepareUiForNoCampaign(): MyStoreBlazeUi {
        return MyStoreBlazeUi(
            isVisible = FeatureFlag.BLAZE_ITERATION_2.isEnabled(),
            product = BlazeProductUi(
                name = "Product name",
                imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
            ),
            blazeActiveCampaign = null
        )
    }

    private fun prepareUiForCampaign(campaign: BlazeCampaignModel): MyStoreBlazeUi {
        return MyStoreBlazeUi(
            isVisible = FeatureFlag.BLAZE_ITERATION_2.isEnabled(),
            product = BlazeProductUi(
                name = "Product name",
                imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
            ),
            blazeActiveCampaign = BlazeCampaignUi(
                product = BlazeProductUi(
                    name = "Product name",
                    imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                ),
                status = CampaignStatusUi.Active,
                impressions = 100,
                clicks = 10,
                budget = 1000
            )
        )
    }

    @Parcelize
    data class MyStoreBlazeUi(
        val isVisible: Boolean,
        val product: BlazeProductUi,
        val blazeActiveCampaign: BlazeCampaignUi?
    ) : Parcelable

    @Parcelize
    data class BlazeProductUi(
        val name: String,
        val imgUrl: String
    ) : Parcelable

    @Parcelize
    data class BlazeCampaignUi(
        val product: BlazeProductUi,
        val status: CampaignStatusUi,
        val impressions: Int,
        val clicks: Int,
        val budget: Int
    ) : Parcelable

    enum class CampaignStatusUi(
        @StringRes val statusDisplayText: Int,
        @ColorRes val textColor: Int,
        @ColorRes val backgroundColor: Int,
    ) {
        InModeration(
            statusDisplayText = R.string.blaze_campaign_status_in_moderation,
            textColor = R.color.blaze_campaign_status_in_moderation_text,
            backgroundColor = R.color.blaze_campaign_status_in_moderation_background
        ),
        Active(
            statusDisplayText = R.string.blaze_campaign_status_active,
            textColor = R.color.blaze_campaign_status_active_text,
            backgroundColor = R.color.blaze_campaign_status_active_background
        ),
        Completed(
            statusDisplayText = R.string.blaze_campaign_status_completed,
            textColor = R.color.blaze_campaign_status_rejected_text,
            backgroundColor = R.color.blaze_campaign_status_rejected_background
        ),
        Rejected(
            statusDisplayText = R.string.blaze_campaign_status_rejected,
            textColor = R.color.blaze_campaign_status_completed_text,
            backgroundColor = R.color.blaze_campaign_status_completed_background
        ),
    }
}
