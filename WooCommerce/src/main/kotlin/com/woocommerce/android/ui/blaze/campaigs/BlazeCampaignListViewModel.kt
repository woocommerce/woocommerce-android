package com.woocommerce.android.ui.blaze.campaigs

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.ui.blaze.CampaignStatusUi.InModeration
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel

class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _state =
        savedStateHandle.getStateFlow(
            scope = viewModelScope,
            initialValue = BlazeCampaignListState(
                campaigns = listOf(
                    BlazeCampaignUi(
                        product = BlazeProductUi(
                            name = "Product name",
                            imgUrl =
                            "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                        ),
                        status = Active,
                        stats = listOf(
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_impressions,
                                value = 100
                            ),
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_clicks,
                                value = 10
                            ),
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_budget,
                                value = 1000
                            ),
                        ),
                    ),
                    BlazeCampaignUi(
                        product = BlazeProductUi(
                            name = "Product name",
                            imgUrl =
                            "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                        ),
                        status = InModeration,
                        stats = listOf(
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_impressions,
                                value = 100
                            ),
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_clicks,
                                value = 10
                            ),
                            BlazeCampaignStat(
                                name = R.string.blaze_campaign_status_budget,
                                value = 1000
                            ),
                        ),
                    )
                ),
                isLoading = false
            )
        )
    val state = _state.asLiveData()

    fun onCampaignSelected() {
        // TODO
    }

    fun onAddNewCampaignClicked() {
        // TODO
    }

    @Parcelize
    data class BlazeCampaignListState(
        val campaigns: List<BlazeCampaignUi>,
        val isLoading: Boolean,
    ) : Parcelable
}
