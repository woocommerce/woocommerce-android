package com.woocommerce.android.ui.blaze.creation.budget

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_BUDGET_SAVE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_BUDGET_SET_DURATION_APPLIED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAXIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAX_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltViewModel
class BlazeCampaignBudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val repository: BlazeRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_DATE_LIMIT_IN_DAYS = 60
    }

    private val navArgs: BlazeCampaignBudgetFragmentArgs by savedStateHandle.navArgs()

    private val budgetUiState = savedStateHandle.getStateFlow(
        viewModelScope,
        BudgetUiState(
            currencyCode = navArgs.budget.currencyCode,
            totalBudget = navArgs.budget.totalBudget,
            budgetRangeMin = navArgs.budget.durationInDays * CAMPAIGN_MINIMUM_DAILY_SPEND,
            budgetRangeMax = navArgs.budget.durationInDays * CAMPAIGN_MAXIMUM_DAILY_SPEND,
            dailySpending = formatDailySpend(
                dailySpend = navArgs.budget.totalBudget / navArgs.budget.durationInDays
            ),
            forecast = getLoadingForecastUi(),
            durationInDays = navArgs.budget.durationInDays,
            durationRangeMin = 1f,
            durationRangeMax = CAMPAIGN_MAX_DURATION.toFloat(),
            confirmedCampaignStartDateMillis = navArgs.budget.startDate.time,
            bottomSheetCampaignStartDateMillis = navArgs.budget.startDate.time,
            campaignDurationDates = getCampaignDurationDisplayDate(
                navArgs.budget.startDate.time,
                navArgs.budget.durationInDays
            ),
            showImpressionsBottomSheet = false,
            showCampaignDurationBottomSheet = false
        )
    )

    private var campaignForecastState
        get() = budgetUiState.value.forecast
        set(value) {
            budgetUiState.update { it.copy(forecast = value) }
        }

    val viewState = budgetUiState.asLiveData()

    init {
        fetchAdForecast()
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onUpdateTapped() {
        triggerEvent(
            ExitWithResult(
                Budget(
                    totalBudget = budgetUiState.value.totalBudget,
                    spentBudget = 0f,
                    durationInDays = budgetUiState.value.durationInDays,
                    startDate = Date(budgetUiState.value.confirmedCampaignStartDateMillis),
                    currencyCode = budgetUiState.value.currencyCode,
                )
            )
        )
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_EDIT_BUDGET_SAVE_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_DURATION to budgetUiState.value.durationInDays,
                AnalyticsTracker.KEY_BLAZE_TOTAL_BUDGET to budgetUiState.value.totalBudget,
            )
        )
    }

    fun onEditDurationTapped() {
        budgetUiState.update {
            it.copy(
                showCampaignDurationBottomSheet = true,
                showImpressionsBottomSheet = false
            )
        }
    }

    fun onImpressionsInfoTapped() {
        budgetUiState.update {
            it.copy(
                showImpressionsBottomSheet = true,
                showCampaignDurationBottomSheet = false
            )
        }
    }

    fun onBudgetUpdated(sliderValue: Float) {
        budgetUiState.update {
            it.copy(
                totalBudget = sliderValue,
                dailySpending = formatDailySpend(sliderValue / it.durationInDays)
            )
        }
    }

    fun onApplyDurationTapped(newDuration: Int) {
        val currentDailySpend = calculateDailySpending(newDuration)
        budgetUiState.update {
            it.copy(
                durationInDays = newDuration,
                budgetRangeMin = newDuration * CAMPAIGN_MINIMUM_DAILY_SPEND,
                budgetRangeMax = newDuration * CAMPAIGN_MAXIMUM_DAILY_SPEND,
                dailySpending = formatDailySpend(currentDailySpend),
                totalBudget = newDuration * currentDailySpend,
                confirmedCampaignStartDateMillis = it.bottomSheetCampaignStartDateMillis,
                campaignDurationDates = getCampaignDurationDisplayDate(
                    it.bottomSheetCampaignStartDateMillis,
                    newDuration
                )
            )
        }
        fetchAdForecast()
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_EDIT_BUDGET_SET_DURATION_APPLIED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_DURATION to budgetUiState.value.durationInDays,
            )
        )
    }

    fun onStartDateChanged(newStartDateMillis: Long) {
        budgetUiState.update {
            it.copy(
                bottomSheetCampaignStartDateMillis = newStartDateMillis,
                campaignDurationDates = getCampaignDurationDisplayDate(
                    newStartDateMillis,
                    it.durationInDays
                )
            )
        }
    }

    fun onBudgetChangeFinished() {
        fetchAdForecast()
        val roundedBudgetToDurationMultiple =
            calculateDailySpending(budgetUiState.value.durationInDays) * budgetUiState.value.durationInDays
        budgetUiState.update {
            it.copy(totalBudget = roundedBudgetToDurationMultiple)
        }
    }

    private fun calculateDailySpending(duration: Int): Float {
        val dailySpend = budgetUiState.value.totalBudget / duration
        return dailySpend.coerceIn(CAMPAIGN_MINIMUM_DAILY_SPEND, CAMPAIGN_MAXIMUM_DAILY_SPEND)
    }

    private fun fetchAdForecast() {
        campaignForecastState = campaignForecastState.copy(isLoading = true)
        launch {
            repository.fetchAdForecast(
                startDate = Date(budgetUiState.value.confirmedCampaignStartDateMillis),
                campaignDurationDays = budgetUiState.value.durationInDays,
                totalBudget = budgetUiState.value.totalBudget,
                targetingParameters = navArgs.targetingParameters
            ).onSuccess { fetchAdForecastResult ->
                campaignForecastState = campaignForecastState.copy(
                    isLoading = false,
                    isError = false,
                    impressionsMin = fetchAdForecastResult.minImpressions,
                    impressionsMax = fetchAdForecastResult.maxImpressions
                )
            }.onFailure {
                campaignForecastState = campaignForecastState.copy(
                    isLoading = false,
                    isError = true
                )
            }
        }
    }

    private fun getCampaignDurationDisplayDate(startDateMillis: Long, duration: Int): String {
        val endDate = Date(startDateMillis + duration.days.inWholeMilliseconds)
        return "${Date(startDateMillis).formatToMMMdd()} - ${endDate.formatToMMMddYYYY()}"
    }

    private fun formatDailySpend(dailySpend: Float) =
        currencyFormatter.formatCurrencyRounded(dailySpend.toDouble(), navArgs.budget.currencyCode)

    private fun getLoadingForecastUi() = ForecastUi(
        isLoading = true,
        impressionsMin = 0,
        impressionsMax = 0,
        isError = false
    )

    @Parcelize
    data class BudgetUiState(
        val currencyCode: String,
        val totalBudget: Float,
        val budgetRangeMin: Float,
        val budgetRangeMax: Float,
        val dailySpending: String,
        val forecast: ForecastUi,
        val durationInDays: Int,
        val durationRangeMin: Float,
        val durationRangeMax: Float,
        val showImpressionsBottomSheet: Boolean,
        val showCampaignDurationBottomSheet: Boolean,
        val confirmedCampaignStartDateMillis: Long,
        val bottomSheetCampaignStartDateMillis: Long,
        val campaignDurationDates: String,
    ) : Parcelable

    @Parcelize
    data class ForecastUi(
        val isLoading: Boolean,
        val impressionsMin: Long,
        val impressionsMax: Long,
        val isError: Boolean
    ) : Parcelable
}
