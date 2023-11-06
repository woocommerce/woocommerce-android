package com.woocommerce.android.ui.payments.hub.depositsummary

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PaymentsHumDepositSummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PaymentsHubDepositSummaryRepository,
    private val mapper: PaymentsHumDepositSummaryStateMapper,
    private val isFeatureEnabled: IsFeatureEnabled,
) : ScopedViewModel(savedState) {

    private val _viewState = MutableStateFlow<PaymentsHubDepositSummaryState>(PaymentsHubDepositSummaryState.Loading)
    val viewState: LiveData<PaymentsHubDepositSummaryState> = _viewState.asLiveData()

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
                                    ?: return@map PaymentsHubDepositSummaryState.Error("Invalid data")
                            )

                        is RetrieveDepositOverviewResult.Remote ->
                            PaymentsHubDepositSummaryState.Success(
                                mapper.mapDepositOverviewToViewModelOverviews(it.overview)
                                    ?: return@map PaymentsHubDepositSummaryState.Error("Invalid data")
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
}

class IsFeatureEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.DEPOSIT_SUMMARY.isEnabled()
}
