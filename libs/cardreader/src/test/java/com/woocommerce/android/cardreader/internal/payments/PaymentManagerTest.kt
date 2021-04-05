package com.woocommerce.android.cardreader.internal.payments

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentIntentStatus
import com.stripe.stripeterminal.model.external.PaymentIntentStatus.CANCELED
import com.stripe.stripeterminal.model.external.PaymentIntentStatus.REQUIRES_CAPTURE
import com.stripe.stripeterminal.model.external.PaymentIntentStatus.REQUIRES_CONFIRMATION
import com.stripe.stripeterminal.model.external.PaymentIntentStatus.REQUIRES_PAYMENT_METHOD
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import test

private const val TIMEOUT = 1000L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PaymentManagerTest {
    private lateinit var manager: PaymentManager
    private val cardReaderStore: CardReaderStore = mock()
    private val createPaymentAction: CreatePaymentAction = mock()
    private val collectPaymentAction: CollectPaymentAction = mock()
    private val processPaymentAction: ProcessPaymentAction = mock()

    private val expectedSequence = listOf(
        InitializingPayment,
        CollectingPayment,
        ProcessingPayment,
        CapturingPayment,
        PaymentCompleted
    )

    @Before
    fun setUp() = test {
        manager = PaymentManager(cardReaderStore, createPaymentAction, collectPaymentAction, processPaymentAction)
        whenever(createPaymentAction.createPaymentIntent(anyInt(), anyString()))
            .thenReturn(flow {
                emit(CreatePaymentStatus.Success(createPaymentIntent(REQUIRES_PAYMENT_METHOD)))
            })

        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.Success(createPaymentIntent(REQUIRES_CONFIRMATION))) })

        whenever(processPaymentAction.processPayment(anyOrNull()))
            .thenReturn(flow { emit(ProcessPaymentStatus.Success(createPaymentIntent(REQUIRES_CAPTURE))) })

        whenever(cardReaderStore.capturePaymentIntent(anyString())).thenReturn(true)
    }

    // BEGIN - Creating Payment intent
    @Test
    fun `when creating payment intent starts, then InitializingPayment is emitted`() = test {
        val result = manager.acceptPayment(0, "")
            .takeUntil(InitializingPayment).toList()

        assertThat(result.last()).isInstanceOf(InitializingPayment::class.java)
    }

    @Test
    fun `when creating payment intent fails, then InitializingPaymentFailed is emitted`() = test {
        whenever(createPaymentAction.createPaymentIntent(anyInt(), anyString()))
            .thenReturn(flow { emit(CreatePaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(InitializingPaymentFailed::class.java)
    }

    @Test
    fun `given status not REQUIRES_PAYMENT_METHOD, when creating payment finishes, then flow terminates`() = test {
        whenever(createPaymentAction.createPaymentIntent(anyInt(), anyString()))
            .thenReturn(flow { emit(CreatePaymentStatus.Success(createPaymentIntent(CANCELED))) })

        val result = withTimeoutOrNull(TIMEOUT) {
            manager.acceptPayment(0, "").toList()
        }

        assertThat(result).isNotNull // verify the flow did not timeout
        verify(collectPaymentAction, never()).collectPayment(anyOrNull())
    }

    // END - Creating Payment intent
    // BEGIN - Collecting Payment
    @Test
    fun `when collecting payment starts, then CollectingPayment is emitted`() = test {
        val result = manager.acceptPayment(0, "")
            .takeUntil(CollectingPayment).toList()

        assertThat(result.last()).isInstanceOf(CollectingPayment::class.java)
    }

    @Test
    fun `when card reader awaiting input, then WaitingForInput emitted`() = test {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.ReaderInputRequested(mock())) })

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(WaitingForInput::class.java)
    }

    @Test
    fun `when card reader requests to display message, then ShowAdditionalInfo emitted`() = test {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.DisplayMessageRequested(mock())) })

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(ShowAdditionalInfo::class.java)
    }

    @Test
    fun `when collecting payment fails, then CollectingPaymentFailed is emitted`() = test {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(CollectingPaymentFailed::class.java)
    }

    @Test
    fun `given status not REQUIRES_CONFIRMATION, when collecting payment finishes, then flow terminates`() = test {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.Success(createPaymentIntent(CANCELED))) })

        val result = withTimeoutOrNull(TIMEOUT) {
            manager.acceptPayment(0, "").toList()
        }

        assertThat(result).isNotNull // verify the flow did not timeout
        verify(processPaymentAction, never()).processPayment(anyOrNull())
    }

    // END - Collecting Payment
    // BEGIN - Processing Payment
    @Test
    fun `when processing payment starts, then ProcessingPayment is emitted`() = test {
        val result = manager.acceptPayment(0, "")
            .takeUntil(ProcessingPayment).toList()

        assertThat(result.last()).isInstanceOf(ProcessingPayment::class.java)
    }

    @Test
    fun `when processing payment fails, then ProcessingPaymentFailed is emitted`() = test {
        whenever(processPaymentAction.processPayment(anyOrNull()))
            .thenReturn(flow { emit(ProcessPaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(ProcessingPaymentFailed::class.java)
    }

    @Test
    fun `given status not REQUIRES_CAPTURE, when processing payment finishes, then flow terminates`() = test {
        whenever(processPaymentAction.processPayment(anyOrNull()))
            .thenReturn(flow { emit(ProcessPaymentStatus.Success(createPaymentIntent(CANCELED))) })

        val result = withTimeoutOrNull(TIMEOUT) {
            manager.acceptPayment(0, "").toList()
        }

        assertThat(result).isNotNull // verify the flow did not timeout
        verify(cardReaderStore, never()).capturePaymentIntent(anyString())
    }

    // END - Processing Payment
    // BEGIN - Capturing Payment
    @Test
    fun `when capturing payment starts, then CapturingPayment is emitted`() = test {
        val result = manager.acceptPayment(0, "")
            .takeUntil(CapturingPayment).toList()

        assertThat(result.last()).isInstanceOf(CapturingPayment::class.java)
    }

    @Test
    fun `when capturing payment succeeds, then PaymentCompleted is emitted`() = test {
        val result = manager.acceptPayment(0, "")
            .takeUntil(PaymentCompleted).toList()

        assertThat(result.last()).isInstanceOf(PaymentCompleted::class.java)
    }

    @Test
    fun `when capturing payment fails, then CapturingPaymentFailed is emitted`() = test {
        whenever(cardReaderStore.capturePaymentIntent(anyString())).thenReturn(false)

        val result = manager.acceptPayment(0, "").toList()

        assertThat(result.last()).isInstanceOf(CapturingPaymentFailed::class.java)
    }
    // END - Capturing Payment

    private fun createPaymentIntent(status: PaymentIntentStatus): PaymentIntent =
        mock<PaymentIntent>().also {
            whenever(it.status).thenReturn(status)
            whenever(it.id).thenReturn("dummyId")
        }

    private fun <T> Flow<T>.takeUntil(untilStatus: CardPaymentStatus): Flow<T> =
        this.take(expectedSequence.indexOf(untilStatus) + 1)
            // the below lines are here just as a safeguard to verify that the expectedSequence is defined correctly
            .withIndex()
            .onEach {
                if (expectedSequence[it.index] != it.value) {
                    throw IllegalStateException("`PaymentManagerTest.expectedSequence` does not match received " +
                        "events. Please verify that `PaymentManagerTest.expectedSequence` is defined correctly.")
                }
            }
            .map { it.value }
}
