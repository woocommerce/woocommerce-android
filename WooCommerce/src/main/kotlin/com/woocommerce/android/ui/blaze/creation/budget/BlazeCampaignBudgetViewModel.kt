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
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAX_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.WEEKLY_DURATION
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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
            dailySpend = navArgs.budget.totalBudget / navArgs.budget.durationInDays,
            formattedTotalBudget = formatBudget(rawValue = navArgs.budget.totalBudget),
            formattedDailySpending = formatBudget(
                rawValue = navArgs.budget.totalBudget / navArgs.budget.durationInDays
            ),
            forecast = getLoadingForecastUi(),
            durationInDays = navArgs.budget.durationInDays,
            durationRangeMin = 1f,
            durationRangeMax = CAMPAIGN_MAX_DURATION.toFloat(),
            confirmedCampaignStartDateMillis = navArgs.budget.startDate.time,
            bottomSheetCampaignStartDateMillis = navArgs.budget.startDate.time,
            showImpressionsBottomSheet = false,
            showCampaignDurationBottomSheet = false,
            isEndlessCampaign = navArgs.budget.isEndlessCampaign,
            formattedStartDate = getFormattedStartDate(
                startDateMillis = navArgs.budget.startDate.time,
                isEndlessCampaign = navArgs.budget.isEndlessCampaign
            ),
            formattedEndDate = getFormattedEndDate(
                startDateMillis = navArgs.budget.startDate.time,
                duration = navArgs.budget.durationInDays
            )
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
                    isEndlessCampaign = budgetUiState.value.isEndlessCampaign
                )
            )
        )
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_EDIT_BUDGET_SAVE_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_DURATION to budgetUiState.value.durationInDays,
                AnalyticsTracker.KEY_BLAZE_TOTAL_BUDGET to budgetUiState.value.totalBudget,
                AnalyticsTracker.KEY_BLAZE_CAMPAIGN_TYPE to when {
                    budgetUiState.value.isEndlessCampaign -> AnalyticsTracker.VALUE_EVERGREEN_CAMPAIGN
                    else -> AnalyticsTracker.VALUE_START_END_CAMPAIGN
                },
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
                dailySpend = sliderValue,
                totalBudget = sliderValue * it.durationInDays,
                formattedTotalBudget = formatBudget(rawValue = sliderValue * it.durationInDays),
                formattedDailySpending = formatBudget(sliderValue)
            )
        }
    }

    fun onApplyDurationTapped(newDuration: Int, isEndlessCampaign: Boolean) {
        val duration = if (isEndlessCampaign) WEEKLY_DURATION else newDuration
        val totalBudget = duration * budgetUiState.value.dailySpend
        budgetUiState.update {
            it.copy(
                durationInDays = duration,
                formattedDailySpending = formatBudget(budgetUiState.value.dailySpend),
                totalBudget = totalBudget,
                formattedTotalBudget = formatBudget(rawValue = totalBudget),
                confirmedCampaignStartDateMillis = it.bottomSheetCampaignStartDateMillis,
                isEndlessCampaign = isEndlessCampaign,
                formattedStartDate = getFormattedStartDate(
                    startDateMillis = it.bottomSheetCampaignStartDateMillis,
                    isEndlessCampaign = isEndlessCampaign
                ),
                formattedEndDate = getFormattedEndDate(
                    startDateMillis = it.bottomSheetCampaignStartDateMillis,
                    duration = duration
                ),
            )
        }
        fetchAdForecast()
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_EDIT_BUDGET_SET_DURATION_APPLIED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_DURATION to budgetUiState.value.durationInDays,
                AnalyticsTracker.KEY_BLAZE_CAMPAIGN_TYPE to when {
                    isEndlessCampaign -> AnalyticsTracker.VALUE_EVERGREEN_CAMPAIGN
                    else -> AnalyticsTracker.VALUE_START_END_CAMPAIGN
                }
            )
        )
    }

    fun onStartDateChanged(newStartDateMillis: Long) {
        budgetUiState.update {
            it.copy(bottomSheetCampaignStartDateMillis = newStartDateMillis)
        }
    }

    fun onBudgetChangeFinished() {
        fetchAdForecast()
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

    private fun getFormattedStartDate(startDateMillis: Long, isEndlessCampaign: Boolean) =
        when {
            isEndlessCampaign -> Date(startDateMillis).formatToMMMddYYYY()
            else -> Date(startDateMillis).formatToMMMdd()
        }

    private fun getFormattedEndDate(startDateMillis: Long, duration: Int) =
        Date(startDateMillis + duration.days.inWholeMilliseconds).formatToMMMddYYYY()

    private fun formatBudget(rawValue: Float) =
        currencyFormatter.formatCurrencyRounded(rawValue.toDouble(), navArgs.budget.currencyCode)

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
        val dailySpend: Float,
        val formattedTotalBudget: String,
        val formattedDailySpending: String,
        val forecast: ForecastUi,
        val durationInDays: Int,
        val durationRangeMin: Float,
        val durationRangeMax: Float,
        val showImpressionsBottomSheet: Boolean,
        val showCampaignDurationBottomSheet: Boolean,
        val confirmedCampaignStartDateMillis: Long,
        val bottomSheetCampaignStartDateMillis: Long,
        val isEndlessCampaign: Boolean,
        val formattedStartDate: String,
        val formattedEndDate: String
    ) : Parcelable

    @Parcelize
    data class ForecastUi(
        val isLoading: Boolean,
        val impressionsMin: Long,
        val impressionsMax: Long,
        val isError: Boolean
    ) : Parcelable
}
