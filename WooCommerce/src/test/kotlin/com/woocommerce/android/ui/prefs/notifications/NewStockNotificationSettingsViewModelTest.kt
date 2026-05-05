package com.woocommerce.android.ui.prefs.notifications

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.notifications.NewStockNotificationSettingsViewModel.StockNotificationType
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
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
        prepareMocks()
        viewModel = NewStockNotificationSettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore
        )
    }

    @Test
    fun `when view is loaded, then expose default stock notification states`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState.notificationsEnabled).isTrue()
        assertThat(viewState.lowStockNotificationsEnabled).isTrue()
        assertThat(viewState.outOfStockNotificationsEnabled).isTrue()
        assertThat(viewState.backorderNotificationsEnabled).isTrue()
        assertThat(viewState.defaultLowStockThreshold).isEqualTo(5)
    }

    @Test
    fun `when notifications switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onNotificationsEnabledChanged(false)

        assertThat(viewModel.viewState.getOrAwaitValue().notificationsEnabled).isFalse()
    }

    @Test
    fun `when default low stock threshold is refreshed, then fetch latest value`() = testBlocking {
        setup {
            whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                WooResult(WCProductSettingsModel(defaultLowStockThreshold = 2))
            )
        }

        viewModel.refreshDefaultLowStockThreshold()

        assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(2)
    }

    @Test
    fun `when default low stock threshold is refreshed without a value, then keep fallback value`() =
        testBlocking {
            setup {
                whenever(wooCommerceStore.fetchSiteProductSettings(site)).thenReturn(
                    WooResult(WCProductSettingsModel(defaultLowStockThreshold = null))
                )
            }

            viewModel.refreshDefaultLowStockThreshold()

            assertThat(viewModel.viewState.getOrAwaitValue().defaultLowStockThreshold).isEqualTo(5)
        }

    @Test
    fun `when low stock switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onStockNotificationEnabledChanged(StockNotificationType.LowStock, false)

        assertThat(viewModel.viewState.getOrAwaitValue().lowStockNotificationsEnabled).isFalse()
    }

    @Test
    fun `when out of stock switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onStockNotificationEnabledChanged(StockNotificationType.OutOfStock, false)

        assertThat(viewModel.viewState.getOrAwaitValue().outOfStockNotificationsEnabled).isFalse()
    }

    @Test
    fun `when backorder switch is changed, then update state`() = testBlocking {
        setup()

        viewModel.onStockNotificationEnabledChanged(StockNotificationType.Backorder, false)

        assertThat(viewModel.viewState.getOrAwaitValue().backorderNotificationsEnabled).isFalse()
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
