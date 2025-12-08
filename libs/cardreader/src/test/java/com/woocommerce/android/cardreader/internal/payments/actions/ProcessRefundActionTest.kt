package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction.ProcessRefundStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction.ProcessRefundStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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
    fun `when process refund succeeds, then Success is returned`() = testBlocking {
        whenever(terminal.processRefund(any(), any())).thenReturn(mock())

        val result = action.processRefund(mock(), mock())

        assertThat(result).isExactlyInstanceOf(Success::class.java)
    }

    @Test
    fun `when process refund fails, then Failure is returned`() = testBlocking {
        whenever(terminal.processRefund(any(), any())).thenAnswer { throw mock<TerminalException>() }

        val result = action.processRefund(mock(), mock())

        assertThat(result).isExactlyInstanceOf(Failure::class.java)
    }

    @Test
    fun `when process refund succeeds, then refund is returned`() = testBlocking {
        val refund = mock<Refund>()
        whenever(terminal.processRefund(any(), any())).thenReturn(refund)

        val result = action.processRefund(mock(), mock())

        assertThat((result as Success).refund).isEqualTo(refund)
    }
}
