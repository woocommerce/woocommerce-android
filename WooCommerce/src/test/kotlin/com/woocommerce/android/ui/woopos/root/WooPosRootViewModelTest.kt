package com.woocommerce.android.ui.woopos.root

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.Menu.MenuItem
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.WooPosCardReaderStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRootViewModelTest : BaseUnitTest() {
    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        onBlocking { readerStatus }.thenReturn(flowOf(NotConnected()))
    }


    @Test
    fun `given reader disconnected, when status button clicked, then should connect`() = testBlocking {
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(NotConnected()))
        val sut = createSut()
        assertNotEquals(WooPosCardReaderStatus.Connected, sut.rootScreenState.value.cardReaderStatus)

        sut.onUiEvent(WooPosRootUIEvent.ConnectToAReaderClicked)

        verify(cardReaderFacade).connectToReader()
    }

    @Test
    fun `given reader connected, when status button clicked, then should not connect`() = testBlocking {
        val cardReader: CardReader = mock()
        whenever(cardReaderFacade.readerStatus).thenReturn(flow { emit(Connected(cardReader)) })
        val sut = createSut()
        val job = launch {
            sut.rootScreenState.drop(1).collect {
                assertEquals(WooPosCardReaderStatus.Connected, it.cardReaderStatus)
            }
        }

        sut.onUiEvent(WooPosRootUIEvent.ConnectToAReaderClicked)

        verify(cardReaderFacade, never()).connectToReader()
        job.cancel()
    }

    @Test
    fun `when exit confirmation dialog dismissed, then dialog should be null`() = testBlocking {
        // GIVEN
        val sut = createSut()
        sut.onUiEvent(WooPosRootUIEvent.ExitConfirmationDialogDismissed)

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isNull()
    }

    @Test
    fun `when exit confirmation dialog clicked, then dialog should be shown`() = testBlocking {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.OnBackFromHomeClicked)

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isEqualTo(
            WooPosRootScreenState.WooPosExitConfirmationDialog
        )
        assertThat(sut.rootScreenState.value.exitConfirmationDialog?.confirmButton).isEqualTo(
            R.string.woopos_exit_confirmation_confirm_button
        )
        assertThat(sut.rootScreenState.value.exitConfirmationDialog?.dismissButton).isEqualTo(
            R.string.woopos_exit_confirmation_dismiss_button
        )
        assertThat(sut.rootScreenState.value.exitConfirmationDialog?.message).isEqualTo(
            R.string.woopos_exit_confirmation_message
        )
        assertThat(sut.rootScreenState.value.exitConfirmationDialog?.title).isEqualTo(
            R.string.woopos_exit_confirmation_title
        )
    }

    @Test
    fun `given OnBackFromHomeClicked, should update exit confirmation dialog`() = testBlocking {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.OnBackFromHomeClicked)

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isEqualTo(
            WooPosRootScreenState.WooPosExitConfirmationDialog
        )
    }

    @Test
    fun `given reader status as not connected, should update state with not connected status`() = testBlocking {
        // GIVEN
        val notConnectedStatus = NotConnected()

        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(notConnectedStatus))

        val sut = createSut()
        val job = launch {
            sut.rootScreenState.drop(1).collect {
                val state = it.cardReaderStatus as WooPosCardReaderStatus.NotConnected
                assertThat(state.title).isEqualTo(R.string.woopos_reader_disconnected)
            }
        }

        job.cancel()
    }

    @Test
    fun `given reader status as connected, should update state with connected status`() = testBlocking {
        // GIVEN
        val cardReader: CardReader = mock()
        val connectedStatus = Connected(cardReader)

        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(connectedStatus))

        val sut = createSut()
        val job = launch {
            sut.rootScreenState.drop(1).collect {
                assertEquals(WooPosCardReaderStatus.Connected, it.cardReaderStatus)
            }
        }

        job.cancel()
    }

    @Test
    fun `when toolbar menu clicked, should show menu`() = testBlocking {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.OnToolbarMenuClicked)

        // THEN
        assertThat(sut.rootScreenState.value.menu).isEqualTo(
            WooPosRootScreenState.Menu.Visible(
                listOf(
                    MenuItem(
                        title = R.string.woopos_get_support_title,
                        icon = R.drawable.woopos_ic_get_support,
                    ),
                    MenuItem(
                        title = R.string.woopos_exit_confirmation_title,
                        icon = R.drawable.woopos_ic_exit_pos,
                    ),
                )
            )
        )
    }

    @Test
    fun `when menu item clicked, should handle accordingly`() = testBlocking {
        // GIVEN
        val sut = createSut()

        // WHEN
        val menuItem = MenuItem(
            title = R.string.woopos_exit_confirmation_title,
            icon = R.drawable.woopos_ic_exit_pos
        )
        sut.onUiEvent(WooPosRootUIEvent.MenuItemClicked(menuItem))

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isEqualTo(
            WooPosRootScreenState.WooPosExitConfirmationDialog
        )
        assertThat(sut.rootScreenState.value.menu).isEqualTo(WooPosRootScreenState.Menu.Hidden)
    }

    @Test
    fun `when OnOutsideOfToolbarMenuClicked, should hide menu if visible`() = testBlocking {
        // GIVEN
        val sut = createSut()
        sut.onUiEvent(WooPosRootUIEvent.OnToolbarMenuClicked)
        assertThat(sut.rootScreenState.value.menu).isEqualTo(
            WooPosRootScreenState.Menu.Visible(
                listOf(
                    MenuItem(
                        title = R.string.woopos_get_support_title,
                        icon = R.drawable.woopos_ic_get_support,
                    ),
                    MenuItem(
                        title = R.string.woopos_exit_confirmation_title,
                        icon = R.drawable.woopos_ic_exit_pos,
                    ),
                )
            )
        )

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.OnOutsideOfToolbarMenuClicked)

        // THEN
        assertThat(sut.rootScreenState.value.menu).isEqualTo(WooPosRootScreenState.Menu.Hidden)
    }

    @Test
    fun `when OnOutsideOfToolbarMenuClicked, should do nothing if menu already hidden`() = testBlocking {
        // GIVEN
        val sut = createSut()
        assertThat(sut.rootScreenState.value.menu).isEqualTo(WooPosRootScreenState.Menu.Hidden)

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.OnOutsideOfToolbarMenuClicked)

        // THEN
        assertThat(sut.rootScreenState.value.menu).isEqualTo(WooPosRootScreenState.Menu.Hidden)
    }

    @Test
    fun `when ExitConfirmationDialogDismissed, should set exitConfirmationDialog to null`() = testBlocking {
        // GIVEN
        val sut = createSut()
        sut.onUiEvent(WooPosRootUIEvent.OnBackFromHomeClicked)
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isEqualTo(
            WooPosRootScreenState.WooPosExitConfirmationDialog
        )

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.ExitConfirmationDialogDismissed)

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isNull()
    }

    private fun createSut() = WooPosRootViewModel(cardReaderFacade)
}
