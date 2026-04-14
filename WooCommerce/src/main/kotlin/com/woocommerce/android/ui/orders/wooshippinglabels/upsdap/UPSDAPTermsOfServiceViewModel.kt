package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UPSDAPTermsOfServiceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val acceptUPSDAPTerms: AcceptUPSDAPTerms
) : ScopedViewModel(savedState) {
    companion object {
        private const val TERMS_URL = "https://www.ups.com/assets/resources/webcontent/" +
            "en_US/ups_dap_supplemental_tc.pdf"
        private const val PROHIBITED_ITEMS_URL = "https://www.ups.com/us/en/support/shipping-support/" +
            "shipping-special-care-regulated-items/prohibited-items.page"
        private const val TECHNOLOGY_AGREEMENT_URL = "https://www.ups.com/assets/resources/webcontent/en_US/UTA.pdf"
    }

    private val args: UPSDAPTermsOfServiceBottomSheetFragmentArgs by savedState.navArgs()

    private val isTermsOfServiceAccepted = savedState.getStateFlow(
        scope = viewModelScope,
        key = "terms_of_service_accepted",
        initialValue = false
    )
    private val isProhibitedItemsChecked = savedState.getStateFlow(
        scope = viewModelScope,
        key = "prohibited_items_checked",
        initialValue = false
    )
    private val isTechnologyAgreementChecked = savedState.getStateFlow(
        scope = viewModelScope,
        key = "technology_agreement_checked",
        initialValue = false
    )

    private val isLoading = MutableStateFlow(false)

    private val conditionsState = combine(
        isTermsOfServiceAccepted,
        isProhibitedItemsChecked,
        isTechnologyAgreementChecked
    ) { termsAccepted, prohibitedItemsChecked, technologyAgreementChecked ->
        ConditionsState(
            isTermsOfServiceChecked = termsAccepted,
            isProhibitedItemsChecked = prohibitedItemsChecked,
            isTechnologyAgreementChecked = technologyAgreementChecked,
            onTermsOfServiceCheckedChanged = { newValue ->
                isTermsOfServiceAccepted.value = newValue
            },
            onProhibitedItemsCheckedChanged = { newValue ->
                isProhibitedItemsChecked.value = newValue
            },
            onTechnologyAgreementCheckedChanged = { newValue ->
                isTechnologyAgreementChecked.value = newValue
            }
        )
    }

    val viewState = combine(isLoading, conditionsState) { isLoading, conditionsState ->
        ViewState(
            isLoading = isLoading,
            originShippingAddress = args.originAddress,
            conditionsState = conditionsState,
            onUrlClicked = ::onUrlClicked,
            onContinueClicked = ::onContinueClicked
        )
    }.asLiveData()

    private fun onUrlClicked(url: String) {
        val finalUrl = when (url) {
            "ups-tos" -> TERMS_URL
            "ups-prohibited-items" -> PROHIBITED_ITEMS_URL
            "ups-technology-agreement" -> TECHNOLOGY_AGREEMENT_URL
            else -> error("Unknown URL: $url")
        }
        triggerEvent(MultiLiveEvent.Event.LaunchUrlInChromeTab(finalUrl))
    }

    private fun onContinueClicked() {
        launch {
            isLoading.value = true
            acceptUPSDAPTerms(args.originAddress).fold(
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

    data class ConditionsState(
        val isTermsOfServiceChecked: Boolean,
        val isProhibitedItemsChecked: Boolean,
        val isTechnologyAgreementChecked: Boolean,
        val onTermsOfServiceCheckedChanged: (Boolean) -> Unit,
        val onProhibitedItemsCheckedChanged: (Boolean) -> Unit,
        val onTechnologyAgreementCheckedChanged: (Boolean) -> Unit
    )

    data class ViewState(
        val isLoading: Boolean,
        val originShippingAddress: OriginShippingAddress,
        val conditionsState: ConditionsState,
        val onUrlClicked: (String) -> Unit,
        val onContinueClicked: () -> Unit,
    ) {
        val areAllConditionsAccepted: Boolean
            get() = conditionsState.isTermsOfServiceChecked &&
                conditionsState.isProhibitedItemsChecked &&
                conditionsState.isTechnologyAgreementChecked
    }
}
