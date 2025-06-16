package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.orders.wooshippinglabels.ObserveAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class WooShippingEditPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAccountSettings: ObserveAccountSettings
) : ScopedViewModel(savedStateHandle) {
    val viewState = observeAccountSettings()
        .map {
            val accountSettings = it ?: return@map ViewState.Loading

            ViewState.Content(
                editEnabled = true, // TODO
                emailTheReceipt = true, // TODO
                storeOwnerName = "John Doe", // TODO
                storeOwnerUsername = "johndoe", // TODO
                paymentMethods = accountSettings.paymentMethodOptions.paymentMethods,
                selectedPaymentMethodId = accountSettings.paymentMethodOptions.selectedPaymentId
            )
        }
        .onStart { emit(ViewState.Loading) }
        .asLiveData()

    fun onUpdatePaymentMethod(paymentMethodId: Int) {
        TODO()
    }

    fun onAddNewPaymentMethod() {
        TODO()
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data class Content(
            val editEnabled: Boolean,
            val emailTheReceipt: Boolean,
            val storeOwnerName: String,
            val storeOwnerUsername: String,
            val paymentMethods: List<PaymentMethodModel>,
            val selectedPaymentMethodId: Int?
        ) : ViewState {
            val selectedPaymentMethod: PaymentMethodModel?
                get() = paymentMethods.find { it.paymentMethodId == selectedPaymentMethodId }
        }
    }
}
