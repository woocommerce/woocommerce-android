package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
    private val ratesListHandler: TaxRateListHandler,
    private val getTaxRateLabel: GetTaxRateLabel,
    private val getTaxRatePercentageValueText: GetTaxRatePercentageValueText,
    private val prefs: AppPrefs,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val isLoading = MutableStateFlow(false)
    private val autoRateSwitchState = savedState.getStateFlow(this, false, "autoRateSwitchState")
    val viewState: StateFlow<ViewState> = combine(
        ratesListHandler.taxRatesFlow,
        isLoading,
        autoRateSwitchState
    ) { rates, isLoading, autoRateSwitchState ->
        rates
            .filter { taxRate ->
                taxRate.city.isNotEmpty() ||
                    taxRate.stateCode.isNotEmpty() ||
                    taxRate.countryCode.isNotEmpty() ||
                    taxRate.postcode.isNotEmpty()
            } // Filter out tax rates with wildcard address
            .map { taxRate ->
                TaxRateUiModel(
                    label = getTaxRateLabel(taxRate),
                    rate = getTaxRatePercentageValueText(taxRate),
                    taxRate = taxRate
                )
            }
            .let {
                ViewState(taxRates = it, isLoading = isLoading, isAutoRateEnabled = autoRateSwitchState)
            }
    }.toStateFlow(ViewState())

    init {
        launch {
            isLoading.value = true
            ratesListHandler.fetchTaxRates()
            isLoading.value = false
        }
    }

    fun onEditTaxRatesInAdminClicked() {
        triggerEvent(EditTaxRatesInAdmin)
        tracker.track(AnalyticsEvent.TAX_RATE_SELECTOR_EDIT_IN_ADMIN_TAPPED)
    }

    fun onInfoIconClicked() {
        triggerEvent(ShowTaxesInfoDialog)
    }

    fun onTaxRateSelected(taxRate: TaxRateUiModel) {
        if (viewState.value.isAutoRateEnabled) {
            prefs.setAutoTaxRateId(taxRate.taxRate.id)
        } else {
            prefs.disableAutoTaxRate()
        }
        triggerEvent(TaxRateSelected(taxRate.taxRate))
        tracker.track(
            AnalyticsEvent.TAX_RATE_SELECTOR_TAX_RATE_TAPPED,
            mapOf(
                AnalyticsTracker.AUTO_TAX_RATE_ENABLED to autoRateSwitchState.value
            )
        )
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

    fun onAutoRateSwitchStateToggled() {
        autoRateSwitchState.value = !autoRateSwitchState.value
    }

    @Parcelize
    data class ViewState(
        val taxRates: List<TaxRateUiModel> = emptyList(),
        val isLoading: Boolean = false,
        val isEmpty: Boolean = taxRates.isEmpty() && !isLoading,
        val isAutoRateEnabled: Boolean = false
    ) : Parcelable

    @Parcelize
    data class TaxRateUiModel(
        val label: String,
        val rate: String,
        val taxRate: TaxRate
    ) : Parcelable

    data class TaxRateSelected(val taxRate: TaxRate) : MultiLiveEvent.Event()
    object EditTaxRatesInAdmin : MultiLiveEvent.Event()
    object ShowTaxesInfoDialog : MultiLiveEvent.Event()

    fun TaxRate.hasAddress(taxRate: TaxRate): Boolean {
        return taxRate.city.isNotEmpty() || taxRate.stateCode.isNotEmpty() ||
            taxRate.postcode.isNotEmpty() || taxRate.countryCode.isNotEmpty()
    }
}
