package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    private val ratesListHandler: TaxRateListHandler,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    val viewState: StateFlow<ViewState> = ratesListHandler.taxRatesFlow.map { rates ->
        rates.map { taxRate ->
            TaxRateUiModel(
                label = calculateTaxRateLabel(taxRate),
                rate = calculateTaxRatePercentageText(taxRate),
                taxRate = taxRate,
            )
        }.let {
            ViewState(it)
        }
    }.toStateFlow(ViewState())

    init {
        launch {
            ratesListHandler.fetchTaxRates()
        }
    }

    private fun calculateTaxRatePercentageText(taxRate: TaxRate) =
        if (taxRate.rate.isNotNullOrEmpty()) {
            "${taxRate.rate}%"
        } else {
            ""
        }

    private fun calculateTaxRateLabel(taxRate: TaxRate) =
        StringBuilder().apply {
            if (taxRate.name.isNotNullOrEmpty()) {
                append(taxRate.name)
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
            ratesListHandler.loadMore()
        }
    }

    @Parcelize
    data class ViewState(
        val taxRates: List<TaxRateUiModel> = emptyList(),
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
