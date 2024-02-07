package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationAdDestinationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    resourceProvider: ResourceProvider,
    selectedSite: SelectedSite,
    productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationAdDestinationFragmentArgs by savedStateHandle.navArgs()
    private val targetUrlPrompt = resourceProvider
            .getString(R.string.blaze_campaign_edit_ad_destination_empty_url_message)
    private val productUrl = requireNotNull(productDetailRepository.getProduct(navArgs.productId))
            .permalink

    private val _viewState = MutableStateFlow(
        ViewState(
            parameters = resourceProvider
                .getString(R.string.blaze_campaign_edit_ad_destination_empty_parameters_message),
            productUrl = productUrl,
            siteUrl = selectedSite.get().url,
            targetUrl = navArgs.targetUrl ?: targetUrlPrompt,
            isUrlDialogVisible = false
        )
    )

    val viewState = _viewState.asLiveData()

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onUrlPropertyTapped() {
        _viewState.update { it.copy(isUrlDialogVisible = true) }
    }

    fun onParameterPropertyTapped() {
        /* TODO */
    }

    fun onDestinationUrlChanged(destinationUrl: String) {
        _viewState.value = _viewState.value.copy(
            targetUrl = destinationUrl,
            isUrlDialogVisible = false
        )
    }

    data class ViewState(
        val parameters: String,
        val productUrl: String,
        val siteUrl: String,
        val targetUrl: String,
        val isUrlDialogVisible: Boolean
    )
}
