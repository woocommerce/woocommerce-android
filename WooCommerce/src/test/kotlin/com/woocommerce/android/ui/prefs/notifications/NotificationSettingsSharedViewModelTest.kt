package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NotificationType
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreReviewPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreStockPreferences
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsSharedViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val site = SiteModel().apply { id = 123 }
    private val defaultNotificationPreferences = WooPushNotificationPreferences(
        storeOrder = StoreOrderPreferences(enabled = true),
        storeReview = StoreReviewPreferences(enabled = true),
        storeStock = StoreStockPreferences(enabled = true)
    )
    private lateinit var notificationPreferencesFlow: MutableStateFlow<WooPushNotificationPreferences?>
    private lateinit var viewModel: NotificationSettingsSharedViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(selectedSite.get()).thenReturn(site)
        notificationPreferencesFlow = MutableStateFlow(null)
        whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
            notificationPreferencesFlow
        )
        mockSuccessfulFetch(defaultNotificationPreferences)
        mockSuccessfulUpdate()
        prepareMocks()
        viewModel = NotificationSettingsSharedViewModel(
            savedStateHandle = SavedStateHandle(),
            selectedSite = selectedSite,
            pushNotificationRepository = pushNotificationRepository,
            resourceProvider = resourceProvider,
            coroutineDispatchers = coroutinesTestRule.testDispatchers
        )
    }

    private suspend fun mockSuccessfulFetch(preferences: WooPushNotificationPreferences) {
        whenever(pushNotificationRepository.fetchWooNotificationPreferences(site)).doSuspendableAnswer {
            notificationPreferencesFlow.value = preferences
            Result.success(preferences)
        }
    }

    private suspend fun mockSuccessfulUpdate() {
        whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any())).doSuspendableAnswer {
            Result.success(it.getArgument<WooPushNotificationPreferences>(1))
        }
    }

    @Test
    fun `when view model is loaded, then expose notification type rows`() = testBlocking {
        setup()

        val notificationTypeItems = viewModel.notificationTypeItems.captureValues().last()

        assertThat(notificationTypeItems.map { it.type }).containsExactly(
            NotificationType.NEW_ORDERS,
            NotificationType.NEW_REVIEWS,
            NotificationType.STOCK
        )
    }

    @Test
    fun `when view model is loaded, then fetch notification preferences`() = testBlocking {
        setup()

        advanceUntilIdle()

        verify(pushNotificationRepository).fetchWooNotificationPreferences(site)
    }

    @Test
    fun `given no cached notification preferences, when fetch is in progress, then show loading`() =
        testBlocking {
            val fetchResult = CompletableDeferred<Result<WooPushNotificationPreferences>>()
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(flowOf(null))
                whenever(pushNotificationRepository.fetchWooNotificationPreferences(site)).doSuspendableAnswer {
                    fetchResult.await()
                }
            }

            val loadingValues = viewModel.isNotificationSettingsLoading.runAndCaptureValues {
                advanceUntilIdle()
            }

            assertThat(loadingValues).contains(true)
            assertThat(viewModel.isNotificationTypeSelectionEnabled.captureValues().last()).isFalse()

            fetchResult.complete(Result.success(defaultNotificationPreferences))
            advanceUntilIdle()

            assertThat(viewModel.isNotificationSettingsLoading.captureValues().last()).isFalse()
            assertThat(viewModel.isNotificationTypeSelectionEnabled.captureValues().last()).isTrue()
        }

    @Test
    fun `given fetch fails, when retry is clicked, then fetch notification preferences again`() =
        testBlocking {
            var fetchFails = true
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(flowOf(null))
                whenever(pushNotificationRepository.fetchWooNotificationPreferences(site)).doSuspendableAnswer {
                    if (fetchFails) {
                        fetchFails = false
                        Result.failure(Exception())
                    } else {
                        Result.success(defaultNotificationPreferences)
                    }
                }
            }

            val event = viewModel.event.runAndCaptureValues {
                advanceUntilIdle()
            }.last()

            val snackbar = event as Event.ShowActionStringSnackbar
            assertThat(snackbar.message).isEqualTo(resourceProvider.getString(R.string.settings_notifs_error_fetch))
            assertThat(snackbar.actionText).isEqualTo(resourceProvider.getString(R.string.retry))

            snackbar.action.onClick(null)
            advanceUntilIdle()

            verify(pushNotificationRepository, times(2)).fetchWooNotificationPreferences(site)
            assertThat(viewModel.isNotificationTypeSelectionEnabled.captureValues().last()).isTrue()
        }

    @Test
    fun `given cached notification preferences, when fetch is in progress, then do not show loading`() =
        testBlocking {
            val fetchResult = CompletableDeferred<Result<WooPushNotificationPreferences>>()
            setup {
                whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
                    flowOf(defaultNotificationPreferences.copy(storeOrder = StoreOrderPreferences(enabled = false)))
                )
                whenever(pushNotificationRepository.fetchWooNotificationPreferences(site)).doSuspendableAnswer {
                    fetchResult.await()
                }
            }

            runCurrent()

            assertThat(viewModel.isNotificationSettingsLoading.captureValues().last()).isFalse()
            assertThat(viewModel.isNotificationTypeSelectionEnabled.captureValues().last()).isTrue()
            assertThat(
                viewModel.notificationTypeItems.captureValues().last()
                    .first { it.type == NotificationType.NEW_ORDERS }
                    .isEnabled
            ).isFalse()

            fetchResult.complete(Result.success(defaultNotificationPreferences))
            advanceUntilIdle()
        }

    @Test
    fun `given fetched notification preferences, when view is loaded, then expose notification type states`() =
        testBlocking {
            val fetchedPreferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = false),
                storeReview = StoreReviewPreferences(enabled = true),
                storeStock = StoreStockPreferences(enabled = false)
            )
            setup {
                mockSuccessfulFetch(fetchedPreferences)
            }

            advanceUntilIdle()

            val notificationTypeItems = viewModel.notificationTypeItems.captureValues().last()

            assertThat(notificationTypeItems.first { it.type == NotificationType.NEW_ORDERS }.isEnabled).isFalse()
            assertThat(notificationTypeItems.first { it.type == NotificationType.NEW_REVIEWS }.isEnabled).isTrue()
            assertThat(notificationTypeItems.first { it.type == NotificationType.STOCK }.isEnabled).isFalse()
        }

    @Test
    fun `when notification type switch is changed, then save changed notification preferences`() = testBlocking {
        setup()
        advanceUntilIdle()

        viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
        advanceUntilIdle()

        val preferences = captureUpdatePreferences()
        assertThat(preferences.storeOrder).isNull()
        assertThat(preferences.storeReview).isNull()
        assertThat(preferences.storeStock).isEqualTo(StoreStockPreferences(enabled = false))
    }

    @Test
    fun `given order preferences have threshold, when order switch is changed, then preserve threshold`() =
        testBlocking {
            val orderPreferences = WooPushNotificationPreferences(
                storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal(50))
            )
            setup {
                notificationPreferencesFlow.value = orderPreferences
                mockSuccessfulFetch(orderPreferences)
            }
            advanceUntilIdle()

            viewModel.onNotificationTypeEnabledChanged(NotificationType.NEW_ORDERS, false)
            advanceUntilIdle()

            val preferences = captureUpdatePreferences()
            assertThat(preferences.storeOrder)
                .isEqualTo(StoreOrderPreferences(enabled = false, minAmount = BigDecimal(50)))
        }

    @Test
    fun `given user reverts notification type before debounce, when debounce completes, then skip update request`() =
        testBlocking {
            setup()
            advanceUntilIdle()

            viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
            viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, true)
            advanceUntilIdle()

            verify(pushNotificationRepository, never()).updateWooNotificationPreferences(eq(site), any())
        }

    @Test
    fun `given update request is pending, when cache emits stale value, then ignore stale cache value`() =
        testBlocking {
            val cachedPreferences = WooPushNotificationPreferences(
                storeStock = StoreStockPreferences(enabled = true)
            )
            setup {
                notificationPreferencesFlow.value = cachedPreferences
                mockSuccessfulFetch(cachedPreferences)
            }
            advanceUntilIdle()

            viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
            notificationPreferencesFlow.value = WooPushNotificationPreferences(
                storeStock = StoreStockPreferences(enabled = true)
            )
            runCurrent()

            val notificationTypeItems = viewModel.notificationTypeItems.captureValues().last()
            assertThat(notificationTypeItems.first { it.type == NotificationType.STOCK }.isEnabled).isFalse()
        }

    @Test
    fun `given update fails, when notification type switch is changed, then rollback and show error`() =
        testBlocking {
            val cachedPreferences = WooPushNotificationPreferences(
                storeStock = StoreStockPreferences(enabled = true)
            )
            setup {
                notificationPreferencesFlow.value = cachedPreferences
                mockSuccessfulFetch(cachedPreferences)
                whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                    .thenReturn(Result.failure(Exception()))
            }

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
                advanceUntilIdle()
            }.last()

            val notificationTypeItems = viewModel.notificationTypeItems.captureValues().last()
            assertThat(notificationTypeItems.first { it.type == NotificationType.STOCK }.isEnabled).isTrue()
            val snackbar = event as Event.ShowActionStringSnackbar
            assertThat(snackbar.message).isEqualTo(resourceProvider.getString(R.string.settings_notifs_error_update))
            assertThat(snackbar.actionText).isEqualTo(resourceProvider.getString(R.string.retry))
        }

    @Test
    fun `given update fails, when retry is clicked, then save notification preferences again`() =
        testBlocking {
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
            advanceUntilIdle()

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
                advanceUntilIdle()
            }.last()

            (event as Event.ShowActionStringSnackbar).action.onClick(null)
            advanceUntilIdle()

            val preferences = captureLastUpdatePreferences()
            assertThat(preferences.storeStock).isEqualTo(StoreStockPreferences(enabled = false))
        }

    @Test
    fun `given update is in progress, when notification type changes again, then save latest state`() =
        testBlocking {
            val firstUpdateGate = CompletableDeferred<Result<WooPushNotificationPreferences>>()
            setup {
                whenever(pushNotificationRepository.updateWooNotificationPreferences(eq(site), any()))
                    .doSuspendableAnswer { invocation ->
                        val preferences = invocation.getArgument<WooPushNotificationPreferences>(1)
                        if (preferences.storeStock?.enabled == false) {
                            firstUpdateGate.await()
                        } else {
                            Result.success(preferences)
                        }
                    }
            }
            advanceUntilIdle()

            viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, false)
            advanceUntilIdle()
            viewModel.onNotificationTypeEnabledChanged(NotificationType.STOCK, true)
            firstUpdateGate.complete(
                Result.success(WooPushNotificationPreferences(storeStock = StoreStockPreferences(enabled = false)))
            )
            advanceUntilIdle()

            val preferences = captureLastUpdatePreferences()
            assertThat(preferences.storeStock).isEqualTo(StoreStockPreferences(enabled = true))
            verify(pushNotificationRepository, times(2)).updateWooNotificationPreferences(eq(site), any())
        }

    @Test
    fun `when new orders notification type is clicked, then open new orders settings`() = testBlocking {
        setup()
        advanceUntilIdle()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNotificationTypeClicked(NotificationType.NEW_ORDERS)
        }.last()

        assertThat(event).isInstanceOf(NotificationSettingsSharedViewModel.OpenNewOrderNotificationSettings::class.java)
    }

    @Test
    fun `when new reviews notification type is clicked, then open new reviews settings`() = testBlocking {
        setup()
        advanceUntilIdle()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNotificationTypeClicked(NotificationType.NEW_REVIEWS)
        }.last()

        assertThat(event).isInstanceOf(
            NotificationSettingsSharedViewModel.OpenNewReviewNotificationSettings::class.java
        )
    }

    @Test
    fun `when stock notification type is clicked, then open stock settings`() = testBlocking {
        setup()
        advanceUntilIdle()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNotificationTypeClicked(NotificationType.STOCK)
        }.last()

        assertThat(event).isInstanceOf(NotificationSettingsSharedViewModel.OpenStockNotificationSettings::class.java)
    }

    private suspend fun captureUpdatePreferences(): WooPushNotificationPreferences {
        val preferencesCaptor = argumentCaptor<WooPushNotificationPreferences>()
        verify(pushNotificationRepository).updateWooNotificationPreferences(eq(site), preferencesCaptor.capture())
        return preferencesCaptor.firstValue
    }

    private suspend fun captureLastUpdatePreferences(): WooPushNotificationPreferences {
        val preferencesCaptor = argumentCaptor<WooPushNotificationPreferences>()
        verify(pushNotificationRepository, times(2))
            .updateWooNotificationPreferences(eq(site), preferencesCaptor.capture())
        return preferencesCaptor.lastValue
    }
}
