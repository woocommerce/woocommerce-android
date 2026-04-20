package com.woocommerce.android.ui.orders.wooshippinglabels.fedex

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FedExTermsOfServiceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val acceptFedExTerms: AcceptFedExTerms,
) : ScopedViewModel(savedState) {
    private val isTermsOfServiceAccepted = savedState.getStateFlow(
        scope = viewModelScope,
        key = "terms_of_service_accepted",
        initialValue = false
    )
    private val isLoading = MutableStateFlow(false)

    val viewState = combine(isLoading, isTermsOfServiceAccepted) { loading, isAccepted ->
        ViewState(
            isLoading = loading,
            isTermsOfServiceAccepted = isAccepted,
            onUrlClicked = ::onUrlClicked,
            onTermsOfServiceCheckedChanged = { newValue ->
                isTermsOfServiceAccepted.value = newValue
            },
            onContinueClicked = ::onContinueClicked
        )
    }.asLiveData()

    private fun onUrlClicked(url: String) {
        val finalUrl = when (url) {
            TERMS_URL_ID -> TERMS_URL
            else -> error("Unknown URL: $url")
        }
        triggerEvent(MultiLiveEvent.Event.LaunchUrlInChromeTab(finalUrl))
    }

    private fun onContinueClicked() {
        launch {
            isLoading.value = true
            acceptFedExTerms().fold(
                onSuccess = {
                    triggerEvent(MultiLiveEvent.Event.ExitWithResult(Unit))
                },
                onFailure = {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
                }
            )
            isLoading.value = false
        }
    }

    data class ViewState(
        val isLoading: Boolean,
        val isTermsOfServiceAccepted: Boolean,
        val onUrlClicked: (String) -> Unit,
        val onTermsOfServiceCheckedChanged: (Boolean) -> Unit,
        val onContinueClicked: () -> Unit,
    )

    companion object {
        const val TERMS_URL_ID = "fedex-tos"
        private const val TERMS_URL = "https://wordpress.com/tos/"
    }
}
