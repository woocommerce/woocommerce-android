package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.RefundCallback
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessInteracRefundAction.ProcessRefundStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ProcessInteracRefundActionTest {
    private lateinit var action: ProcessInteracRefundAction
    private val terminal: TerminalWrapper = mock()

    @Before
    fun setUp() {
        action = ProcessInteracRefundAction(terminal)
    }

    @Test
    fun `when processing interac refund succeeds, then Success is emitted`() = runBlockingTest {
        whenever(terminal.processRefund(any())).thenAnswer {
            (it.arguments[0] as RefundCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund().first()

        assertThat(result).isExactlyInstanceOf(ProcessRefundStatus.Success::class.java)
    }

    @Test
    fun `when processing interac refund fails, then Failure is emitted`() = runBlockingTest {
        whenever(terminal.processRefund(any())).thenAnswer {
            (it.arguments[0] as RefundCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund().first()

        assertThat(result).isExactlyInstanceOf(ProcessRefundStatus.Failure::class.java)
    }

    @Test
    fun `when processing interac refund succeeds, then flow is terminated`() = runBlockingTest {
        whenever(terminal.processRefund(any())).thenAnswer {
            (it.arguments[0] as RefundCallback).onSuccess(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund().toList()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `when processing interac refund fails, then flow is terminated`() = runBlockingTest {
        whenever(terminal.processRefund(any())).thenAnswer {
            (it.arguments[0] as RefundCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.processRefund().toList()

        assertThat(result.size).isEqualTo(1)
    }
}
