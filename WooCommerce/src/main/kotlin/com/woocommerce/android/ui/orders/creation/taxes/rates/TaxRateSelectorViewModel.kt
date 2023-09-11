package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    private val ratesListHandler: TaxRateListHandler,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val isLoading = MutableStateFlow(false)
    val viewState: StateFlow<ViewState> =
        ratesListHandler.taxRatesFlow.combine(isLoading) { rates, isLoading ->
            rates.map { taxRate ->
                TaxRateUiModel(
                    label = calculateTaxRateLabel(taxRate),
                    rate = calculateTaxRatePercentageText(taxRate),
                    taxRate = taxRate,
                )
            }.let {
                ViewState(taxRates = it, isLoading = isLoading)
            }
        }.toStateFlow(ViewState())

    init {
        launch {
            isLoading.value = true
            ratesListHandler.fetchTaxRates()
            isLoading.value = false
        }
    }

    private fun calculateTaxRatePercentageText(taxRate: TaxRate) =
        if (taxRate.rate.isNotNullOrEmpty()) {
            val standardisedRate = taxRate.rate.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
            "$standardisedRate%"
        } else {
            ""
        }

    @Suppress("ComplexCondition")
    private fun calculateTaxRateLabel(taxRate: TaxRate) =
        StringBuilder().apply {
            if (taxRate.name.isNotNullOrEmpty()) {
                append(taxRate.name)
            }
            if (taxRate.countryCode.isNotNullOrEmpty() ||
                taxRate.stateCode.isNotNullOrEmpty() ||
                taxRate.postcode.isNotNullOrEmpty() ||
                taxRate.city.isNotNullOrEmpty()
            ) {
                append(" · ")
            }
            if (taxRate.countryCode.isNotNullOrEmpty()) {
                append(taxRate.countryCode)
                append(SPACE_CHAR)
            }
            if (taxRate.stateCode.isNotNullOrEmpty()) {
                append(taxRate.stateCode)
                append(SPACE_CHAR)
            }
            if (taxRate.postcode.isNotNullOrEmpty()) {
                append(taxRate.postcode)
                append(SPACE_CHAR)
            }
            if (taxRate.city.isNotNullOrEmpty()) {
                append(taxRate.city)
            }
        }.toString()

    fun onEditTaxRatesInAdminClicked() {
        triggerEvent(EditTaxRatesInAdmin)
    }
    fun onInfoIconClicked() {
        triggerEvent(ShowTaxesInfoDialog)
    }

    fun onTaxRateSelected(taxRate: TaxRateUiModel) {
        triggerEvent(TaxRateSelected(taxRate.taxRate))
    }

    fun onDismissed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onLoadMore() {
        launch {
            isLoading.value = true
            ratesListHandler.loadMore()
            isLoading.value = false
        }
    }

    @Parcelize
    data class ViewState(
        val taxRates: List<TaxRateUiModel> = emptyList(),
        val isLoading: Boolean = false,
        val isEmpty: Boolean = taxRates.isEmpty() && !isLoading,
    ) : Parcelable

    @Parcelize
    data class TaxRateUiModel(
        val label: String,
        val rate: String,
        val taxRate: TaxRate,
    ) : Parcelable

    data class TaxRateSelected(val taxRate: TaxRate) : MultiLiveEvent.Event()
    object EditTaxRatesInAdmin : MultiLiveEvent.Event()
    object ShowTaxesInfoDialog : MultiLiveEvent.Event()

    private companion object {
        private const val SPACE_CHAR = " "
    }
}
