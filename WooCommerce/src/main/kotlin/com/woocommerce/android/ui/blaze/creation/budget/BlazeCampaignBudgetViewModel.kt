package com.woocommerce.android.ui.blaze.creation.budget

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
            startDate = Date(),
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

    data class BudgetUiState(
        val totalBudget: Float,
        val spentBudget: Float,
        val currencyCode: String,
        val durationInDays: Int,
        val startDate: Date,
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
