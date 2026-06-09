package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class NewStockNotificationSettingsViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private lateinit var viewModel: NewStockNotificationSettingsViewModel
    private val site = SiteModel().apply {
        adminUrl = "https://example.com/wp-admin"
    }

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooCommerceStore.getProductSettings(site)).thenReturn(null)
        whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
            WooResult(WCProductSettingsModel(defaultLowStockThreshold = null))
        )
        prepareMocks()
        viewModel = NewStockNotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore
        )
    }

    @Test
    fun `when view is loaded, then expose default low stock threshold state`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState.defaultLowStockThreshold).isNull()
        assertThat(viewState.isDefaultLowStockThresholdLoading).isFalse()
    }

    @Test
    fun `when view is loaded, then fetch default low stock threshold`() = testBlocking {
        setup {
            whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                WooResult(WCProductSettingsModel(defaultLowStockThreshold = 2))
            )
        }

        assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(2)
        assertThat(viewModel.viewState.getOrAwaitValue().isDefaultLowStockThresholdLoading).isFalse()
    }

    @Test
    fun `when cached low stock threshold exists, then expose cached value while fetching latest value`() = testBlocking {
        setup {
            whenever(wooCommerceStore.getProductSettings(site)).thenReturn(
                WCProductSettingsModel(defaultLowStockThreshold = 2)
            )
            whenever(wooCommerceStore.fetchSiteProductSettings(site)).doSuspendableAnswer {
                delay(1_000)
                WooResult(WCProductSettingsModel(defaultLowStockThreshold = 3))
            }
        }

        runCurrent()

        assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(2)
        assertThat(viewModel.viewState.getOrAwaitValue().isDefaultLowStockThresholdLoading).isFalse()
    }

    @Test
    fun `when default low stock threshold fetch fails without cached value, then stop loading and show error`() =
        testBlocking {
            setup {
                whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                    WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                )
            }

            val viewState = viewModel.viewState.getOrAwaitValue()
            val event = viewModel.event.runAndCaptureValues { }.last()

            assertThat(viewState.defaultLowStockThreshold).isNull()
            assertThat(viewState.isDefaultLowStockThresholdLoading).isFalse()
            assertThat(event).isInstanceOf(MultiLiveEvent.Event.ShowSnackbar::class.java)
            assertThat((event as MultiLiveEvent.Event.ShowSnackbar).message)
                .isEqualTo(R.string.settings_notifs_stock_low_stock_threshold_error)
        }

    @Test
    fun `when default low stock threshold fetch fails with cached value, then keep cached value and show error`() =
        testBlocking {
            setup {
                whenever(wooCommerceStore.getProductSettings(site)).thenReturn(
                    WCProductSettingsModel(defaultLowStockThreshold = 2)
                )
                whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                    WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                )
            }

            val viewState = viewModel.viewState.getOrAwaitValue()
            val event = viewModel.event.runAndCaptureValues { }.last()

            assertThat(viewState.defaultLowStockThreshold).isEqualTo(2)
            assertThat(viewState.isDefaultLowStockThresholdLoading).isFalse()
            assertThat(event).isInstanceOf(MultiLiveEvent.Event.ShowSnackbar::class.java)
            assertThat((event as MultiLiveEvent.Event.ShowSnackbar).message)
                .isEqualTo(R.string.settings_notifs_stock_low_stock_threshold_error)
        }

    @Test
    fun `when store settings web view is closed, then refresh default low stock threshold`() = testBlocking {
        setup {
            whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                WooResult(WCProductSettingsModel(defaultLowStockThreshold = 2)),
                WooResult(WCProductSettingsModel(defaultLowStockThreshold = 3))
            )
        }

        viewModel.onStoreSettingsWebViewClosed()

        assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(3)
        assertThat(viewModel.viewState.getOrAwaitValue().isDefaultLowStockThresholdLoading).isFalse()
    }

    @Test
    fun `when default low stock threshold refresh returns no value, then keep current threshold`() =
        testBlocking {
            setup {
                whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                    WooResult(WCProductSettingsModel(defaultLowStockThreshold = 2)),
                    WooResult(WCProductSettingsModel(defaultLowStockThreshold = null))
                )
            }

            viewModel.onStoreSettingsWebViewClosed()

            assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(2)
            assertThat(viewModel.viewState.getOrAwaitValue().isDefaultLowStockThresholdLoading).isFalse()
        }

    @Test
    fun `when edit store settings is clicked, then open authenticated web view`() =
        testBlocking {
            setup()

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onEditStoreSettingsClicked()
            }.last()

            assertThat(event).isInstanceOf(MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView::class.java)
            val webViewEvent = event as MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView
            assertThat(webViewEvent.screenTitle).isEqualTo(UiString.UiStringRes(R.string.more_menu_button_wс_admin))
        }
}
