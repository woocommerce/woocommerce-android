package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_DESTINATION_SAVE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationAdDestinationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    productDetailRepository: ProductDetailRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationAdDestinationFragmentArgs by savedStateHandle.navArgs()
    private val productUrl = requireNotNull(productDetailRepository.getProduct(navArgs.productId)).permalink
    private val initialDestinationValues: ViewState

    private val _viewState = MutableStateFlow(
        ViewState(
            productUrl = productUrl,
            siteUrl = selectedSite.get().url,
            targetUrl = navArgs.destinationParameters.targetUrl,
            parameters = navArgs.destinationParameters.parameters,
            isUrlDialogVisible = false
        )
    )

    val viewState = _viewState.asLiveData()

    init {
        initialDestinationValues = _viewState.value
    }

    fun onBackPressed() {
        if (initialDestinationValues != _viewState.value) {
            analyticsTrackerWrapper.track(stat = BLAZE_CREATION_EDIT_DESTINATION_SAVE_TAPPED)
        }
        triggerEvent(ExitWithResult(DestinationParameters(_viewState.value.targetUrl, _viewState.value.parameters)))
    }

    fun onUrlPropertyTapped() {
        _viewState.update { it.copy(isUrlDialogVisible = true) }
    }

    fun onParameterPropertyTapped() {
        triggerEvent(
            NavigateToParametersScreen(DestinationParameters(_viewState.value.targetUrl, _viewState.value.parameters))
        )
    }

    fun onDestinationParametersUpdated(targetUrl: String, parameters: Map<String, String>? = null) {
        _viewState.update {
            it.copy(
                targetUrl = targetUrl,
                parameters = parameters ?: it.parameters,
                isUrlDialogVisible = false
            )
        }
    }

    data class ViewState(
        val productUrl: String,
        val siteUrl: String,
        val targetUrl: String,
        val parameters: Map<String, String>,
        val isUrlDialogVisible: Boolean
    ) {
        val joinedParameters: String
            get() = parameters.entries.joinToString(separator = "\n")
    }

    data class NavigateToParametersScreen(val destinationParameters: DestinationParameters) : MultiLiveEvent.Event()
}
