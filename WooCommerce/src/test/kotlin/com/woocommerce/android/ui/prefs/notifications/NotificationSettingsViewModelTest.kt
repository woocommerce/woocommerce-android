package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val notificationChannelsHandler: NotificationChannelsHandler = mock()
    private val showTestNotification: ShowTestNotification = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: NotificationSettingsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = NotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            resourceProvider = resourceProvider,
            notificationChannelsHandler = notificationChannelsHandler,
            showTestNotification = showTestNotification,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `given cha ching sound is enabled, when view is loaded, then expose correct state`() = testBlocking {
        val status = NotificationChannelsHandler.NewOrderNotificationSoundStatus.DEFAULT
        setup {
            whenever(notificationChannelsHandler.checkNewOrderNotificationSound()).thenReturn(status)
        }

        val isChaChingSoundEnabled = viewModel.newOrderNotificationSoundStatus.captureValues().last()

        assertThat(isChaChingSoundEnabled).isEqualTo(status)
    }

    @Test
    fun `given cha ching sound is disabled, when view is loaded, then expose correct state`() = testBlocking {
        val status = NotificationChannelsHandler.NewOrderNotificationSoundStatus.DISABLED
        setup {
            whenever(notificationChannelsHandler.checkNewOrderNotificationSound()).thenReturn(status)
        }

        val isChaChingSoundEnabled = viewModel.newOrderNotificationSoundStatus.captureValues().last()

        assertThat(isChaChingSoundEnabled).isEqualTo(status)
    }

    @Test
    fun `given order notification sound modified, when view is loaded, then expose correct state`() = testBlocking {
        val status = NotificationChannelsHandler.NewOrderNotificationSoundStatus.SOUND_MODIFIED
        setup {
            whenever(notificationChannelsHandler.checkNewOrderNotificationSound()).thenReturn(status)
        }

        val isChaChingSoundEnabled = viewModel.newOrderNotificationSoundStatus.captureValues().last()

        assertThat(isChaChingSoundEnabled).isEqualTo(status)
    }

    @Test
    fun `when manage notifications is clicked, then open device settings`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onManageNotificationsClicked()
        }.last()

        assertThat(event).isInstanceOf(NotificationSettingsViewModel.OpenDeviceNotificationSettings::class.java)
    }

    @Test
    fun `when enable cha ching sound is clicked, then recreate notification channel`() = testBlocking {
        setup()

        viewModel.onEnableChaChingSoundClicked()

        verify(notificationChannelsHandler).recreateNotificationChannel(NotificationChannelType.NEW_ORDER)
    }

    @Test
    fun `when enable cha ching sound is clicked, then show success snackbar`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onEnableChaChingSoundClicked()
        }.last()

        assertThat(event).matches {
            it is Event.ShowActionSnackbar &&
                it.message == resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar) &&
                it.actionText == resourceProvider.getString(R.string.cha_ching_sound_succcess_snackbar_action)
        }
    }

    @Test
    fun `when test sound is clicked, then show test notification`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onEnableChaChingSoundClicked()
        }.last()
        (event as Event.ShowActionSnackbar).action.onClick(null)

        verify(showTestNotification).invoke(
            title = resourceProvider.getString(R.string.cha_ching_sound_test_notification_title),
            message = resourceProvider.getString(R.string.cha_ching_sound_test_notification_message),
            channelType = NotificationChannelType.NEW_ORDER,
            dismissDelay = 10.seconds
        )
    }
}
