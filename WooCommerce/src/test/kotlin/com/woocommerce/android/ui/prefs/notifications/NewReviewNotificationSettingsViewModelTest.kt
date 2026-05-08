package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.notifications.NewReviewNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewReviewNotificationSettingsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: NewReviewNotificationSettingsViewModel

    private fun setup() {
        viewModel = NewReviewNotificationSettingsViewModel(SavedStateHandle())
    }

    @Test
    fun `when view is loaded, then all reviews preference is selected`() = testBlocking {
        setup()

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.AllReviews)
    }

    @Test
    fun `when notifications switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationsEnabled).isFalse()
    }

    @Test
    fun `when rating filtered reviews preference is selected, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.RatingFilteredReviews)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.RatingFilteredReviews)
    }

    @Test
    fun `when selected rating is changed, then update state`() = testBlocking {
        setup()

        viewModel.onSelectedRatingChanged(4)

        assertThat(viewModel.viewState.getOrAwaitValue().selectedRating).isEqualTo(4)
    }

    @Test
    fun `when selected rating is outside supported range, then constrain state`() = testBlocking {
        setup()

        viewModel.onSelectedRatingChanged(10)

        assertThat(viewModel.viewState.getOrAwaitValue().selectedRating)
            .isEqualTo(NewReviewNotificationSettingsViewModel.MAX_RATING)
    }
}
