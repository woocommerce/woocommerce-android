package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationEditFeeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private val PERCENTAGE_BASE = BigDecimal(100)
    }

    private val navArgs: OrderCreationEditFeeFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val currencyDecimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())
            ?.currencyDecimalNumber
            ?: DEFAULT_DECIMAL_PRECISION

    private val activeFeeValue
        get() = when (viewState.isPercentageSelected) {
            true -> (navArgs.orderTotal * viewState.feePercentage) / PERCENTAGE_BASE
            false -> viewState.feeAmount
        }

    private val appliedPercentageFromCurrentFeeValue
        get() = navArgs.currentFeeValue
            ?.takeIf { navArgs.orderTotal > BigDecimal.ZERO }
            ?.let { it.divide(navArgs.orderTotal) * PERCENTAGE_BASE }
            ?: BigDecimal.ZERO

    fun start() {
        navArgs.currentFeeValue?.let {
            viewState = viewState.copy(
                feeAmount = it,
                feePercentage = appliedPercentageFromCurrentFeeValue,
                shouldDisplayRemoveFeeButton = true
            )
        }
    }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(activeFeeValue))
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        viewState = viewState.copy(isPercentageSelected = isChecked)
    }

    fun onFeeAmountChanged(feeAmount: BigDecimal) {
        viewState = viewState.copy(feeAmount = feeAmount)
    }

    fun onFeePercentageChanged(feePercentage: String) {
        viewState = viewState.copy(
            feePercentage = feePercentage.toBigDecimalOrNull() ?: BigDecimal.ZERO
        )
    }

    @Parcelize
    data class ViewState(
        val feeAmount: BigDecimal = BigDecimal.ZERO,
        val feePercentage: BigDecimal = BigDecimal.ZERO,
        val isPercentageSelected: Boolean = false,
        val shouldDisplayRemoveFeeButton: Boolean = false
    ) : Parcelable

    data class UpdateFee(
        val amount: BigDecimal
    ) : Event()
}
