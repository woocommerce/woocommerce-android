package com.woocommerce.android.ui.sitepicker.sitevisibility

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.ui.sitepicker.SitePickerTestUtils
import com.woocommerce.android.ui.sitepicker.sitevisibility.WooSitesVisibilityViewModel.WooStoreUi
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationSettingErrorType
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.NotificationSettingsUpdateError
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.SiteNotificationSetting
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WooSitesVisibilityViewModelTest : BaseUnitTest() {
    companion object {
        private val ALL_WOO_SITES = SitePickerTestUtils.generateStores().map {
            it.apply {
                hasWooCommerce = true
                name = "name $siteId"
                url = "www.$siteId.com"
            }
        }
        private val CURRENT_SELECTED_SITE = ALL_WOO_SITES.first()
        private val AVAILABLE_WOO_SITES_TO_HIDE = ALL_WOO_SITES
            .filter { it.siteId != CURRENT_SELECTED_SITE.siteId }
            .map {
                WooStoreUi(
                    siteName = it.name,
                    siteUrl = it.url,
                    siteId = it.siteId,
                    isSelected = true
                )
            }
        private val A_WOO_SITE_UI_MODEL = ALL_WOO_SITES.last().let {
            WooStoreUi(
                siteName = it.name,
                siteUrl = it.url,
                siteId = it.siteId,
                isSelected = true
            )
        }
    }

    private val sitePickerRepository: SitePickerRepository = mock {
        on { getSites() } doReturn ALL_WOO_SITES
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(CURRENT_SELECTED_SITE)
    }
    private val visibleWooSitesDataStore: VisibleWooSitesDataStore = mock {
        on { isSiteVisible(any()) } doReturn flowOf(true)
    }
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wpComPushNotificationStore: WpComPushNotificationStore = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock {
        on { getWooPushRegisteredSiteIds() } doReturn emptySet()
    }

    @Test
    fun `given all sites are selected, when selected sites change, then enable save button`() =
        testBlocking {
            val viewModel = createViewModel()

            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)

            val updatedState = viewModel.viewState.getOrAwaitValue()
            assertFalse(updatedState.wooStores.last().isSelected)
            assertTrue(updatedState.isSaveButtonEnabled)
        }

    @Test
    fun `given all sites are selected, when selecting unselecting same site, then save button is disabled`() =
        testBlocking {
            val viewModel = createViewModel()

            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)
            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)

            val updatedState = viewModel.viewState.getOrAwaitValue()
            assertTrue(updatedState.wooStores.first().isSelected)
            assertFalse(updatedState.isSaveButtonEnabled)
        }

    @Test
    fun `given update notification settings succeeds, when tapping save, then save site's visibility locally`() =
        testBlocking {
            whenever(wpComPushNotificationStore.updateNotificationSettingsFor(any())).thenReturn(Result.success(Unit))
            val viewModel = createViewModel()

            val hiddenSite = A_WOO_SITE_UI_MODEL
            viewModel.onSiteTapped(hiddenSite)

            viewModel.onSaveTapped()

            verify(visibleWooSitesDataStore).updateSiteVisibilityStatus(
                AVAILABLE_WOO_SITES_TO_HIDE.associate { it.siteId to (hiddenSite.siteId != it.siteId) }
            )
        }

    @Test
    fun `given updating notification settings succeeds, when tapping save, then exit with result`() =
        testBlocking {
            val viewModel = createViewModel()

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onSaveTapped()
            }.last()

            assertThat(event).isEqualTo(ExitWithResult(data = true))
        }

    @Test
    fun `given updating notification settings fails, when tapping save, then error dialog is shown`() =
        testBlocking {
            whenever(wpComPushNotificationStore.updateNotificationSettingsFor(any()))
                .thenReturn(
                    Result.failure(
                        NotificationSettingsUpdateError(
                            type = NotificationSettingErrorType.ApiError("Any error")
                        )
                    )
                )
            val viewModel = createViewModel()

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onSaveTapped()
            }.last()

            assertThat(event).isInstanceOf(ShowDialog::class.java)
        }

    @Test
    fun `when tapping save, then site notification settings are updated on the backend`() =
        testBlocking {
            val viewModel = createViewModel()

            viewModel.onSaveTapped()

            verify(wpComPushNotificationStore).updateNotificationSettingsFor(
                AVAILABLE_WOO_SITES_TO_HIDE.map {
                    SiteNotificationSetting(
                        siteId = it.siteId,
                        newCommentEnabled = it.isSelected,
                        storeOrderEnabled = it.isSelected
                    )
                }
            )
        }

    @Test
    fun `given save was tapped, when loading is done, then hide loading state`() =
        testBlocking {
            val viewModel = createViewModel()

            viewModel.onSaveTapped()

            val updatedState = viewModel.viewState.getOrAwaitValue()

            assertFalse(updatedState.isLoading)
        }

    @Test
    fun `given site has Woo Push token, when tapping save, then exclude it from notification settings API`() =
        testBlocking {
            val siteWithWooPush = AVAILABLE_WOO_SITES_TO_HIDE.first()
            whenever(pushNotificationRepository.getWooPushRegisteredSiteIds())
                .thenReturn(setOf(siteWithWooPush.siteId))
            val viewModel = createViewModel()

            viewModel.onSaveTapped()

            val expectedSites = AVAILABLE_WOO_SITES_TO_HIDE
                .filter { it.siteId != siteWithWooPush.siteId }
                .map {
                    SiteNotificationSetting(
                        siteId = it.siteId,
                        newCommentEnabled = it.isSelected,
                        storeOrderEnabled = it.isSelected
                    )
                }
            verify(wpComPushNotificationStore).updateNotificationSettingsFor(expectedSites)
        }

    @Test
    fun `given all sites have Woo Push token, when tapping save, then skip notification settings API call`() =
        testBlocking {
            val allSiteIds = AVAILABLE_WOO_SITES_TO_HIDE.map { it.siteId }.toSet()
            whenever(pushNotificationRepository.getWooPushRegisteredSiteIds()).thenReturn(allSiteIds)
            val viewModel = createViewModel()

            viewModel.onSaveTapped()

            verify(wpComPushNotificationStore, never()).updateNotificationSettingsFor(any())
        }

    private fun createViewModel() = WooSitesVisibilityViewModel(
        sitePickerRepository = sitePickerRepository,
        selectedSite = selectedSite,
        visibleSitesDataStore = visibleWooSitesDataStore,
        notificationsStore = wpComPushNotificationStore,
        pushNotificationRepository = pushNotificationRepository,
        trackerWrapper = trackerWrapper,
        savedStateHandle = mock()
    )
}
