package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.featureflags.WooPosHistoricalOrdersM1Enabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosToolbarViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        onBlocking { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
    }
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val networkStatus: WooPosNetworkStatus = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val wooPosHistoricalOrdersM1Enabled: WooPosHistoricalOrdersM1Enabled = mock()

    @Test
    fun `given card reader status is NotConnected, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `given card reader status is Connected, when initialized, then state should be Connected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.Connected)
    }

    @Test
    fun `given card reader status is Connecting, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `when OnToolbarMenuClicked passed, then menu should be visible with settings`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(
                WooPosToolbarState.Menu.Visible(
                    listOf(
                        WooPosToolbarState.Menu.MenuItem(
                            title = R.string.woopos_settings_title,
                            icon = Icons.Default.Settings,
                        ),
                        WooPosToolbarState.Menu.MenuItem(
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
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnToolbarMenuClicked)

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnOutsideOfToolbarMenuClicked)

        // THEN
        assertThat(viewModel.state.value.menu)
            .isEqualTo(WooPosToolbarState.Menu.Hidden)
    }

    @Test
    fun `when ConnectToAReaderClicked passed, then connect to reader should be called`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

        // THEN
        verify(cardReaderFacade).connectToReader()
    }

    @Test
    fun `when MenuItemClicked with ExitPosClicked, then ExitPosClicked event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.ExitPosClicked)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosToolbarState.Menu.Hidden)
    }

    @Test
    fun `given card reader status is Connected, when OnCardReaderStatusClicked, then disconnect from reader should be called`() =
        runTest {
            // GIVEN
            whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

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
            viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade).connectToReader()
        }

    @Test
    fun `given there is no internet, when trying to connect card reader, then trigger proper event`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(cardReaderFacade.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
        whenever(resourceProvider.getString(R.string.woopos_no_internet_message)).thenReturn("No internet")

        // WHEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

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
            viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade, never()).connectToReader()
        }

    @Test
    fun `when Exit menu item is clicked, then should track analytics event`() = runTest {
        val viewModel = createViewModel()
        val menuItem = WooPosToolbarState.Menu.MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )
        viewModel.onUiEvent(WooPosToolbarUIEvent.MenuItemClicked(menuItem))

        verify(analyticsTracker).track(ExitTapped)
    }

    @Test
    fun `when Settings MenuItemClicked, then ToSettings navigation event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosToolbarState.Menu.MenuItem(
            title = R.string.woopos_settings_title,
            icon = Icons.Default.Settings
        )

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NavigationEvent.ToSettings)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosToolbarState.Menu.Hidden)
    }

    @Test
    fun `when Orders MenuItemClicked, then ToOrders navigation event should be sent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        val menuItem = WooPosToolbarState.Menu.MenuItem(
            title = com.woocommerce.android.R.string.woopos_orders_title,
            icon = Icons.Default.Description
        )

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.MenuItemClicked(menuItem))

        // THEN
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NavigationEvent.ToOrders)
        assertThat(viewModel.state.value.menu).isEqualTo(WooPosToolbarState.Menu.Hidden)
    }

    private fun createViewModel() = WooPosToolbarViewModel(
        cardReaderFacade,
        childrenToParentEventSender,
        networkStatus,
        resourceProvider,
        analyticsTracker,
        wooPosHistoricalOrdersM1Enabled,
    )
}
