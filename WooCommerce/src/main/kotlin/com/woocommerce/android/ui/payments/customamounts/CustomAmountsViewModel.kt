package com.woocommerce.android.ui.payments.customamounts

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CustomAmountsViewModel @Inject constructor(
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

    var currentPercentage: BigDecimal
        get() {
            val orderTotal = BigDecimal(args.orderTotal ?: "0")
            return if (orderTotal > BigDecimal.ZERO) {
                (viewState.customAmountUIModel.currentPrice.divide(orderTotal, 2, RoundingMode.HALF_UP)).multiply(
                    BigDecimal(PERCENTAGE_SCALE_FACTOR)
                )
            } else {
                BigDecimal.ZERO
            }
        }
        set(value) {
            val totalAmount = BigDecimal(args.orderTotal ?: "0")

            if (totalAmount > BigDecimal.ZERO) {
                val percentage = value.toString().toDouble().roundToInt()
                val updatedAmount = (
                    totalAmount.multiply(BigDecimal(percentage))
                        .divide(BigDecimal(PERCENTAGE_SCALE_FACTOR), 2, RoundingMode.HALF_UP)
                    )
                viewState = viewState.copy(
                    isDoneButtonEnabled = value > BigDecimal.ZERO,
                    customAmountUIModel = viewState.customAmountUIModel.copy(
                        currentPrice = updatedAmount,
                    )
                )
            }
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

    private val args: CustomAmountsFragmentArgs by savedState.navArgs()

    init {
        if (isInCreateMode()) {
            tracker.track(ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED)
        } else {
            // Edit mode
            populateUIWithExistingData()
            tracker.track(ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED)
        }
        updateCustomAmountType()
    }

    private fun updateCustomAmountType() {
        viewState = viewState.copy(
            customAmountUIModel = viewState.customAmountUIModel.copy(
                type = args.customAmountUIModel.type
            )
        )
    }

    private fun populateUIWithExistingData() {
        args.customAmountUIModel.apply {
            val orderTotalValue = BigDecimal(args.orderTotal ?: "0")
            if (orderTotalValue > BigDecimal.ZERO) {
                populatePercentage(this)
                when (type) {
                    CustomAmountType.FIXED_CUSTOM_AMOUNT -> {
                        currentPrice = amount
                    }

                    CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT -> {
                        currentPercentage = (amount.divide(orderTotalValue, 2, RoundingMode.HALF_UP))
                            .multiply(BigDecimal(PERCENTAGE_SCALE_FACTOR))
                    }
                }
            }

            viewState = viewState.copy(
                customAmountUIModel = viewState.customAmountUIModel.copy(
                    id = id,
                    name = name,
                    taxStatus = taxStatus,
                    currentPrice = amount,
                    type = type
                )
            )
        }
    }

    private fun populatePercentage(customAmountUIModel: CustomAmountUIModel) {
        triggerEvent(PopulatePercentage(customAmountUIModel))
    }

    fun isInCreateMode() = args.customAmountUIModel.amount.compareTo(BigDecimal.ZERO) == 0

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
        val taxStatus: TaxStatus = TaxStatus(),
        val type: CustomAmountType = CustomAmountType.FIXED_CUSTOM_AMOUNT,
    ) : Parcelable

    @Parcelize
    data class TaxStatus(
        val isTaxable: Boolean = false,
        val text: Int = R.string.custom_amounts_tax_label,
    ) : Parcelable

    enum class CustomAmountType {
        FIXED_CUSTOM_AMOUNT,
        PERCENTAGE_CUSTOM_AMOUNT
    }

    data class PopulatePercentage(val customAmountUIModel: CustomAmountUIModel) : Event()

    companion object {
        const val PERCENTAGE_SCALE_FACTOR = 100
    }
}
