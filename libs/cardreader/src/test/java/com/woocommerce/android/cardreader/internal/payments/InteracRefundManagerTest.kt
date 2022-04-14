package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessInteracRefundAction
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.RefundParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.reflect.KClass

private val DUMMY_AMOUNT = BigDecimal(0)
private const val USD_CURRENCY = "USD"
private const val DUMMY_CHARGE_ID = "ch_abcdefgh"
private const val TIMEOUT = 1000L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class InteracRefundManagerTest {
    private lateinit var manager: InteracRefundManager

    private val collectInteracRefundAction: CollectInteracRefundAction = mock()
    private val processInteracRefundAction: ProcessInteracRefundAction = mock()

    private val expectedInteracRefundSequence = listOf(
        CardInteracRefundStatus.CollectingInteracRefund::class,
        CardInteracRefundStatus.ProcessingInteracRefund::class,
        CardInteracRefundStatus.InteracRefundSuccess::class
    )

    @Before
    fun setUp() = runBlockingTest {
        manager = InteracRefundManager(
            collectInteracRefundAction,
            processInteracRefundAction
        )
    }

    @Test
    fun `when interac refund starts, then CollectingInteracRefund is emitted`() = runBlockingTest {
        val result = manager.refundInteracPayment(createRefundParams())
            .takeUntil(CardInteracRefundStatus.CollectingInteracRefund::class).toList()

        assertThat(result.last()).isInstanceOf(CardInteracRefundStatus.CollectingInteracRefund::class.java)
    }

    @Test
    fun `given collect interac refund success, when refund starts, then ProcessingInteracRefund is emitted`() =
        runBlockingTest {
            whenever(collectInteracRefundAction.collectRefund(anyOrNull()))
                .thenReturn(flow { emit(CollectInteracRefundAction.CollectInteracRefundStatus.Success) })
            val result = manager.refundInteracPayment(createRefundParams())
                .takeUntil(CardInteracRefundStatus.ProcessingInteracRefund::class).toList()

            assertThat(result.last()).isInstanceOf(CardInteracRefundStatus.ProcessingInteracRefund::class.java)
        }

    @Test
    fun `given collect interac refund failure, when refund starts, then ProcessingInteracRefund is NOT emitted`() =
        runBlockingTest {
            whenever(collectInteracRefundAction.collectRefund(anyOrNull()))
                .thenReturn(flow { emit(CollectInteracRefundAction.CollectInteracRefundStatus.Failure(mock())) })
            val result = manager.refundInteracPayment(createRefundParams()).toList()

            assertThat(result.last()).isNotInstanceOf(CardInteracRefundStatus.ProcessingInteracRefund::class.java)
            verify(processInteracRefundAction, never()).processRefund()
        }

    @Test
    fun `given collect interac refund failure, when refund starts, then failure is emitted`() =
        runBlockingTest {
            whenever(collectInteracRefundAction.collectRefund(anyOrNull()))
                .thenReturn(flow { emit(CollectInteracRefundAction.CollectInteracRefundStatus.Failure(mock())) })
            val result = manager.refundInteracPayment(createRefundParams()).toList()

            assertThat(result.last()).isInstanceOf(CardInteracRefundStatus.InteracRefundFailure::class.java)
        }

    @Test
    fun `given collect interac refund failure, when refund starts, then flow terminates`() =
        runBlockingTest {
            whenever(collectInteracRefundAction.collectRefund(anyOrNull()))
                .thenReturn(flow { emit(CollectInteracRefundAction.CollectInteracRefundStatus.Failure(mock())) })
            val result = withTimeoutOrNull(TIMEOUT) {
                manager.refundInteracPayment(createRefundParams()).toList()
            }

            assertThat(result).isNotNull // verify the flow did not timeout
        }

    private fun <T> Flow<T>.takeUntil(untilStatus: KClass<*>): Flow<T> =
        this.take(expectedInteracRefundSequence.indexOf(untilStatus) + 1)
            // the below lines are here just as a safeguard to verify that the
            // expectedInteracRefundSequence is defined correctly
            .withIndex()
            .onEach {
                if (expectedInteracRefundSequence[it.index] != it.value!!::class) {
                    throw IllegalStateException(
                        "`PaymentManagerTest.expectedSequence` does not match received " +
                            "events. Please verify that `PaymentManagerTest.expectedSequence` is defined correctly."
                    )
                }
            }
            .map { it.value }

    private fun createRefundParams(
        chargeId: String = DUMMY_CHARGE_ID,
        amount: BigDecimal = DUMMY_AMOUNT,
        currency: String = USD_CURRENCY
    ): RefundParams =
        RefundParams(
            chargeId = chargeId,
            amount = amount,
            currency = currency
        )
}
