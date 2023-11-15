package com.woocommerce.android.ui.payments.hub.depositsummary

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.util.FeatureFlag
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
import javax.inject.Inject

@HiltViewModel
class PaymentsHubDepositSummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PaymentsHubDepositSummaryRepository,
    private val mapper: PaymentsHubDepositSummaryStateMapper,
    isFeatureEnabled: IsFeatureEnabled,
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
        if (!isFeatureEnabled()) {
            _viewState.value = PaymentsHubDepositSummaryState.Error("Invalid data")
        } else {
            launch {
                repository.retrieveDepositOverview().map {
                    when (it) {
                        is RetrieveDepositOverviewResult.Cache ->
                            PaymentsHubDepositSummaryState.Success(
                                mapper.mapDepositOverviewToViewModelOverviews(it.overview)
                                    ?: return@map PaymentsHubDepositSummaryState.Error("Invalid data"),
                                onLearnMoreClicked = { onLearnMoreClicked() },
                                onExpandCollapseClicked = { onExpandCollapseClicked() }
                            )

                        is RetrieveDepositOverviewResult.Remote ->
                            PaymentsHubDepositSummaryState.Success(
                                mapper.mapDepositOverviewToViewModelOverviews(it.overview)
                                    ?: return@map PaymentsHubDepositSummaryState.Error("Invalid data"),
                                onLearnMoreClicked = { onLearnMoreClicked() },
                                onExpandCollapseClicked = { onExpandCollapseClicked() }
                            )

                        is RetrieveDepositOverviewResult.Error -> {
                            PaymentsHubDepositSummaryState.Error(it.error.message ?: "Unknown error")
                        }
                    }
                }.collect {
                    _viewState.value = it
                }
            }
        }
    }

    private fun onLearnMoreClicked() {
        launch {
            _openBrowserEvents.emit(LEARN_MORE_ABOUT_DEPOSIT_URL)
        }
    }

    private fun onExpandCollapseClicked() {
        // for the future tracking
    }

    private companion object {
        private const val LEARN_MORE_ABOUT_DEPOSIT_URL =
            "https://woocommerce.com/document/woopayments/deposits/deposit-schedule/"

        private const val LEARN_MORE_CLICKS_THROTTLING_DELAY = 500L
    }
}

class IsFeatureEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.DEPOSIT_SUMMARY.isEnabled()
}
