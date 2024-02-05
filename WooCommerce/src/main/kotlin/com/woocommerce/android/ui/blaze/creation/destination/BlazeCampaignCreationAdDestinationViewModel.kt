package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationAdDestinationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(
        ViewState(
            destinationUrl = resourceProvider.getString(R.string.blaze_campaign_edit_ad_destination_empty_url_message),
            parameters = resourceProvider.getString(R.string.blaze_campaign_edit_ad_destination_empty_parameters_message)
        )
    )

    val viewState = _viewState.asLiveData()

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onUrlPropertyTapped() {
        /* TODO */
    }

    fun onParameterPropertyTapped() {
        /* TODO */
    }

    data class ViewState(
        val destinationUrl: String,
        val parameters: String
    )
}
