package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.wooshippinglabels.ObserveAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class WooShippingEditPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAccountSettings: ObserveAccountSettings
) : ScopedViewModel(savedStateHandle) {
    private val selectedPaymentMethod = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = Int::class.java,
        key = "selectedPaymentMethod",
    )

    val viewState = combine(
        observeAccountSettings(),
        selectedPaymentMethod
    ) { accountSettings, selectedPaymentMethod ->
        if (accountSettings == null) return@combine ViewState.Loading

        ViewState.Content(
            canManagePaymentMethods = true, // TODO
            canEditSettings = true, // TODO
            emailTheReceipt = true, // TODO
            storeOwnerName = "John Doe", // TODO
            storeOwnerUsername = "johndoe", // TODO
            selectedPaymentMethodId = selectedPaymentMethod ?: accountSettings.paymentMethodOptions.selectedPaymentId,
            currentPaymentOptions = accountSettings.paymentMethodOptions
        )
    }
        .onStart { emit(ViewState.Loading) }
        .asLiveData()

    fun onAddNewPaymentMethod() {
        TODO()
    }

    fun onPaymentMethodSelected(paymentMethodId: Int?) {
        selectedPaymentMethod.value = paymentMethodId
    }

    fun onSaveClicked() {
        TODO()
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data class Content(
            val canManagePaymentMethods: Boolean,
            val canEditSettings: Boolean,
            val emailTheReceipt: Boolean,
            val selectedPaymentMethodId: Int?,
            val storeOwnerName: String,
            val storeOwnerUsername: String,
            val currentPaymentOptions: PaymentMethodOptions
        ) : ViewState {
            val paymentMethods get() = currentPaymentOptions.paymentMethods
        }
    }
}
