package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NewReviewNotificationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    fun onNotificationsEnabledChanged(isEnabled: Boolean) {
        _viewState.update { it.copy(notificationsEnabled = isEnabled) }
    }

    fun onNotificationPreferenceChanged(preference: NotificationPreference) {
        _viewState.update { it.copy(notificationPreference = preference) }
    }

    fun onSelectedRatingChanged(rating: Int) {
        _viewState.update { it.copy(selectedRating = rating.coerceIn(MIN_RATING, MAX_RATING)) }
    }

    data class ViewState(
        val notificationsEnabled: Boolean = true,
        val notificationPreference: NotificationPreference = NotificationPreference.AllReviews,
        val selectedRating: Int = DEFAULT_SELECTED_RATING
    )

    enum class NotificationPreference {
        AllReviews,
        RatingFilteredReviews
    }

    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        private const val DEFAULT_SELECTED_RATING = 2
    }
}
