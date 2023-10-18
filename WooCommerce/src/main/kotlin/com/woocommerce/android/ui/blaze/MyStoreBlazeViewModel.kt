package com.woocommerce.android.ui.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.util.FeatureFlag.BLAZE_ITERATION_2
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MyStoreBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _blazeCampaignState =
        MutableStateFlow(
            MyStoreBlazeUi(
                isVisible = BLAZE_ITERATION_2.isEnabled(),
                product = BlazeProductUi(
                    name = "Product name",
                    imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                ),
                blazeActiveCampaign = BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = "Product name",
                        imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                    ),
                    status = Active,
                    stats = listOf(
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_impressions,
                            value = 100
                        ),
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_clicks,
                            value = 10
                        )
                    ),
                )
            )
        )
    val blazeCampaignState = _blazeCampaignState.asLiveData()

    fun onShowAllCampaignsClicked() {
        triggerEvent(ShowAllCampaigns)
    }

    data class MyStoreBlazeUi(
        val isVisible: Boolean,
        val product: BlazeProductUi,
        val blazeActiveCampaign: BlazeCampaignUi?
    )

    object ShowAllCampaigns : MultiLiveEvent.Event()
}
