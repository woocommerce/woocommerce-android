package com.woocommerce.android.ui.blaze.creation.budget

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAXIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAX_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.ONE_DAY_IN_MILLIS
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignBudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val repository: BlazeRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignBudgetFragmentArgs by savedStateHandle.navArgs()

    private val campaignForecastState = savedStateHandle.getStateFlow(viewModelScope, getLoadingForecastUi())
    private val budgetUiState = savedStateHandle.getStateFlow(
        viewModelScope,
        BudgetUiState(
            currencyCode = navArgs.currencyCode,
            totalBudget = navArgs.totalBudget,
            sliderValue = navArgs.totalBudget,
            budgetRangeMin = navArgs.durationInDays * CAMPAIGN_MINIMUM_DAILY_SPEND,
            budgetRangeMax = navArgs.durationInDays * CAMPAIGN_MAXIMUM_DAILY_SPEND,
            dailySpending = formatDailySpend(dailySpend = navArgs.totalBudget / navArgs.durationInDays),
            forecast = getLoadingForecastUi(),
            durationInDays = navArgs.durationInDays,
            durationRangeMin = 1f,
            durationRangeMax = CAMPAIGN_MAX_DURATION.toFloat(),
            campaignStartDateMillis = navArgs.campaignStartDateMillis,
            campaignDurationDates = getCampaignDurationDisplayDate(
                navArgs.campaignStartDateMillis, navArgs.durationInDays
            ),
            showImpressionsBottomSheet = false,
            showCampaignDurationBottomSheet = false
        )
    )

    val viewState = combine(
        campaignForecastState,
        budgetUiState
    ) { forecast, budgetUiState ->
        budgetUiState.copy(forecast = forecast)
    }.asLiveData()

    init {
        fetchAdForecast()
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onUpdateTapped() {
        triggerEvent(
            ExitWithResult(
                EditBudgetAndDurationResult(
                    totalBudget = budgetUiState.value.totalBudget,
                    durationInDays = budgetUiState.value.durationInDays,
                    campaignStartDateMillis = budgetUiState.value.campaignStartDateMillis
                )
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
        budgetUiState.update { it.copy(sliderValue = sliderValue) }
        if (sliderValue.toInt().mod(budgetUiState.value.durationInDays) == 0) {
            budgetUiState.update {
                it.copy(
                    totalBudget = sliderValue,
                    dailySpending = formatDailySpend(sliderValue / it.durationInDays)
                )
            }
        }
    }

    fun onCampaignDurationUpdated(duration: Int) {
        val currentDailyExpend = budgetUiState.value.totalBudget / budgetUiState.value.durationInDays
        val newTotalBudget = duration * currentDailyExpend
        budgetUiState.update {
            it.copy(
                durationInDays = duration,
                budgetRangeMin = duration * CAMPAIGN_MINIMUM_DAILY_SPEND,
                budgetRangeMax = duration * CAMPAIGN_MAXIMUM_DAILY_SPEND,
                dailySpending = formatDailySpend(currentDailyExpend),
                totalBudget = newTotalBudget,
                sliderValue = newTotalBudget,
                campaignDurationDates = getCampaignDurationDisplayDate(it.campaignStartDateMillis, duration)
            )
        }
        fetchAdForecast()
    }

    fun onStartDateChanged(newStartDateMillis: Long) {
        budgetUiState.update {
            it.copy(
                campaignStartDateMillis = newStartDateMillis,
                campaignDurationDates = getCampaignDurationDisplayDate(
                    newStartDateMillis,
                    it.durationInDays
                )
            )
        }
    }

    fun onBudgetChangeFinished() {
        fetchAdForecast()
    }

    private fun fetchAdForecast() {
        campaignForecastState.update { it.copy(isLoading = true) }
        launch {
            repository.fetchAdForecast(
                startDate = Date(budgetUiState.value.campaignStartDateMillis),
                campaignDurationDays = budgetUiState.value.durationInDays,
                totalBudget = budgetUiState.value.totalBudget
            ).onSuccess { fetchAdForecastResult ->
                campaignForecastState.update {
                    it.copy(
                        isLoading = false,
                        isError = false,
                        impressionsMin = fetchAdForecastResult.minImpressions,
                        impressionsMax = fetchAdForecastResult.maxImpressions
                    )
                }
            }.onFailure {
                campaignForecastState.update {
                    it.copy(
                        isLoading = false,
                        isError = true
                    )
                }
            }
        }
    }

    private fun getCampaignDurationDisplayDate(startDateMillis: Long, duration: Int): String {
        val endDate = Date(startDateMillis + duration * ONE_DAY_IN_MILLIS)
        return "${Date(startDateMillis).formatToMMMdd()} - ${endDate.formatToMMMddYYYY()}"
    }

    private fun formatDailySpend(dailySpend: Float) =
        currencyFormatter.formatCurrencyRounded(dailySpend.toDouble(), navArgs.currencyCode)

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
        val sliderValue: Float,
        val budgetRangeMin: Float,
        val budgetRangeMax: Float,
        val dailySpending: String,
        val forecast: ForecastUi,
        val durationInDays: Int,
        val durationRangeMin: Float,
        val durationRangeMax: Float,
        val showImpressionsBottomSheet: Boolean,
        val showCampaignDurationBottomSheet: Boolean,
        val campaignStartDateMillis: Long,
        val campaignDurationDates: String,
    ) : Parcelable

    @Parcelize
    data class ForecastUi(
        val isLoading: Boolean,
        val impressionsMin: Int,
        val impressionsMax: Int,
        val isError: Boolean
    ) : Parcelable

    @Parcelize
    data class EditBudgetAndDurationResult(
        val totalBudget: Float,
        val durationInDays: Int,
        val campaignStartDateMillis: Long
    ) : Parcelable
}
