package com.woocommerce.android.ui.woopos.root

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.WooPosCardReaderStatus
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WooPosRootViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(testDispatcher)

    private val cardReaderFacade: WooPosCardReaderFacade = mock()

    @Test
    fun `given reader disconnected, when status button clicked, then should connect`() {
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(NotConnected()))
        val sut = createSut()
        assertNotEquals(WooPosCardReaderStatus.Connected, sut.rootScreenState.value.cardReaderStatus)

        sut.onUiEvent(WooPosRootUIEvent.ConnectToAReaderClicked)

        verify(cardReaderFacade).connectToReader()
    }

    @Test
    fun `given reader connected, when status button clicked, then should not connect`() = runTest {
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
    fun `when exit confirmation dialog dismissed, then dialog should be null`() {
        // GIVEN
        val sut = createSut()
        sut.onUiEvent(WooPosRootUIEvent.ExitPOSClicked)

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.ExitConfirmationDialogDismissed)

        // THEN
        assertThat(sut.rootScreenState.value.exitConfirmationDialog).isNull()
    }

    @Test
    fun `when exit confirmation dialog clicked, then dialog should be shown`() {
        // GIVEN
        val sut = createSut()

        // WHEN
        sut.onUiEvent(WooPosRootUIEvent.ExitPOSClicked)

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
    fun `given OnBackFromHomeClicked, should update exit confirmation dialog`() {
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
    fun `given reader status as not connected, should update state with not connected status`() = runTest {
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

    private fun createSut() = WooPosRootViewModel(cardReaderFacade)
}
