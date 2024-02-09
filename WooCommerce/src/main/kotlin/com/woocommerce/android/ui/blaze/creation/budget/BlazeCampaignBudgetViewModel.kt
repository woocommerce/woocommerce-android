package com.woocommerce.android.ui.blaze.creation.budget

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAXIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MAX_DURATION
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.ONE_DAY_IN_MILLIS
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class BlazeCampaignBudgetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignBudgetFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableLiveData(
        BudgetUiState(
            currencyCode = navArgs.currencyCode,
            totalBudget = navArgs.totalBudget,
            sliderValue = navArgs.totalBudget,
            budgetRange = getBudgetRange(navArgs.durationInDays),
            dailySpending = formatDailySpend(dailySpend = navArgs.totalBudget / navArgs.durationInDays),
            forecast = ForecastUi(
                isLoaded = false,
                impressionsMin = 0,
                impressionsMax = 0
            ),
            durationInDays = navArgs.durationInDays,
            durationRangeDays = getDurationRange(),
            campaignStartDateMillis = navArgs.campaignStartDateMillis,
            campaignDurationDates = getCampaignDurationDisplayDate(
                navArgs.campaignStartDateMillis, navArgs.durationInDays
            ),
        )
    )
    val viewState = _viewState

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onUpdateTapped() {
        triggerEvent(
            ExitWithResult(
                EditBudgetAndDurationResult(
                    totalBudget = viewState.value?.totalBudget!!,
                    durationInDays = viewState.value?.durationInDays!!,
                    campaignStartDateMillis = viewState.value?.campaignStartDateMillis!!
                )
            )
        )
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
            campaignDurationDates = getCampaignDurationDisplayDate(
                viewState.value?.campaignStartDateMillis!!,
                duration
            )
        )
    }

    fun onStartDateChanged(newStartDateMillis: Long) {
        _viewState.value = _viewState.value?.copy(
            campaignStartDateMillis = newStartDateMillis,
            campaignDurationDates = getCampaignDurationDisplayDate(
                newStartDateMillis,
                viewState.value?.durationInDays!!
            )
        )
    }

    private fun getCampaignDurationDisplayDate(startDateMillis: Long, duration: Int): String {
        val endDate = Date(startDateMillis + duration * ONE_DAY_IN_MILLIS)
        return "${Date(startDateMillis).formatToMMMdd()} - ${endDate.formatToMMMddYYYY()}"
    }

    private fun formatDailySpend(dailySpend: Float) =
        currencyFormatter.formatCurrency(dailySpend.roundToInt().toBigDecimal(), navArgs.currencyCode)

    private fun getBudgetRange(duration: Int) =
        duration * CAMPAIGN_MINIMUM_DAILY_SPEND..duration * CAMPAIGN_MAXIMUM_DAILY_SPEND

    private fun getDurationRange() = 1f..CAMPAIGN_MAX_DURATION.toFloat()

    data class BudgetUiState(
        val currencyCode: String,
        val totalBudget: Float,
        val sliderValue: Float,
        val budgetRange: ClosedFloatingPointRange<Float>,
        val dailySpending: String,
        val forecast: ForecastUi,
        val durationInDays: Int,
        val durationRangeDays: ClosedFloatingPointRange<Float>,
        val showImpressionsBottomSheet: Boolean = false,
        val showCampaignDurationBottomSheet: Boolean = false,
        val campaignStartDateMillis: Long,
        val campaignDurationDates: String,
    )

    data class ForecastUi(
        val isLoaded: Boolean = false,
        val impressionsMin: Int,
        val impressionsMax: Int
    )

    @Parcelize
    data class EditBudgetAndDurationResult(
        val totalBudget: Float,
        val durationInDays: Int,
        val campaignStartDateMillis: Long
    ) : Parcelable
}
