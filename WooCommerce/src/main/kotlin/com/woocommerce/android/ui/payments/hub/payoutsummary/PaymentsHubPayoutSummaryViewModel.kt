package com.woocommerce.android.ui.payments.hub.payoutsummary

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
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject

@HiltViewModel
class PaymentsHubPayoutSummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PaymentsHubPayoutSummaryRepository,
    private val mapper: PaymentsHubPayoutSummaryStateMapper,
    private val trackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val _viewState = MutableStateFlow<PaymentsHubPayoutSummaryState>(PaymentsHubPayoutSummaryState.Loading)
    val viewState: LiveData<PaymentsHubPayoutSummaryState> = _viewState.asLiveData()

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
            repository.retrievePayoutOverview().map {
                when (it) {
                    is RetrievePayoutOverviewResult.Cache ->
                        buildPayoutSummaryState(
                            overview = it.overview,
                            fromCache = true
                        )

                    is RetrievePayoutOverviewResult.Remote -> {
                        buildPayoutSummaryState(
                            overview = it.overview,
                            fromCache = false
                        )
                    }

                    is RetrievePayoutOverviewResult.Error -> {
                        PaymentsHubPayoutSummaryState.Error(it.error)
                    }
                }
            }.collect {
                if (it is PaymentsHubPayoutSummaryState.Error) {
                    trackApiError(it)
                }
                _viewState.value = it
            }
        }
    }

    fun onSummaryPayoutShown() {
        val success = _viewState.value as? PaymentsHubPayoutSummaryState.Success ?: return
        trackerWrapper.track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN,
            properties = mapOf(
                NUMBER_OF_CURRENCIES_TRACK_PROP_KEY to success.overview.infoPerCurrency.size.toString(),
            )
        )
    }

    private fun buildPayoutSummaryState(
        overview: WooPaymentsDepositsOverview,
        fromCache: Boolean
    ): PaymentsHubPayoutSummaryState =
        when (val mappingResult = mapper.mapPayoutOverviewToViewModelOverviews(overview)) {
            PaymentsHubPayoutSummaryStateMapper.Result.InvalidInputData -> constructApiError()
            is PaymentsHubPayoutSummaryStateMapper.Result.Success -> {
                PaymentsHubPayoutSummaryState.Success(
                    overview = mappingResult.overview,
                    fromCache = fromCache,
                    onLearnMoreClicked = { onLearnMoreClicked() },
                    onExpandCollapseClicked = { expanded -> onExpandCollapseClicked(expanded) },
                    onCurrencySelected = { currency -> onCurrencySelected(currency) }
                )
            }
        }

    private fun onLearnMoreClicked() {
        trackerWrapper.track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_LEARN_MORE_CLICKED)
        launch {
            _openBrowserEvents.emit(LEARN_MORE_ABOUT_PAYOUT_URL)
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

    private fun onExpandCollapseClicked(expanded: Boolean) {
        if (expanded) {
            trackerWrapper.track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_EXPANDED)
        }
    }

    private fun trackApiError(error: PaymentsHubPayoutSummaryState.Error) {
        trackerWrapper.track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_ERROR,
            errorContext = this@PaymentsHubPayoutSummaryViewModel.javaClass.simpleName,
            errorType = error.error.type.name,
            errorDescription = error.error.message
        )
    }

    private fun constructApiError() = PaymentsHubPayoutSummaryState.Error(
        WooError(
            WooErrorType.API_ERROR,
            BaseRequest.GenericErrorType.UNKNOWN,
            "Invalid data"
        )
    )

    private companion object {
        private const val LEARN_MORE_ABOUT_PAYOUT_URL =
            "https://woocommerce.com/document/woopayments/payouts/payout-schedule/"

        private const val NUMBER_OF_CURRENCIES_TRACK_PROP_KEY = "number_of_currencies"
        private const val LEARN_MORE_CLICKS_THROTTLING_DELAY = 500L
    }
}
