package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction.CollectInteracRefundStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction.CollectInteracRefundStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CollectInteracRefundActionTest {
    private lateinit var action: CollectInteracRefundAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = CollectInteracRefundAction(terminal)
    }

    @Test
    fun `when collecting interac refund succeeds, then Success is emitted`() = runBlockingTest {
        whenever(terminal.refundPayment(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onSuccess()
            mock<Cancelable>()
        }

        val result = action.collectRefund(mock()).first()

        assertThat(result).isExactlyInstanceOf(Success::class.java)
    }

    @Test
    fun `when collecting interac refund fails, then Failure is emitted`() = runBlockingTest {
        whenever(terminal.refundPayment(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectRefund(mock()).first()

        assertThat(result).isExactlyInstanceOf(Failure::class.java)
    }

    @Test
    fun `when collecting interac refund succeeds, then flow is terminated`() = runBlockingTest {
        whenever(terminal.refundPayment(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onSuccess()
            mock<Cancelable>()
        }

        val result = action.collectRefund(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when collecting interac refund fails, then flow is terminated`() = runBlockingTest {
        whenever(terminal.refundPayment(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.collectRefund(mock()).toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `given flow not terminated, when job canceled, then interac refund gets canceled`() = runBlockingTest {
        val cancelable = mock<Cancelable>()
        whenever(cancelable.isCompleted).thenReturn(false)
        whenever(terminal.refundPayment(any(), any())).thenAnswer { cancelable }
        val job = launch {
            action.collectRefund(mock()).collect { }
        }

        job.cancel()
        joinAll(job)

        verify(cancelable).cancel(any())
    }
}
