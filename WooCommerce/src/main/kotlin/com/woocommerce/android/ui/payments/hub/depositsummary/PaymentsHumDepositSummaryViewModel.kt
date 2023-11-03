package com.woocommerce.android.ui.payments.hub.depositsummary

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PaymentsHumDepositSummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PaymentsHubDepositSummaryRepository,
) : ScopedViewModel(savedState) {

    private val _viewState = MutableStateFlow<PaymentsHubDepositSummaryState>(PaymentsHubDepositSummaryState.Loading)
    val viewState: LiveData<PaymentsHubDepositSummaryState> = _viewState.asLiveData()

    init {
        launch {
            repository.retrieveDepositOverview().map {
                when (it) {
                    is RetrieveDepositOverviewResult.Cache ->
                    is RetrieveDepositOverviewResult.Error -> TODO()
                    is RetrieveDepositOverviewResult.Remote -> TODO()
                }
            }.collect {
                _viewState.value = it
            }
        }
    }
}

