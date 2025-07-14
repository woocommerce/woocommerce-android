package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.StoreOwnerDetails
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.DataLoadState.Success
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class EditShippingLabelPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState) {
    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        launch {
            loadPaymentMethods(forceRefresh = false)
            if (viewState.dataLoadState == DataLoadState.Success &&
                viewState.paymentMethods.isEmpty() &&
                viewState.canManagePayments
            ) {
                triggerEvent(AddPaymentMethod)
            }
        }
    }

    private suspend fun loadPaymentMethods(forceRefresh: Boolean) {
        viewState = viewState.copy(dataLoadState = DataLoadState.Loading)
        viewState = shippingLabelRepository.getAccountSettings(forceRefresh)
            .model?.let { accountSettings ->
                viewState.copy(
                    dataLoadState = DataLoadState.Success,
                    currentAccountSettings = accountSettings,
                    paymentMethods = accountSettings.paymentMethods.map {
                        PaymentMethodUiModel(
                            paymentMethod = it,
                            isSelected = it.id == accountSettings.selectedPaymentId
                        )
                    },
                    canManagePayments = accountSettings.canManagePayments,
                    // Allow editing the email receipts option if the user has either the permission to change settings
                    // or changing payment options
                    canEditSettings = accountSettings.canEditSettings || accountSettings.canManagePayments,
                    emailReceipts = accountSettings.isEmailReceiptEnabled,
                    storeOwnerDetails = accountSettings.storeOwnerDetails
                )
            } ?: viewState.copy(dataLoadState = DataLoadState.Error)
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

    fun onAddPaymentMethodClicked() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_ADD_PAYMENT_METHOD_TAPPED)
        triggerEvent(AddPaymentMethod)
    }

    fun onDoneButtonClicked() {
        launch {
            val selectedPaymentMethod =
                viewState.paymentMethods.find { it.isSelected }!!.paymentMethod

            val requiresSaving = selectedPaymentMethod.id != viewState.currentAccountSettings?.selectedPaymentId ||
                viewState.emailReceipts != viewState.emailReceipts

            if (requiresSaving) {
                viewState = viewState.copy(showSavingProgressDialog = true)
                val result = shippingLabelRepository.updatePaymentSettings(
                    selectedPaymentMethodId = selectedPaymentMethod.id,
                    emailReceipts = viewState.emailReceipts
                )
                viewState = viewState.copy(showSavingProgressDialog = false)

                if (result.isError) {
                    triggerEvent(ShowSnackbar(R.string.shipping_label_payments_saving_error))
                    return@launch
                }
            }
            triggerEvent(ExitWithResult(selectedPaymentMethod))
        }
    }

    fun onBackButtonClicked() {
        triggerEvent(Exit)
    }

    fun refreshData() {
        launch {
            loadPaymentMethods(forceRefresh = false)
        }
    }

    fun onPaymentMethodAdded() {
        launch {
            val countOfCurrentPaymentMethods = viewState.paymentMethods.size
            loadPaymentMethods(forceRefresh = true)
            if (viewState.dataLoadState == Success &&
                viewState.paymentMethods.size == countOfCurrentPaymentMethods + 1
            ) {
                AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_PAYMENT_METHOD_ADDED)
                triggerEvent(ShowSnackbar(R.string.shipping_label_payment_method_added))
            }
        }
    }

    @Parcelize
    data class ViewState(
        val dataLoadState: DataLoadState? = null,
        val currentAccountSettings: ShippingAccountSettings? = null,
        val canManagePayments: Boolean = false,
        val canEditSettings: Boolean = false,
        val paymentMethods: List<PaymentMethodUiModel> = emptyList(),
        val emailReceipts: Boolean = false,
        val storeOwnerDetails: StoreOwnerDetails? = null,
        val showSavingProgressDialog: Boolean = false
    ) : Parcelable {
        val canSave: Boolean
            get() = canEditSettings && paymentMethods.any { it.isSelected }
        val showAddPaymentButton: Boolean
            get() = canManagePayments && paymentMethods.isNotEmpty()
        val showAddFirstPaymentButton: Boolean
            get() = canManagePayments && paymentMethods.isEmpty()
    }

    enum class DataLoadState {
        Loading, Error, Success
    }

    @Parcelize
    data class PaymentMethodUiModel(
        val paymentMethod: PaymentMethod,
        val isSelected: Boolean
    ) : Parcelable

    object AddPaymentMethod : MultiLiveEvent.Event()
}
