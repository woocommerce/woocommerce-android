package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class ProcessPaymentActionTest {
    private lateinit var action: ProcessPaymentAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = ProcessPaymentAction(terminal, mock())
    }

    @Test
    fun `when processing payment succeeds, then Success is emitted`() = runTest {
        whenever(terminal.processPayment(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.processPayment(mock()).first()

        Assertions.assertThat(result).isExactlyInstanceOf(ProcessPaymentStatus.Success::class.java)
    }

    @Test
    fun `when processing payment fails, then Failure is emitted`() = runTest {
        whenever(terminal.processPayment(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.processPayment(mock()).first()

        Assertions.assertThat(result).isExactlyInstanceOf(ProcessPaymentStatus.Failure::class.java)
    }

    @Test
    fun `when processing payment succeeds, then updated paymentIntent is returned`() = runTest {
        val updatedPaymentIntent = mock<PaymentIntent>()
        whenever(terminal.processPayment(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(updatedPaymentIntent)
        }

        val result = action.processPayment(mock()).first()

        Assertions.assertThat((result as ProcessPaymentStatus.Success).paymentIntent).isEqualTo(updatedPaymentIntent)
    }

    @Test
    fun `when processing payment succeeds, then flow is terminated`() = runTest {
        whenever(terminal.processPayment(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onSuccess(mock())
        }

        val result = action.processPayment(mock()).toList()

        Assertions.assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when processing payment fails, then flow is terminated`() = runTest {
        whenever(terminal.processPayment(any(), any())).thenAnswer {
            (it.arguments[1] as PaymentIntentCallback).onFailure(mock())
        }

        val result = action.processPayment(mock()).toList()

        Assertions.assertThat(result.size).isEqualTo(1)
    }
}
