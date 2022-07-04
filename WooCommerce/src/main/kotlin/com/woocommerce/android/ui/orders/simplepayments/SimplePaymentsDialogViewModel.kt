package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreationRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SimplePaymentsDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val orderCreationRepository: OrderCreationRepository
) : ScopedViewModel(savedState) {
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData

    var currentPrice: BigDecimal
        get() = viewState.currentPrice
        set(value) {
            viewState = viewState.copy(
                currentPrice = value,
                isDoneButtonEnabled = value > BigDecimal.ZERO
            )
        }

    fun onDoneButtonClicked() {
        createSimplePaymentsOrder()
    }

    private fun createSimplePaymentsOrder() {
        if (!networkStatus.isConnected()) {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            return
        }

        viewState = viewState.copy(isProgressShowing = true, isDoneButtonEnabled = false)

        launch(Dispatchers.IO) {
            val result = orderCreationRepository.createSimplePaymentOrder(viewState.currentPrice)
            withContext(Dispatchers.Main) {
                viewState = viewState.copy(isProgressShowing = false, isDoneButtonEnabled = true)
                result.fold(
                    onSuccess = {
                        viewState = viewState.copy(createdOrder = it)
                    },
                    onFailure = {
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.simple_payments_creation_error))
                    }
                )
            }
        }
    }

    @Parcelize
    data class ViewState(
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null
    ) : Parcelable
}
