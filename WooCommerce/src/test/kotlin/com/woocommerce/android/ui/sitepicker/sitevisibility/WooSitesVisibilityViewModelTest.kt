package com.woocommerce.android.ui.sitepicker.sitevisibility

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.NotificationSettingErrorType
import org.wordpress.android.fluxc.store.NotificationStore.NotificationSettingsUpdateError
import org.wordpress.android.fluxc.store.NotificationStore.SiteNotificationSetting
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
        onBlocking { getSites() } doReturn ALL_WOO_SITES
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(CURRENT_SELECTED_SITE)
    }
    private val visibleWooSitesDataStore: VisibleWooSitesDataStore = mock {
        onBlocking { isSiteVisible(any()) } doReturn flowOf(true)
    }
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()

    private val notificationStore: NotificationStore = mock()

    private lateinit var viewModel: WooSitesVisibilityViewModel

    @Before
    fun setUp() {
        viewModel = WooSitesVisibilityViewModel(
            sitePickerRepository = sitePickerRepository,
            selectedSite = selectedSite,
            visibleSitesDataStore = visibleWooSitesDataStore,
            notificationsStore = notificationStore,
            trackerWrapper = trackerWrapper,
            savedStateHandle = mock()
        )
    }

    @Test
    fun `given all sites are selected, when selected sites change, then enable save button`() =
        testBlocking {
            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)

            val updatedState = viewModel.viewState.getOrAwaitValue()
            assertFalse(updatedState.wooStores.last().isSelected)
            assertTrue(updatedState.isSaveButtonEnabled)
        }

    @Test
    fun `given all sites are selected, when selecting unselecting same site, then save button is disabled`() =
        testBlocking {
            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)
            viewModel.onSiteTapped(A_WOO_SITE_UI_MODEL)

            val updatedState = viewModel.viewState.getOrAwaitValue()
            assertTrue(updatedState.wooStores.first().isSelected)
            assertFalse(updatedState.isSaveButtonEnabled)
        }

    @Test
    fun `given update notification settings succeeds, when tapping save, then save site's visibility locally`() =
        testBlocking {
            whenever(notificationStore.updateNotificationSettingsFor(any())).thenReturn(Result.success(Unit))

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
            val event = viewModel.event.runAndCaptureValues {
                viewModel.onSaveTapped()
            }.last()

            assertThat(event).isEqualTo(ExitWithResult(data = true))
        }

    @Test
    fun `given updating notification settings fails, when tapping save, then error dialog is shown`() =
        testBlocking {
            whenever(notificationStore.updateNotificationSettingsFor(any()))
                .thenReturn(
                    Result.failure(
                        NotificationSettingsUpdateError(
                            type = NotificationSettingErrorType.ApiError("Any error")
                        )
                    )
                )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onSaveTapped()
            }.last()

            assertThat(event).isInstanceOf(ShowDialog::class.java)
        }

    @Test
    fun `when tapping save, then site notification settings are updated on the backend`() =
        testBlocking {
            viewModel.onSaveTapped()

            verify(notificationStore).updateNotificationSettingsFor(
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
            viewModel.onSaveTapped()

            val updatedState = viewModel.viewState.getOrAwaitValue()

            assertFalse(updatedState.isLoading)
        }
}
