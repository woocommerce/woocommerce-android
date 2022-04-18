package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class CollectPaymentActionTest : CardReaderBaseUnitTest() {
    private lateinit var action: CollectPaymentAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = CollectPaymentAction(terminal, mock())
    }

    @Test
    fun `when collecting payment succeeds, then Success is emitted`() = testBlocking {
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isExactlyInstanceOf(Success::class.java)
    }

    @Test
    fun `when collecting payment fails, then Failure is emitted`() = testBlocking {
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat(result).isExactlyInstanceOf(Failure::class.java)
    }

    @Test
    fun `when collecting payment succeeds, then updated paymentIntent is returned`() = testBlocking {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(updatedPaymentIntent)
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).first()

        assertThat((result as Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `when collecting payment succeeds, then flow is terminated`() = testBlocking {
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when collecting payment fails, then flow is terminated`() = testBlocking {
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectPayment(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `given flow not terminated, when job canceled, then collect payment gets canceled`() = testBlocking {
        val cancelable = mock<Cancelable>()
        whenever(cancelable.isCompleted).thenReturn(false)
        whenever(terminal.collectPaymentMethod(any(), any())).thenAnswer { cancelable }
        val job = launch {
            action.collectPayment(mock()).collect { }
        }

        job.cancel()
        joinAll(job)

        verify(cancelable).cancel(any())
    }
}
