package com.woocommerce.android.ui.payments.customamounts

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CustomAmountsDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    tracker: AnalyticsTrackerWrapper,
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

    var taxToggleState: TaxStatus
        get() = viewState.customAmountUIModel.taxStatus
        set(value) {
            viewState = viewState.copy(
                customAmountUIModel = viewState.customAmountUIModel.copy(
                    taxStatus = viewState.customAmountUIModel.taxStatus.copy(
                        isTaxable = value.isTaxable
                    )
                )
            )
        }

    private val args: CustomAmountsDialogArgs by savedState.navArgs()

    init {
        args.customAmountUIModel?.let {
            // Edit mode
            currentPrice = it.amount
            viewState = viewState.copy(
                customAmountUIModel = viewState.customAmountUIModel.copy(
                    id = it.id,
                    name = it.name,
                    taxStatus = it.taxStatus
                )
            )
            tracker.track(ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED)
        } ?: run {
            tracker.track(ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED)
        }
    }
    @Parcelize
    data class ViewState(
        val customAmountUIModel: CustomAmountUIState = CustomAmountUIState(),
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null,
    ) : Parcelable

    @Parcelize
    data class CustomAmountUIState(
        val id: Long = 0,
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val name: String = "",
        val taxStatus: TaxStatus = TaxStatus()
    ) : Parcelable

    @Parcelize
    data class TaxStatus(
        val isTaxable: Boolean = false,
        val text: Int = R.string.custom_amounts_tax_label,
    ) : Parcelable
}
