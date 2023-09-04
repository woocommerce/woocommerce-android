package com.woocommerce.android.ui.orders.creation.taxes.rates

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    repository: TaxRateRepository,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val _viewState: MutableStateFlow<ViewState> =
        savedState.getStateFlow(
            scope = this,
            initialValue = ViewState(),
            key = "view_state"
        )
    val viewState: StateFlow<ViewState> = _viewState

    init {
        launch {
            repository.fetchTaxRates().let { taxRates ->
                _viewState.value = _viewState.value.copy(
                    taxRates = taxRates.map { taxRate ->
                        TaxRateUiModel(
                            label = calculateTaxRateLabel(taxRate),
                            rate = calculateTaxRatePercentageText(taxRate),
                        )
                    }
                )
            }
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

    fun onEditTaxRatesInAdminClicked() = Unit
    fun onInfoIconClicked() = Unit

    @Suppress("UNUSED_PARAMETER")
    fun onTaxRateSelected(taxRate: TaxRateUiModel) = Unit

    data class ViewState(
        val taxRates: List<TaxRateUiModel> = emptyList(),
    )

    data class TaxRateUiModel(
        val label: String,
        val rate: String,
    )

    private companion object {
        private const val SPACE_CHAR = " "
    }
}
