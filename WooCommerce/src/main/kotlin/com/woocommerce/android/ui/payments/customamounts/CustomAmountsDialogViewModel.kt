package com.woocommerce.android.ui.payments.customamounts

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CustomAmountsDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val orderCreateEditRepository: OrderCreateEditRepository,
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

    }

    fun onCancelDialogClicked() {
        // track cancel dialog clicked
    }

    @Parcelize
    data class ViewState(
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val customAmountName: String = "",
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null
    ) : Parcelable
}
