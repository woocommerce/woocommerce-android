package com.woocommerce.android.ui.woopos.home.toolbar

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosToolbarViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()
    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        onBlocking { readerStatus }.thenReturn(flowOf(CardReaderStatus.NotConnected()))
    }
    private val getSupportFacade: WooPosGetSupportFacade = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val networkStatus: WooPosNetworkStatus = mock()

    @Test
    fun `given card reader status is NotConnected, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `given card reader status is Connected, when initialized, then state should be Connected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.Connected(mock())))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.Connected)
    }

    @Test
    fun `given card reader status is Connecting, when initialized, then state should be NotConnected`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.Connecting))
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.cardReaderStatus)
            .isEqualTo(WooPosToolbarState.WooPosCardReaderStatus.NotConnected)
    }

    @Test
    fun `when OnToolbarMenuClicked passed, then menu should be visible`() = runTest {
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
                            title = R.string.woopos_get_support_title,
                            icon = R.drawable.woopos_ic_get_support,
                        ),
                        WooPosToolbarState.Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.woopos_ic_exit_pos,
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
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))
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
            icon = R.drawable.woopos_ic_exit_pos
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
            whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.Connected(mock())))
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
            whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

            // THEN
            verify(cardReaderFacade).connectToReader()
        }

    @Test
    fun `when get support clicked, then should open support form`() {
        val viewModel = createViewModel()

        viewModel.onUiEvent(
            WooPosToolbarUIEvent.MenuItemClicked(
                WooPosToolbarState.Menu.MenuItem(
                    title = R.string.woopos_get_support_title,
                    icon = R.drawable.woopos_ic_get_support,
                )
            )
        )

        verify(getSupportFacade).openSupportForm()
    }

    @Test
    fun `given there is no internet, when trying to connect card reader, then trigger proper event`() = runTest {
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))

        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked)

        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NoInternet)
    }

    private fun createViewModel() = WooPosToolbarViewModel(
        cardReaderFacade,
        childrenToParentEventSender,
        getSupportFacade,
        networkStatus
    )
}
