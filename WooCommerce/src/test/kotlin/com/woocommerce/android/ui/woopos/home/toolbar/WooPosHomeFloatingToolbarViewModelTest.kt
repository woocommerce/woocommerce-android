package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_DOCUMENTATION_URL
import com.woocommerce.android.ui.woopos.featureflags.WooPosHistoricalOrdersM1Enabled
import com.woocommerce.android.ui.woopos.featureflags.WooPosPosSettingsEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.GetSupportTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SimpleProductExplanationDialogShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ViewDocsTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosHomeFloatingToolbarViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        onBlocking { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
    }
    private val getSupportFacade: WooPosGetSupportFacade = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val wooPosPosSettingsEnabled: WooPosPosSettingsEnabled = mock()
    private val wooPosHistoricalOrdersM1Enabled: WooPosHistoricalOrdersM1Enabled = mock()

    @Test
    fun `given card reader status is NotConnected, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `given card reader status is Connected, when initialized, then state should be Connected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected)
    }

    @Test
    fun `given card reader status is Connecting, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `when OnToolbarMenuClicked passed with settings feature flag disabled, then menu should be visible without settings`() = runTest {
        // GIVEN
        whenever(wooPosPosSettingsEnabled.invoke()).thenReturn(false)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(
                WooPosHomeFloatingToolbarState.Menu.Visible(
                    listOf(
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_barcode_scanning_title,
                            icon = Icons.Default.DocumentScanner,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_product_limitations_title,
                            icon = Icons.Default.SearchOff,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_documentation_title,
                            icon = Icons.Default.Info,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_get_support_title,
                            icon = Icons.AutoMirrored.Filled.Help,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                        ),
                    )
                )
            )
    }

    @Test
    fun `when OnToolbarMenuClicked passed with settings feature flag enabled, then menu should be visible with settings`() = runTest {
        // GIVEN
        whenever(wooPosPosSettingsEnabled.invoke()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(
                WooPosHomeFloatingToolbarState.Menu.Visible(
                    listOf(
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_settings_title,
                            icon = Icons.Default.Settings,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_barcode_scanning_title,
                            icon = Icons.Default.DocumentScanner,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_product_limitations_title,
                            icon = Icons.Default.SearchOff,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_documentation_title,
                            icon = Icons.Default.Info,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_get_support_title,
                            icon = Icons.AutoMirrored.Filled.Help,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                        ),
                    )
                )
            )
    }

    @Test
    fun `when OnToolbarMenuClicked passed with orders flag enabled, then menu should include orders`() = runTest {
        // GIVEN
        whenever(wooPosPosSettingsEnabled.invoke()).thenReturn(false)
        whenever(wooPosHistoricalOrdersM1Enabled.invoke()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(
                WooPosHomeFloatingToolbarState.Menu.Visible(
                    listOf(
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_orders_title,
                            icon = Icons.Default.Description,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_barcode_scanning_title,
                            icon = Icons.Default.DocumentScanner,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_product_limitations_title,
                            icon = Icons.Default.SearchOff,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_documentation_title,
                            icon = Icons.Default.Info,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_get_support_title,
                            icon = Icons.AutoMirrored.Filled.Help,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                        ),
                    )
                )
            )
    }

    @Test
    fun `when OnOutsideOfToolbarMenuClicked passed and menu is visible, then menu should be hidden`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked)

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    @Test
    fun `when ConnectToAReaderClicked passed, then connect to reader should be called`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

        // THEN
        verify(cardReaderFacade).connectToReader()
    }

    @Test
    fun `when MenuItemClicked with ExitPosClicked, then ExitPosClicked event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.ExitPosClicked)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    @Test
    fun `given card reader status is Connected, when OnCardReaderStatusClicked, then disconnect from reader should be called`() =
        runTest {
            // GIVEN
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade).disconnectFromReader()
        }

    @Test
    fun `given card reader status is NotConnected, when OnCardReaderStatusClicked, then connect to reader should be called`() =
        runTest {
            // GIVEN
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(networkStatus.isConnected()).thenReturn(true)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade).connectToReader()
        }

    @Test
    fun `when get support clicked, then should open support form`() {
        val viewModel = createViewModel()

        viewModel.onUiEvent(
            WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(
                WooPosHomeFloatingToolbarState.Menu.MenuItem(
                    title = R.string.woopos_get_support_title,
                    icon = Icons.AutoMirrored.Filled.Help,
                )
            )
        )

        verify(getSupportFacade).openSupportForm()
    }

    @Test
    fun `given there is no internet, when trying to connect card reader, then trigger proper event`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message)).thenReturn("No internet")

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.ToastMessageDisplayed(
                message = "No internet"
            )
        )
    }

    @Test
    fun `given there is no internet, when trying to connect card reader, then connect card reader method is not called`() =
        runTest {
            // GIVEN
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(resourceProvider.getString(R.string.woopos_no_internet_message)).thenReturn("No internet")

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade, never()).connectToReader()
        }

    @Test
    fun `when Documentation MenuItemClicked, then openUrlEvent should be emitted with proper url`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_documentation_title,
            icon = Icons.Default.Description
        )

        viewModel.openUrlEvent.test {
            // WHEN
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

            // THEN
            assertEquals(WOO_POS_DOCUMENTATION_URL, awaitItem())
            assertEquals(WooPosHomeFloatingToolbarState.Menu.Hidden, viewModel.state.value.menu)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `when where are my products clicked, then should open product explanation dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.onUiEvent(
            WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(
                WooPosHomeFloatingToolbarState.Menu.MenuItem(
                    title = R.string.woopos_product_limitations_title,
                    icon = Icons.Default.SearchOff,
                )
            )
        )

        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.SimpleProductExplanationMenuItemClicked
        )
    }

    @Test
    fun `when where are my products is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_product_limitations_title,
            icon = Icons.Default.SearchOff,
        )
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        verify(analyticsTracker).track(SimpleProductExplanationDialogShown)
    }

    @Test
    fun `when get Support is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_get_support_title,
            icon = Icons.AutoMirrored.Filled.Help
        )
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        verify(analyticsTracker).track(GetSupportTapped)
    }

    @Test
    fun `when View Documentation is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_documentation_title,
            icon = Icons.Default.Description
        )
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        verify(analyticsTracker).track(ViewDocsTapped)
    }

    @Test
    fun `when Exit menu item is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        verify(analyticsTracker).track(ExitTapped)
    }

    @Test
    fun `when Settings MenuItemClicked, then ToSettings navigation event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_settings_title,
            icon = Icons.Default.Settings
        )

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NavigationEvent.ToSettings)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    private fun createViewModel() = WooPosHomeFloatingToolbarViewModel(
        cardReaderFacade,
        childrenToParentEventSender,
        getSupportFacade,
        networkStatus,
        resourceProvider,
        analyticsTracker,
        wooPosPosSettingsEnabled,
        wooPosHistoricalOrdersM1Enabled,
    )
}
