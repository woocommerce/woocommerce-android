package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.RefundCallback
import com.stripe.stripeterminal.external.models.Refund
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction.ProcessRefundStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction.ProcessRefundStatus.Success
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

@Suppress("DoNotMockDataClass")
@ExperimentalCoroutinesApi
internal class ProcessRefundActionTest : CardReaderBaseUnitTest() {
    private lateinit var action: ProcessRefundAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = ProcessRefundAction(terminal)
    }

    @Test
    fun `when process refund succeeds, then Success is emitted`() = testBlocking {
        whenever(terminal.processRefund(any(), any(), any())).thenAnswer {
            (it.arguments[2] as RefundCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund(mock(), mock()).first()

        assertThat(result).isExactlyInstanceOf(Success::class.java)
    }

    @Test
    fun `when process refund fails, then Failure is emitted`() = testBlocking {
        whenever(terminal.processRefund(any(), any(), any())).thenAnswer {
            (it.arguments[2] as RefundCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund(mock(), mock()).first()

        assertThat(result).isExactlyInstanceOf(Failure::class.java)
    }

    @Test
    fun `when process refund succeeds, then refund is returned`() = testBlocking {
        val refund = mock<Refund>()
        whenever(terminal.processRefund(any(), any(), any())).thenAnswer {
            (it.arguments[2] as RefundCallback).onSuccess(refund)
            mock<Cancelable>()
        }

        val result = action.processRefund(mock(), mock()).first()

        assertThat((result as Success).refund).isEqualTo(refund)
    }

    @Test
    fun `when process refund succeeds, then flow is terminated`() = testBlocking {
        whenever(terminal.processRefund(any(), any(), any())).thenAnswer {
            (it.arguments[2] as RefundCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund(mock(), mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when process refund fails, then flow is terminated`() = testBlocking {
        whenever(terminal.processRefund(any(), any(), any())).thenAnswer {
            (it.arguments[2] as RefundCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund(mock(), mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when job canceled, then process refund gets canceled`() =
        testBlocking {
            val cancelable = mock<Cancelable>()
            whenever(cancelable.isCompleted).thenReturn(false)
            whenever(terminal.processRefund(any(), any(), any())).thenAnswer { cancelable }
            val job = launch {
                action.processRefund(mock(), mock()).collect { }
            }

            job.cancel()
            joinAll(job)

            verify(cancelable).cancel(any())
        }
}
