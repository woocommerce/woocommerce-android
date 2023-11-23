package com.woocommerce.android.ui.products.subscriptions

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.SubscriptionPeriod.Day
import com.woocommerce.android.model.SubscriptionPeriod.Month
import com.woocommerce.android.model.SubscriptionPeriod.Week
import com.woocommerce.android.model.SubscriptionPeriod.Year
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductSubscriptionFreeTrialViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val MAX_TRIAL_LENGTH_DAYS = 90
        private const val MAX_TRIAL_LENGTH_WEEKS = 52
        private const val MAX_TRIAL_LENGTH_MONTHS = 24
        private const val MAX_TRIAL_LENGTH_YEARS = 5
    }

    private val navArgs: ProductSubscriptionFreeTrialFragmentArgs by savedState.navArgs()

    private val _viewState = MutableStateFlow(
        FreeTrialState(
            length = navArgs.subscription.trialLength ?: 0,
            period = navArgs.subscription.trialPeriod ?: Day,
        )
    )
    val viewState = _viewState.asLiveData()

    fun onLengthChanged(length: Int) {
        val isError = checkForError(length, _viewState.value.period)
        _viewState.update { _viewState.value.copy(length = length, isError = isError) }
    }

    private fun checkForError(length: Int, period: SubscriptionPeriod): Boolean {
        return when (period) {
            Day -> length < 0 || length > MAX_TRIAL_LENGTH_DAYS
            Week -> length < 0 || length > MAX_TRIAL_LENGTH_WEEKS
            Month -> length < 0 || length > MAX_TRIAL_LENGTH_MONTHS
            Year -> length < 0 || length > MAX_TRIAL_LENGTH_YEARS
            else -> false
        }
    }

    fun onPeriodChanged(period: SubscriptionPeriod) {
        val isError = checkForError(_viewState.value.length, period)
        _viewState.update { _viewState.value.copy(period = period, isError = isError) }
    }

    fun onExitRequested() {
        analyticsTracker.track(
            AnalyticsEvent.PRODUCT_SUBSCRIPTION_FREE_TRIAL_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges())
        )

        if (_viewState.value.isError) {
            triggerEvent(Exit)
        } else {
            triggerEvent(ExitWithResult(_viewState.value))
        }
    }

    private fun hasChanges(): Boolean {
        return _viewState.value.run {
            !isError && (length != navArgs.subscription.trialLength || period != navArgs.subscription.trialPeriod)
        }
    }

    @Parcelize
    data class FreeTrialState(
        val length: Int,
        val period: SubscriptionPeriod,
        val isError: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val periods = listOf(Day, Week, Month, Year)
    }
}
