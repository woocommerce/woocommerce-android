package com.woocommerce.android.ui.woopos.home.toolbar

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BatteryStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosEffectiveReaderStatusProvider
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionController
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionControllerFactory
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosDiscoveredReader
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderSession
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.BatteryState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.InetAddress

@ExperimentalCoroutinesApi
class WooPosHomeFloatingToolbarViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        on { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        on { batteryStatus }.thenReturn(flowOf(CardReaderBatteryStatus.Unknown))
    }
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val controller: WooPosCardReaderConnectionController = mock()
    private val controllerFactory: WooPosCardReaderConnectionControllerFactory = mock {
        on { create(any()) }.thenReturn(controller)
    }
    private val remoteReaderSession: WooPosRemoteReaderSession = mock {
        on { state }.thenReturn(MutableStateFlow(WooPosRemoteReaderSession.State.Idle))
    }

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
            .isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
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
    fun `when OnToolbarMenuClicked passed, then menu should be visible with settings`() = runTest {
        // GIVEN
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
                            icon = R.drawable.ic_description_filled_24dp,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_settings_title,
                            icon = R.drawable.ic_settings_filled_24dp,
                        ),
                        WooPosHomeFloatingToolbarState.Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.ic_exit_to_app_24dp,
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
    fun `when ConnectToAReaderClicked passed, then show connection dialog event should be sent`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.ShowCardReaderConnectionDialog)
    }

    @Test
    fun `when MenuItemClicked with ExitPosClicked, then ExitPosClicked event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = R.drawable.ic_exit_to_app_24dp
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
            verify(controller).disconnect()
        }

    @Test
    fun `given card reader status is NotConnected, when OnCardReaderStatusClicked, then show connection dialog event should be sent`() =
        runTest {
            // GIVEN
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(networkStatus.isConnected()).thenReturn(true)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.ShowCardReaderConnectionDialog)
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
    fun `given there is no internet, when trying to connect card reader, then show connection dialog event is not sent`() =
        runTest {
            // GIVEN
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            whenever(resourceProvider.getString(R.string.woopos_no_internet_message)).thenReturn("No internet")

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(childrenToParentEventSender, never()).sendToParent(ChildToParentEvent.ShowCardReaderConnectionDialog)
        }

    @Test
    fun `when Exit menu item is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = R.drawable.ic_exit_to_app_24dp
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
            icon = R.drawable.ic_settings_filled_24dp
        )

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NavigationEvent.ToSettings)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    @Test
    fun `when Orders MenuItemClicked, then ToOrders navigation event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosHomeFloatingToolbarState.Menu.MenuItem(
            title = R.string.woopos_orders_title,
            icon = R.drawable.ic_description_filled_24dp
        )

        // WHEN
        viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NavigationEvent.ToOrders)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosHomeFloatingToolbarState.Menu.Hidden)
    }

    @Test
    fun `given card reader status is Reconnecting, when initialized, then state should be Reconnecting`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Reconnecting))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Reconnecting)
    }

    @Test
    fun `given card reader status is Reconnecting, when OnCardReaderStatusClicked, then cancelReconnection should be called`() =
        runTest {
            // GIVEN
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Reconnecting))
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade).cancelReconnection()
        }

    @Test
    fun `given connected with nominal battery, when initialized, then battery state should be NOMINAL`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderFacade.batteryStatus).thenReturn(
            flowOf(CardReaderBatteryStatus.StatusChanged(0.8f, BatteryStatus.NOMINAL, false))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val status = viewModel.state.value.cardReaderStatus
        assertThat(status).isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
        assertThat((status as WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected).batteryState)
            .isEqualTo(BatteryState.NOMINAL)
    }

    @Test
    fun `given connected with low battery, when initialized, then battery state should be LOW`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderFacade.batteryStatus).thenReturn(
            flowOf(CardReaderBatteryStatus.StatusChanged(0.2f, BatteryStatus.LOW, false))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val status = viewModel.state.value.cardReaderStatus
        assertThat(status).isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
        assertThat((status as WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected).batteryState)
            .isEqualTo(BatteryState.LOW)
    }

    @Test
    fun `given connected with critical battery, when initialized, then battery state should be CRITICAL`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderFacade.batteryStatus).thenReturn(
            flowOf(CardReaderBatteryStatus.StatusChanged(0.05f, BatteryStatus.CRITICAL, false))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val status = viewModel.state.value.cardReaderStatus
        assertThat(status).isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
        assertThat((status as WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected).batteryState)
            .isEqualTo(BatteryState.CRITICAL)
    }

    @Test
    fun `given connected with battery warning, when initialized, then battery state should be LOW`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderFacade.batteryStatus).thenReturn(flowOf(CardReaderBatteryStatus.Warning))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val status = viewModel.state.value.cardReaderStatus
        assertThat(status).isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
        assertThat((status as WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected).batteryState)
            .isEqualTo(BatteryState.LOW)
    }

    @Test
    fun `given connected with unknown battery, when initialized, then battery state should be NOMINAL`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderFacade.batteryStatus).thenReturn(flowOf(CardReaderBatteryStatus.Unknown))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val status = viewModel.state.value.cardReaderStatus
        assertThat(status).isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
        assertThat((status as WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected).batteryState)
            .isEqualTo(BatteryState.NOMINAL)
    }

    @Test
    fun `given remote session is Connected, when initialized, then state should be Connected`() = runTest {
        // GIVEN
        val phone = WooPosDiscoveredReader.Phone(
            name = "Andrey's phone",
            host = InetAddress.getByName("127.0.0.1"),
            port = 0,
            fingerprintBase64 = "fp",
        )
        whenever(remoteReaderSession.state).thenReturn(
            MutableStateFlow(WooPosRemoteReaderSession.State.Connected(phone, readerSerial = "serial"))
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isInstanceOf(WooPosHomeFloatingToolbarState.WooPosCardReaderStatus.Connected::class.java)
    }

    private fun createViewModel() = WooPosHomeFloatingToolbarViewModel(
        cardReaderFacade,
        childrenToParentEventSender,
        networkStatus,
        resourceProvider,
        analyticsTracker,
        WooPosEffectiveReaderStatusProvider(cardReaderFacade, remoteReaderSession),
        controllerFactory
    )
}
