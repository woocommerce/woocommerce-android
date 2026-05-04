package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class NewOrderNotificationSettingsViewModelTest : BaseUnitTest() {
    private val parameterRepository: ParameterRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val notificationChannelsHandler: NotificationChannelsHandler = mock()
    private val showTestNotification: ShowTestNotification = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: NewOrderNotificationSettingsViewModel

    private fun setup(prepareMocks: () -> Unit = {}) {
        whenever(parameterRepository.getParameters()).thenReturn(
            SiteParameters(
                currencyCode = "USD",
                currencySymbol = "$",
                currencyFormattingParameters = null,
                weightUnit = null,
                dimensionUnit = null,
                gmtOffset = 0f
            )
        )
        whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
            .thenReturn(NotificationChannelsHandler.NewOrderNotificationSoundStatus.DEFAULT)
        prepareMocks()
        viewModel = NewOrderNotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider,
            notificationChannelsHandler = notificationChannelsHandler,
            showTestNotification = showTestNotification,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `when view is loaded, then all orders preference is selected`() = testBlocking {
        setup()

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.AllOrders)
    }

    @Test
    fun `when notifications switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationsEnabled).isFalse()
    }

    @Test
    fun `when high value preference is selected, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.HighValueOrders)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.HighValueOrders)
    }

    @Test
    fun `when high value threshold amount is changed, then update state`() = testBlocking {
        setup()

        viewModel.onThresholdAmountChanged(BigDecimal(750))

        assertThat(viewModel.viewState.getOrAwaitValue().thresholdAmount)
            .isEqualTo(BigDecimal(750))
    }
    
     @Test
     fun `when high value threshold amount is below minimum, then use minimum amount`() = testBlocking {
        setup()

        viewModel.onThresholdAmountChanged(BigDecimal.ZERO)

        assertThat(viewModel.viewState.getOrAwaitValue().thresholdAmount)
            .isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `given cha ching sound is modified, when view is loaded, then expose modified state`() = testBlocking {
        val status = NotificationChannelsHandler.NewOrderNotificationSoundStatus.SOUND_MODIFIED
        setup {
            whenever(notificationChannelsHandler.checkNewOrderNotificationSound()).thenReturn(status)
        }

        assertThat(viewModel.viewState.getOrAwaitValue().newOrderNotificationSoundStatus)
            .isEqualTo(status)
    }

    @Test
    fun `when notification settings are refreshed, then update cha ching sound state`() = testBlocking {
        setup {
            whenever(notificationChannelsHandler.checkNewOrderNotificationSound()).thenReturn(
                NotificationChannelsHandler.NewOrderNotificationSoundStatus.DEFAULT,
                NotificationChannelsHandler.NewOrderNotificationSoundStatus.DISABLED
            )
        }

        viewModel.refreshNotificationSettings()

        assertThat(viewModel.viewState.getOrAwaitValue().newOrderNotificationSoundStatus)
            .isEqualTo(NotificationChannelsHandler.NewOrderNotificationSoundStatus.DISABLED)
    }

    @Test
    fun `when enable cha ching sound is clicked, then recreate notification channel`() = testBlocking {
        setup()

        viewModel.onEnableChaChingSoundClicked()

        verify(notificationChannelsHandler).recreateNotificationChannel(NotificationChannelType.NEW_ORDER)
    }

    @Test
    fun `when enable cha ching sound is clicked, then track source`() = testBlocking {
        setup()

        viewModel.onEnableChaChingSoundClicked()

        verify(analyticsTracker).track(
            AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_FIX_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to "new_order_settings")
        )
    }

    @Test
    fun `when enable cha ching sound is clicked, then show success snackbar`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onEnableChaChingSoundClicked()
        }.last()

        val snackbar = event as Event.ShowActionStringSnackbar
        assertThat(snackbar.message).isNotBlank()
        assertThat(snackbar.actionText).isNotBlank()
    }

    @Test
    fun `when test sound is clicked, then show test notification`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onEnableChaChingSoundClicked()
        }.last()
        (event as Event.ShowActionStringSnackbar).action.onClick(null)

        val invocation = mockingDetails(showTestNotification).invocations.single()
        assertThat(invocation.arguments[0]).isInstanceOf(String::class.java)
        assertThat(invocation.arguments[1]).isInstanceOf(String::class.java)
        assertThat(invocation.arguments[2]).isEqualTo(NotificationChannelType.NEW_ORDER)
    }
}
