package com.woocommerce.android.ui.payments.customamounts

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CustomAmountsDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData
    var currentPrice: BigDecimal
        get() = viewState.customAmountUIModel.currentPrice
        set(value) {
            viewState = viewState.copy(
                isDoneButtonEnabled = value > BigDecimal.ZERO,
                customAmountUIModel = viewState.customAmountUIModel.copy(
                    currentPrice = value
                )
            )
        }

    var currentName: String
        get() = viewState.customAmountUIModel.name
        set(value) {
            viewState = viewState.copy(
                customAmountUIModel = viewState.customAmountUIModel.copy(
                    name = value
                )
            )
        }

    fun onCancelDialogClicked() {
        // track cancel dialog clicked
    }

    @Parcelize
    data class ViewState(
        val customAmountUIModel: CustomAmountUIState,
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null
    ) : Parcelable

    @Parcelize
    data class CustomAmountUIState(
        val id: Long = 0,
        val currentPrice: BigDecimal,
        val name: String
    ) : Parcelable
}
