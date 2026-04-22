package com.woocommerce.android.ui.payments.cardreader.readermode

import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
        verify(session).start(any())
    }

    @Test
    fun `given starting session state, when emitted, then starting view state is shown`() = testBlocking {
        sessionState.value = CardReaderRemoteSessionState.Starting

        advanceUntilIdle()

        assertThat(viewModel.stateOverride.getOrAwaitValue()).isInstanceOf(RemoteTapToPayStarting::class.java)
    }

    @Test
    fun `given ready to pair session state, when emitted, then ready to pair view state is shown`() = testBlocking {
        sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
            deviceName = "Pixel",
            fingerprintSuffix = "1234"
        )

        advanceUntilIdle()

        val viewState = viewModel.stateOverride.getOrAwaitValue() as RemoteTapToPayReadyToPair
        assertThat(viewState.deviceName).isEqualTo("Pixel")
        assertThat(viewState.fingerprintSuffix).isEqualTo("1234")
    }

    @Test
    fun `given waiting for payment session state, when emitted, then waiting for payment view state is shown`() =
        testBlocking {
            sessionState.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = "Tablet 1")

            advanceUntilIdle()

            val viewState = viewModel.stateOverride.getOrAwaitValue() as RemoteTapToPayWaitingForPayment
            assertThat(viewState.tabletName).isEqualTo("Tablet 1")
        }

    @Test
    fun `given starting view state, when cancel clicked, then exit event is emitted`() = testBlocking {
        sessionState.value = CardReaderRemoteSessionState.Starting

        advanceUntilIdle()

        (viewModel.stateOverride.getOrAwaitValue() as RemoteTapToPayStarting).onPrimaryActionClicked.invoke()

        assertThat(viewModel.events.getOrAwaitValue()).isEqualTo(CardReaderModeExit)
    }
}
