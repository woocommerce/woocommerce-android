package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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
    productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationAdDestinationFragmentArgs by savedStateHandle.navArgs()
    private val productUrl = requireNotNull(productDetailRepository.getProduct(navArgs.productId))
        .permalink

    private val _viewState = MutableStateFlow(
        ViewState(
            productUrl = productUrl,
            siteUrl = selectedSite.get().url,
            targetUrl = navArgs.targetUrl + "?utm_source=woocommerce_android&utm_medium=ad&utm_campaign=blaze",
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
        triggerEvent(NavigateToParametersScreen(_viewState.value.targetUrl))
    }

    fun onDestinationUrlChanged(destinationUrl: String) {
        _viewState.value = _viewState.value.copy(
            targetUrl = destinationUrl,
            isUrlDialogVisible = false
        )
    }

    data class ViewState(
        val productUrl: String,
        val siteUrl: String,
        val targetUrl: String,
        val isUrlDialogVisible: Boolean
    ) {
        val parameters: String?
            get() = getParameters(targetUrl)

        private fun getParameters(url: String): String? {
            val parameters = url.split("?").getOrNull(1) ?: return null
            return parameters.replace("&", "\n")
        }
    }

    data class NavigateToParametersScreen(val url: String) : MultiLiveEvent.Event()
}
