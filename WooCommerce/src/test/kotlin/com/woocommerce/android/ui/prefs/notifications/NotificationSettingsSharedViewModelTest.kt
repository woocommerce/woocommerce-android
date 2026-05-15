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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreReviewPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreStockPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsSharedViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val site = SiteModel().apply { id = 123 }
    private lateinit var notificationPreferencesFlow: MutableStateFlow<WooPushNotificationPreferences?>
    private lateinit var viewModel: NotificationSettingsSharedViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(selectedSite.get()).thenReturn(site)
        notificationPreferencesFlow = MutableStateFlow(null)
        whenever(pushNotificationRepository.observeWooNotificationPreferences(site)).thenReturn(
            notificationPreferencesFlow
        )
        mockSuccessfulFetch(WooPushNotificationPreferences())
        prepareMocks()
        viewModel = NotificationSettingsSharedViewModel(
            savedStateHandle = SavedStateHandle(),
            selectedSite = selectedSite,
            pushNotificationRepository = pushNotificationRepository,
            resourceProvider = resourceProvider
        )
    }

    private suspend fun mockSuccessfulFetch(preferences: WooPushNotificationPreferences) {
        whenever(pushNotificationRepository.fetchWooNotificationPreferences(site)).doSuspendableAnswer {
            notificationPreferencesFlow.value = preferences
            Result.success(preferences)
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

            fetchResult.complete(Result.success(WooPushNotificationPreferences()))
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
                        Result.success(WooPushNotificationPreferences())
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
                    flowOf(WooPushNotificationPreferences(storeOrder = StoreOrderPreferences(enabled = false)))
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

            fetchResult.complete(Result.success(WooPushNotificationPreferences()))
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
}
