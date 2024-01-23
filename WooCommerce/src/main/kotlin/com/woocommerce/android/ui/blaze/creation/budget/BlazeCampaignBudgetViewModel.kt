package com.woocommerce.android.ui.blaze.creation.budget

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignBudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData(
        BudgetUiState(
            totalBudget = 35f,
            spentBudget = 0f,
            currencyCode = "USD",
            durationInDays = 7,
            startDateMmmDdYyyy = Date().formatToMMMddYYYY(),
            forecast = ForecastUi(
                isLoaded = false,
                impressionsMin = 0,
                impressionsMax = 0
            )
        )
    )
    val viewState = _viewState

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onEditDurationTapped() {
        _viewState.value = _viewState.value?.copy(
            showCampaignDurationBottomSheet = true,
            showImpressionsBottomSheet = false
        )
    }

    fun onImpressionsInfoTapped() {
        _viewState.value = _viewState.value?.copy(
            showImpressionsBottomSheet = true,
            showCampaignDurationBottomSheet = false
        )
    }

    fun onTotalBudgetUpdated(totalBudget: Float) {
        _viewState.value = _viewState.value?.copy(totalBudget = totalBudget)
    }

    data class BudgetUiState(
        val totalBudget: Float,
        val spentBudget: Float,
        val currencyCode: String,
        val durationInDays: Int,
        val startDateMmmDdYyyy: String,
        val forecast: ForecastUi,
        val showImpressionsBottomSheet: Boolean = false,
        val showCampaignDurationBottomSheet: Boolean = false,
    )

    data class ForecastUi(
        val isLoaded: Boolean = false,
        val impressionsMin: Int,
        val impressionsMax: Int
    )
}
