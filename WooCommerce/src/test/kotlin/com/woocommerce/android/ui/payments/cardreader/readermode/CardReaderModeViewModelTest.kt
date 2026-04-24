package com.woocommerce.android.ui.payments.cardreader.readermode

import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class CardReaderModeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderModeViewModel
    private val sessionState = MutableStateFlow<CardReaderRemoteSessionState>(CardReaderRemoteSessionState.Idle)
    private val session: CardReaderRemoteSession = mock {
        on { state }.thenReturn(sessionState)
    }

    @Before
    fun setUp() {
        viewModel = CardReaderModeViewModel(session)
    }

    @Test
    fun `when view model initialized, then session started`() {
        // THEN
        verify(session).start(any())
    }

    @Test
    fun `given starting session state, when emitted, then starting view state is shown`() = testBlocking {
        // WHEN
        sessionState.value = CardReaderRemoteSessionState.Starting
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayStarting::class.java)
    }

    @Test
    fun `given ready to pair session state, when emitted, then ready to pair view state is shown`() = testBlocking {
        // WHEN
        sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
            deviceName = "Pixel",
            fingerprintSuffix = "1234"
        )
        advanceUntilIdle()

        // THEN
        val viewState = viewModel.viewState.value as RemoteTapToPayReadyToPair
        assertThat(viewState.deviceName).isEqualTo("Pixel")
        assertThat(viewState.fingerprintSuffix).isEqualTo("1234")
    }

    @Test
    fun `given waiting for payment session state, when emitted, then waiting for payment view state is shown`() =
        testBlocking {
            // WHEN
            sessionState.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = "Tablet 1")
            advanceUntilIdle()

            // THEN
            val viewState = viewModel.viewState.value as RemoteTapToPayWaitingForPayment
            assertThat(viewState.tabletName).isEqualTo("Tablet 1")
        }

    @Test
    fun `given error session state, when emitted, then error view state carries the message`() = testBlocking {
        // WHEN
        sessionState.value = CardReaderRemoteSessionState.Error(message = "java.net.SocketException: closed")
        advanceUntilIdle()

        // THEN
        val viewState = viewModel.viewState.value as RemoteTapToPayError
        assertThat(viewState.message).isEqualTo("java.net.SocketException: closed")
    }

    @Test
    fun `given starting view state, when cancel clicked, then exit event is emitted`() = testBlocking {
        // GIVEN
        sessionState.value = CardReaderRemoteSessionState.Starting
        advanceUntilIdle()

        // WHEN
        (viewModel.viewState.value as RemoteTapToPayStarting).onPrimaryActionClicked.invoke()

        // THEN
        assertThat(viewModel.events.first()).isEqualTo(CardReaderModeExit)
    }
}
