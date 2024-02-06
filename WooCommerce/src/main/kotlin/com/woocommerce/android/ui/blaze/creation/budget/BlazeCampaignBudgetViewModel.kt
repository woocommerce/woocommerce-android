package com.woocommerce.android.ui.blaze.creation.budget

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.BLAZE_DEFAULT_CURRENCY_CODE
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAXIMUM_DAILY_SPEND_LIMIT
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAX_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND_LIMIT
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.DEFAULT_CAMPAIGN_DURATION
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class BlazeCampaignBudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData(
        BudgetUiState(
            currencyCode = BLAZE_DEFAULT_CURRENCY_CODE,
            totalBudget = CAMPAIGN_MINIMUM_DAILY_SPEND_LIMIT * DEFAULT_CAMPAIGN_DURATION,
            sliderValue = CAMPAIGN_MINIMUM_DAILY_SPEND_LIMIT * DEFAULT_CAMPAIGN_DURATION,
            spentBudget = 0f,
            budgetRange = getBudgetRange(DEFAULT_CAMPAIGN_DURATION),
            dailySpending = formatDailySpend(
                dailySpend = (CAMPAIGN_MINIMUM_DAILY_SPEND_LIMIT * DEFAULT_CAMPAIGN_DURATION) / DEFAULT_CAMPAIGN_DURATION
            ),
            durationInDays = DEFAULT_CAMPAIGN_DURATION,
            durationRangeDays = getDurationRange(),
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

    fun onBudgetUpdated(sliderValue: Float) {
        if (sliderValue.toInt().mod(viewState.value?.durationInDays!!) == 0) {
            val dailySpending = sliderValue / viewState.value?.durationInDays!!
            _viewState.value = _viewState.value?.copy(
                totalBudget = sliderValue,
                dailySpending = formatDailySpend(dailySpending)
            )
        }
        _viewState.value = _viewState.value?.copy(
            sliderValue = sliderValue,
        )
    }

    fun onCampaignDurationUpdated(duration: Int) {
        val currentDailyExpend = viewState.value?.totalBudget!! / viewState.value?.durationInDays!!
        val newTotalBudget = duration * currentDailyExpend
        _viewState.value = _viewState.value?.copy(
            durationInDays = duration,
            budgetRange = getBudgetRange(duration),
            dailySpending = formatDailySpend(currentDailyExpend),
            totalBudget = newTotalBudget,
            sliderValue = newTotalBudget,
        )
    }

    private fun formatDailySpend(dailySpend: Float) =
        currencyFormatter.formatCurrency(dailySpend.roundToInt().toBigDecimal(), BLAZE_DEFAULT_CURRENCY_CODE)

    private fun getBudgetRange(duration: Int) =
        duration * CAMPAIGN_MINIMUM_DAILY_SPEND_LIMIT..duration * CAMPAIGN_MAXIMUM_DAILY_SPEND_LIMIT

    private fun getDurationRange() = 1f..CAMPAIGN_MAX_DURATION.toFloat()

    data class BudgetUiState(
        val currencyCode: String,
        val totalBudget: Float,
        val sliderValue: Float,
        val spentBudget: Float,
        val budgetRange: ClosedFloatingPointRange<Float>,
        val dailySpending: String,
        val durationInDays: Int,
        val durationRangeDays: ClosedFloatingPointRange<Float>,
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
