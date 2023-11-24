package com.woocommerce.android.ui.payments.hub.depositsummary

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CURRENCY
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject

@HiltViewModel
class PaymentsHubDepositSummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PaymentsHubDepositSummaryRepository,
    private val mapper: PaymentsHubDepositSummaryStateMapper,
    private val trackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val _viewState = MutableStateFlow<PaymentsHubDepositSummaryState>(PaymentsHubDepositSummaryState.Loading)
    val viewState: LiveData<PaymentsHubDepositSummaryState> = _viewState.asLiveData()

    private val _openBrowserEvents = MutableSharedFlow<String>()
    val openBrowserEvents = _openBrowserEvents
        .asSharedFlow()
        .conflate()
        .transform {
            emit(it)
            delay(LEARN_MORE_CLICKS_THROTTLING_DELAY)
        }

    init {
        launch {
            repository.retrieveDepositOverview().map {
                when (it) {
                    is RetrieveDepositOverviewResult.Cache ->
                        PaymentsHubDepositSummaryState.Success(
                            mapper.mapDepositOverviewToViewModelOverviews(it.overview)
                                ?: return@map constructApiError(),
                            fromCache = true,
                            onLearnMoreClicked = { onLearnMoreClicked() },
                            onExpandCollapseClicked = { expanded -> onExpandCollapseClicked(expanded) },
                            onCurrencySelected = { currency -> onCurrencySelected(currency) }
                        )

                    is RetrieveDepositOverviewResult.Remote -> {
                        PaymentsHubDepositSummaryState.Success(
                            mapper.mapDepositOverviewToViewModelOverviews(it.overview)
                                ?: return@map constructApiError(),
                            onLearnMoreClicked = { onLearnMoreClicked() },
                            onExpandCollapseClicked = { expanded -> onExpandCollapseClicked(expanded) },
                            onCurrencySelected = { currency -> onCurrencySelected(currency) },
                            fromCache = false,
                        )
                    }

                    is RetrieveDepositOverviewResult.Error -> {
                        PaymentsHubDepositSummaryState.Error(it.error)
                    }
                }
            }.collect {
                if (it is PaymentsHubDepositSummaryState.Error) {
                    trackApiError(it)
                }
                _viewState.value = it
            }
        }
    }

    private fun onCurrencySelected(currency: String) {
        trackerWrapper.track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_CURRENCY_SELECTED,
            properties = mapOf(
                KEY_CURRENCY to currency.lowercase()
            )
        )
    }

    fun onSummaryDepositShown() {
        val success = _viewState.value as? PaymentsHubDepositSummaryState.Success ?: return
        trackerWrapper.track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN,
            properties = mapOf(
                NUMBER_OF_CURRENCIES_TRACK_PROP_KEY to success.overview.infoPerCurrency.size.toString(),
            )
        )
    }

    private fun onLearnMoreClicked() {
        trackerWrapper.track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_LEARN_MORE_CLICKED)
        launch {
            _openBrowserEvents.emit(LEARN_MORE_ABOUT_DEPOSIT_URL)
        }
    }

    private fun onExpandCollapseClicked(expanded: Boolean) {
        if (expanded) {
            trackerWrapper.track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_EXPANDED)
        }
    }

    private fun trackApiError(error: PaymentsHubDepositSummaryState.Error) {
        trackerWrapper.track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_ERROR,
            errorContext = this@PaymentsHubDepositSummaryViewModel.javaClass.simpleName,
            errorType = error.error.type.name,
            errorDescription = error.error.message
        )
    }

    private fun constructApiError() = PaymentsHubDepositSummaryState.Error(
        WooError(
            WooErrorType.API_ERROR,
            BaseRequest.GenericErrorType.UNKNOWN,
            "Invalid data"
        )
    )

    private companion object {
        private const val LEARN_MORE_ABOUT_DEPOSIT_URL =
            "https://woocommerce.com/document/woopayments/deposits/deposit-schedule/"

        private const val NUMBER_OF_CURRENCIES_TRACK_PROP_KEY = "number_of_currencies"
        private const val LEARN_MORE_CLICKS_THROTTLING_DELAY = 500L
    }
}
