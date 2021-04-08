package com.woocommerce.android.cardreader.internal.payments.actions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.callable.ReaderDisplayListener
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.DisplayMessageRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.ReaderInputRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class CollectPaymentActionTest {
    private lateinit var action: CollectPaymentAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = CollectPaymentAction(terminal, mock())
    }

    @Test
    fun `when collecting payment succeeds, then Success is emitted`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[2] as PaymentIntentCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isExactlyInstanceOf(Success::class.java)
    }

    @Test
    fun `when collecting payment fails, then Failure is emitted`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[2] as PaymentIntentCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isExactlyInstanceOf(Failure::class.java)
    }

    @Test
    fun `when collecting payment succeeds, then updated paymentIntent is returned`() = runBlockingTest {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[2] as PaymentIntentCallback).onSuccess(updatedPaymentIntent)
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat((result as Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `when collecting payment succeeds, then flow is terminated`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[2] as PaymentIntentCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when collecting payment fails, then flow is terminated`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[2] as PaymentIntentCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when display message requested, then DisplayMessageRequested emitted`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderDisplayMessage(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isInstanceOf(DisplayMessageRequested::class.java)
    }

    @Test
    fun `when insert card requested, then ReaderInputRequested emitted`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderInput(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isInstanceOf(ReaderInputRequested::class.java)
    }

    @Test
    fun `given last event is terminal, when multiple events emitted, then flow terminates`() = runBlockingTest {
        whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderInput(mock()) // non-terminal
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderDisplayMessage(mock()) // non-terminal
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderInput(mock()) // non-terminal
            (it.arguments[1] as ReaderDisplayListener).onRequestReaderDisplayMessage(mock()) // non-terminal
            (it.arguments[2] as PaymentIntentCallback).onSuccess(mock()) // terminal
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).toList()

        assertThat(result.size).isEqualTo(5)
    }

    @Test(expected = ClosedSendChannelException::class)
    fun `given more events emitted, when terminal event already processed, then exception is thrown`() =
        runBlockingTest {
            whenever(terminal.collectPaymentMethod(any(), any(), any())).thenAnswer {
                (it.arguments[2] as PaymentIntentCallback).onSuccess(mock()) // terminal
                (it.arguments[1] as ReaderDisplayListener).onRequestReaderInput(mock()) // non-terminal
                mock<Cancelable>()
            }

            action.collectPayment(mock()).toList()
        }
}
