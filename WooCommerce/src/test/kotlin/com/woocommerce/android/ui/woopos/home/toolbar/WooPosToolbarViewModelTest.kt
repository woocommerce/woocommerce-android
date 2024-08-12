package com.woocommerce.android.ui.woopos.home.toolbar

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()

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
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)

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
    fun `when connect to card reader clicked multiple times, then debounce prevents multiple clicks`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        advanceUntilIdle()

        // THEN
        verify(cardReaderFacade, times(1)).connectToReader()
    }

    @Test
    fun `when connect to card reader clicked multiple times after delay, then debounce handles all clicks`() = runTest {
        // GIVEN
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(CardReaderStatus.NotConnected()))
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        advanceUntilIdle()
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        advanceUntilIdle()
        viewModel.onUiEvent(WooPosToolbarUIEvent.ConnectToAReaderClicked)
        advanceUntilIdle()

        // THEN
        verify(cardReaderFacade, times(3)).connectToReader()
    }

    private fun createViewModel() = WooPosToolbarViewModel(
        cardReaderFacade,
        childrenToParentEventSender
    )
}
