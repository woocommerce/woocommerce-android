package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class UPSDAPTermsOfServiceViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val args: UPSDAPTermsOfServiceBottomSheetFragmentArgs by savedState.navArgs()

    private var isTermsOfServiceAccepted = savedState.getStateFlow(
        scope = viewModelScope,
        key = "terms_of_service_accepted",
        initialValue = false
    )
    private var isProhibitedItemsChecked = savedState.getStateFlow(
        scope = viewModelScope,
        key = "prohibited_items_checked",
        initialValue = false
    )
    private var isTechnologyAgreementChecked = savedState.getStateFlow(
        scope = viewModelScope,
        key = "technology_agreement_checked",
        initialValue = false
    )

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

    val viewState = conditionsState.map { conditionsState ->
        ViewState(
            originShippingAddress = args.originAddress,
            conditionsState = conditionsState,
            onUrlClicked = ::onUrlClicked,
            onContinueClicked = ::onContinueClicked
        )
    }.asLiveData()

    private fun onUrlClicked(url: String) {
        triggerEvent(MultiLiveEvent.Event.OpenUrl(url))
    }

    private fun onContinueClicked() {
        // TODO handle accepting the terms and conditions
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(Unit))
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
