package com.woocommerce.android.ui.sitepicker.sitediscovery

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

@HiltViewModel
class SitePickerSiteDiscoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val siteAddressFlow = savedStateHandle.getStateFlow(viewModelScope, "")
    private val stepFlow = savedStateHandle.getStateFlow(viewModelScope, Step.AddressInput)

    private val isLoadingFlow = MutableStateFlow(false)
    private val inlineErrorFlow = savedStateHandle.getStateFlow(viewModelScope, 0)

    val viewState = combine(siteAddressFlow, stepFlow) { address, step ->
        Pair(address, step)
    }.transformLatest<Pair<String, Step>, ViewState> { (address, step) ->
        when (step) {
            Step.AddressInput -> emitAll(prepareAddressViewState(address))
            Step.JetpackUnavailable -> TODO()
            Step.NotWordpress -> TODO()
        }
    }.asLiveData()

    private fun prepareAddressViewState(address: String): Flow<ViewState.AddressInputState> {
        return combine(isLoadingFlow, inlineErrorFlow) { isLoading, error ->
            ViewState.AddressInputState(
                siteAddress = address,
                isAddressValid = PatternsCompat.WEB_URL.matcher(address).matches(),
                isLoading = isLoading,
                inlineErrorMessage = error
            )
        }
    }

    fun onAddressChanged(address: String) {
        inlineErrorFlow.value = 0
        siteAddressFlow.value = address
    }

    private enum class Step {
        AddressInput, JetpackUnavailable, NotWordpress
    }

    sealed class ViewState {
        abstract val siteAddress: String

        data class AddressInputState(
            override val siteAddress: String,
            val isAddressValid: Boolean,
            val isLoading: Boolean,
            @StringRes val inlineErrorMessage: Int = 0
        ) : ViewState()

        data class ErrorState(
            override val siteAddress: String,
            val message: String,
            val primaryButtonText: String,
            val primaryButtonAction: () -> Unit,
            val secondaryButtonText: String,
            val secondaryButtonAction: () -> Unit
        ) : ViewState()
    }
}
