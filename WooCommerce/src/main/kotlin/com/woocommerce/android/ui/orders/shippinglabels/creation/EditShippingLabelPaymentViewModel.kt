package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.StoreOwnerDetails
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class EditShippingLabelPaymentViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        loadPaymentMethods()
    }

    private fun loadPaymentMethods() {
        launch {
            viewState = viewState.copy(isLoading = true)
            val accountSettings = shippingLabelRepository.getAccountSettings().let {
                if (it.isError) {
                    triggerEvent(ShowSnackbar(0))
                    triggerEvent(Exit)
                    return@launch
                }
                it.model!!
            }
            viewState = ViewState(
                isLoading = false,
                currentAccountSettings = accountSettings,
                paymentMethods = accountSettings.paymentMethods.map {
                    PaymentMethodUiModel(paymentMethod = it, isSelected = it.id == accountSettings.selectedPaymentId)
                },
                canManagePayments = accountSettings.canManagePayments,
                emailReceipts = accountSettings.isEmailReceiptEnabled,
                storeOwnerDetails = accountSettings.storeOwnerDetails
            )
        }
    }

    fun onEmailReceiptsCheckboxChanged(isChecked: Boolean) {
        viewState = viewState.copy(emailReceipts = isChecked)
    }

    fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
        val paymentMethodsModels = viewState.paymentMethods.map {
            it.copy(isSelected = it.paymentMethod == paymentMethod)
        }
        viewState = viewState.copy(paymentMethods = paymentMethodsModels)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        private val currentAccountSettings: ShippingAccountSettings? = null,
        val canManagePayments: Boolean = false,
        val paymentMethods: List<PaymentMethodUiModel> = emptyList(),
        val emailReceipts: Boolean = false,
        val storeOwnerDetails: StoreOwnerDetails? = null
    ) : Parcelable {
        val hasChanges: Boolean
            get() {
                return currentAccountSettings?.let {
                    val selectedPaymentMethod = paymentMethods.find { it.isSelected }
                    selectedPaymentMethod?.paymentMethod?.id != currentAccountSettings.selectedPaymentId ||
                        emailReceipts != currentAccountSettings.isEmailReceiptEnabled
                } ?: false
            }
    }

    @Parcelize
    data class PaymentMethodUiModel(
        val paymentMethod: PaymentMethod,
        val isSelected: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelPaymentViewModel>
}
