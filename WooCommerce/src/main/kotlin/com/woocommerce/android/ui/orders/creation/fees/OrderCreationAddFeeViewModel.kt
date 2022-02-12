package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.AMOUNT
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.PERCENTAGE
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

private const val DEFAULT_DECIMAL_PRECISION = 2

@HiltViewModel
class OrderCreationAddFeeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val currencyDecimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())
            ?.currencyDecimalNumber
            ?: DEFAULT_DECIMAL_PRECISION

    private val currentFeeType
        get() = when (viewState.isPercentageSelected) {
            true -> PERCENTAGE
            false -> AMOUNT
        }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(viewState.feeInputValue, currentFeeType))
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        viewState = viewState.copy(isPercentageSelected = isChecked)
        if(isChecked) triggerEvent(DisplayPercentageMode)
        else triggerEvent(DisplayAmountMode)
    }

    fun onFeeInputValueChanged(inputValue: BigDecimal) {
        viewState = viewState.copy(feeInputValue = inputValue)
    }

    @Parcelize
    data class ViewState(
        val feeInputValue: BigDecimal = BigDecimal.ZERO,
        val isPercentageSelected: Boolean = false
    ) : Parcelable

    enum class FeeType {
        AMOUNT, PERCENTAGE
    }

    data class UpdateFee(
        val amount: BigDecimal,
        val feeType: FeeType
    ) : Event()

    object DisplayPercentageMode : Event()
    object DisplayAmountMode : Event()
}
