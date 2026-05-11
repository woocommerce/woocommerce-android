package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.ShowTestNotification
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
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
    private val selectedSite: SelectedSite = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val site = SiteModel().apply { id = 123 }
    private val appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher)
    private lateinit var viewModel: NewOrderNotificationSettingsViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
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
        whenever(selectedSite.get()).thenReturn(site)
        whenever(pushNotificationRepository.observeWooNotificationPreferences(site))
            .thenReturn(flowOf(null))
        whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
            .doSuspendableAnswer { invocation ->
                val preferences = invocation.getArgument<WooPushNotificationPreferences>(1)
                Result.success(preferences)
            }
        prepareMocks()
        viewModel = NewOrderNotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            parameterRepository = parameterRepository,
            resourceProvider = resourceProvider,
            notificationChannelsHandler = notificationChannelsHandler,
            showTestNotification = showTestNotification,
            analyticsTracker = analyticsTracker,
            selectedSite = selectedSite,
            pushNotificationRepository = pushNotificationRepository,
            appCoroutineScope = appCoroutineScope,
            coroutineDispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when view is loaded, then all orders preference is selected`() = testBlocking {
        setup()

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.AllOrders)
    }

    @Test
    fun `given cached order preferences, when view is loaded, then apply cached preferences`() = testBlocking {
        setup {
            whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                flowOf(
                    WooPushNotificationPreferences(
                        storeOrder = StoreOrderPreferences(enabled = false, minAmount = BigDecimal(50))
                    )
                )
            )
        }

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState.notificationsEnabled).isFalse()
        assertThat(viewState.notificationPreference).isEqualTo(NotificationPreference.HighValueOrders)
        assertThat(viewState.thresholdAmount).isEqualTo(BigDecimal(50))
    }

    @Test
    fun `when notifications switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationsEnabled).isFalse()
    }

    @Test
    fun `when notifications switch is changed, then save order preferences`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)
        advanceUntilIdle()

        val preferences = captureUpdatePreferences()
        assertThat(preferences.storeOrder).isEqualTo(StoreOrderPreferences(enabled = false, minAmount = null))
    }

    @Test
    fun `given order preferences are changed back, when debounce completes, then do not save`() =
        testBlocking {
            setup()

            viewModel.onNotificationsEnabledChanged(false)
            viewModel.onNotificationsEnabledChanged(true)
            advanceUntilIdle()

            verify(pushNotificationRepository, never()).updateWooNotificationPreferences(eq(site), any())
        }

    @Test
    fun `when high value preference is selected, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.HighValueOrders)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationPreference)
            .isEqualTo(NotificationPreference.HighValueOrders)
    }

    @Test
    fun `when all orders preference is selected, then save order preferences without amount`() = testBlocking {
        setup {
            whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                flowOf(
                    WooPushNotificationPreferences(
                        storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(50))
                    )
                )
            )
        }
        runCurrent()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.AllOrders)
        advanceUntilIdle()

        val preferences = captureUpdatePreferences()
        assertThat(preferences.storeOrder).isEqualTo(StoreOrderPreferences(enabled = true, minAmount = null))
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
    fun `when high value threshold amount is changed, then save order preferences after debounce`() = testBlocking {
        setup()

        viewModel.onNotificationPreferenceChanged(NotificationPreference.HighValueOrders)
        advanceUntilIdle()
        viewModel.onThresholdAmountChanged(BigDecimal(750))
        advanceUntilIdle()

        val preferences = captureLastUpdatePreferences()
        assertThat(preferences.storeOrder).isEqualTo(StoreOrderPreferences(enabled = true, minAmount = BigDecimal(750)))
    }

    @Test
    fun `given threshold amount save is pending, when flushing pending preferences, then save immediately`() =
        testBlocking {
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                    flowOf(
                        WooPushNotificationPreferences(
                            storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(50))
                        )
                    )
                )
            }

            runCurrent()
            viewModel.onThresholdAmountChanged(BigDecimal(750))
            viewModel.savePendingOrderPreferences()
            runCurrent()

            val preferences = captureUpdatePreferences()
            assertThat(preferences.storeOrder)
                .isEqualTo(StoreOrderPreferences(enabled = true, minAmount = BigDecimal(750)))
        }

    @Test
    fun `given no pending order preferences, when saving pending preferences, then do not save order preferences`() =
        testBlocking {
            setup()

            viewModel.savePendingOrderPreferences()

            verify(pushNotificationRepository, never()).updateWooNotificationPreferences(eq(site), any())
        }

    @Test
    fun `given order preferences save is in progress, when flushing same preferences, then do not save again`() =
        testBlocking {
            val updateGate = CompletableDeferred<Result<WooPushNotificationPreferences>>()
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                    flowOf(
                        WooPushNotificationPreferences(
                            storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(50))
                        )
                    )
                )
                whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                    .doSuspendableAnswer { invocation ->
                        val preferences = invocation.getArgument<WooPushNotificationPreferences>(1)
                        updateGate.await().map { preferences }
                    }
            }

            runCurrent()
            viewModel.onThresholdAmountChanged(BigDecimal(750))
            advanceUntilIdle()
            viewModel.savePendingOrderPreferences()
            runCurrent()
            updateGate.complete(Result.success(WooPushNotificationPreferences()))
            advanceUntilIdle()

            verify(pushNotificationRepository, times(1)).updateWooNotificationPreferences(eq(site), any())
        }

    @Test
    fun `given update in progress, when order preferences change again, then save latest state`() = testBlocking {
        val firstUpdateGate = CompletableDeferred<Result<WooPushNotificationPreferences>>()

        setup {
            whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                .doSuspendableAnswer { invocation ->
                    val preferences = invocation.getArgument<WooPushNotificationPreferences>(1)
                    val result = if (preferences.storeOrder?.enabled == false) {
                        firstUpdateGate.await()
                    } else {
                        Result.success(preferences)
                    }
                    result
                }
        }

        viewModel.onNotificationsEnabledChanged(false)
        advanceUntilIdle()
        viewModel.onNotificationsEnabledChanged(true)
        firstUpdateGate.complete(
            Result.success(
                WooPushNotificationPreferences(
                    storeOrder = StoreOrderPreferences(false)
                )
            )
        )
        advanceUntilIdle()

        val preferences = captureLastUpdatePreferences()
        assertThat(preferences.storeOrder).isEqualTo(StoreOrderPreferences(enabled = true, minAmount = null))
    }

    @Test
    fun `given update fails, when retry is clicked, then save requested preferences again`() = testBlocking {
        var updateFails = true
        setup {
            whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                .doSuspendableAnswer { invocation ->
                    val preferences = invocation.getArgument<WooPushNotificationPreferences>(1)
                    if (updateFails) {
                        updateFails = false
                        Result.failure(Exception())
                    } else {
                        Result.success(preferences)
                    }
                }
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNotificationsEnabledChanged(false)
            advanceUntilIdle()
        }.last()

        (event as Event.ShowActionStringSnackbar).action.onClick(null)
        advanceUntilIdle()

        val preferencesCaptor = argumentCaptor<WooPushNotificationPreferences>()
        verify(pushNotificationRepository, times(2))
            .updateWooNotificationPreferences(eq(site), preferencesCaptor.capture())
        assertThat(preferencesCaptor.allValues.map { it.storeOrder }).containsOnly(
            StoreOrderPreferences(enabled = false, minAmount = null)
        )
    }

    @Test
    fun `given saved order preferences, when update fails, then rollback to latest saved state`() =
        testBlocking {
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                    flowOf(
                        WooPushNotificationPreferences(
                            storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(50))
                        )
                    )
                )
                whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                    .thenReturn(Result.failure(Exception()))
            }

            runCurrent()
            viewModel.onNotificationsEnabledChanged(false)
            advanceUntilIdle()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.notificationsEnabled).isTrue()
            assertThat(viewState.notificationPreference).isEqualTo(NotificationPreference.HighValueOrders)
            assertThat(viewState.thresholdAmount).isEqualTo(BigDecimal(50))
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

    private suspend fun captureUpdatePreferences(): WooPushNotificationPreferences {
        val preferencesCaptor = argumentCaptor<WooPushNotificationPreferences>()
        verify(pushNotificationRepository).updateWooNotificationPreferences(eq(site), preferencesCaptor.capture())
        return preferencesCaptor.firstValue
    }

    private suspend fun captureLastUpdatePreferences(): WooPushNotificationPreferences {
        val preferencesCaptor = argumentCaptor<WooPushNotificationPreferences>()
        verify(pushNotificationRepository, atLeastOnce())
            .updateWooNotificationPreferences(eq(site), preferencesCaptor.capture())
        return preferencesCaptor.lastValue
    }
}
