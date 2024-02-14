package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.getBaseUrl
import com.woocommerce.android.util.parseParameters
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    selectedSite: SelectedSite,
    productDetailRepository: ProductDetailRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationAdDestinationFragmentArgs by savedStateHandle.navArgs()
    private val productUrl = requireNotNull(productDetailRepository.getProduct(navArgs.productId))
        .permalink

    private val _viewState = MutableStateFlow(
        ViewState(
            productUrl = productUrl.trim('/'),
            siteUrl = selectedSite.get().url.trim('/'),
            destinationUrl = navArgs.targetUrl.getBaseUrl() + "?a=b&c=d",
            parameters = getParameters(navArgs.targetUrl),
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
        triggerEvent(
            NavigateToParametersScreen(getTargetUrl(_viewState.value.destinationUrl, _viewState.value.parameters))
        )
    }

    fun onTargetUrlUpdated(targetUrl: String) {
        _viewState.update {
            it.copy(
                destinationUrl = targetUrl.getBaseUrl(),
                parameters = getParameters(targetUrl),
            )
        }
    }

    fun onDestinationUrlChanged(destinationUrl: String) {
        _viewState.value = _viewState.value.copy(
            destinationUrl = destinationUrl,
            isUrlDialogVisible = false
        )
    }

    private fun getParameters(url: String): String {
        return url.parseParameters().entries.joinToString(separator = "\n")
            .ifBlank {
                resourceProvider.getString(R.string.blaze_campaign_edit_ad_destination_empty_parameters_message)
            }
    }

    private fun getTargetUrl(baseUrl: String, parameters: String): String {
        return if (parameters.isEmpty()) {
            baseUrl
        } else {
            "$baseUrl?${parameters.replace("\n", "&")}"
        }
    }

    data class ViewState(
        val productUrl: String,
        val siteUrl: String,
        val destinationUrl: String,
        val parameters: String,
        val isUrlDialogVisible: Boolean
    )

    data class NavigateToParametersScreen(val url: String) : MultiLiveEvent.Event()
}
