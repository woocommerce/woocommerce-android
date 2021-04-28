package com.woocommerce.android.cardreader.internal.payments

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
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
import com.stripe.stripeterminal.model.external.TerminalException
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
import com.woocommerce.android.cardreader.CardPaymentStatus.UnexpectedError
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

private const val TIMEOUT = 1000L
private val DUMMY_AMOUNT = BigDecimal(0)
private val USD_CURRENCY = "USD"
private val NONE_USD_CURRENCY = "CZK"

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
    fun setUp() = runBlockingTest {
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

    // BEGIN - Arguments validation and conversion
    @Test
    fun `when currency not USD, then UnexpectedError emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, NONE_USD_CURRENCY).single()

        assertThat(result).isInstanceOf(UnexpectedError::class.java)
    }

    @Test
    fun `when currency is USD, then flow initiated`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(InitializingPayment).toList()

        assertThat(result.last()).isInstanceOf(InitializingPayment::class.java)
    }

    @Test
    fun `when payment flow started, then dollar amount converted to cents`() = runBlockingTest {
        val captor = argumentCaptor<Int>()

        manager.acceptPayment(BigDecimal(1), USD_CURRENCY).toList()

        verify(createPaymentAction).createPaymentIntent(captor.capture(), anyString())
        assertThat(captor.firstValue).isEqualTo(100)
    }

    @Test
    fun `when amount $1 ¢005, then it gets rounded down to ¢100`() = runBlockingTest {
        val captor = argumentCaptor<Int>()

        manager.acceptPayment(BigDecimal(1.005), USD_CURRENCY).toList()

        verify(createPaymentAction).createPaymentIntent(captor.capture(), anyString())
        assertThat(captor.firstValue).isEqualTo(100)
    }

    @Test
    fun `when amount $1 ¢006, then it gets rounded up to ¢101`() = runBlockingTest {
        val captor = argumentCaptor<Int>()

        manager.acceptPayment(BigDecimal(1.006), USD_CURRENCY).toList()

        verify(createPaymentAction).createPaymentIntent(captor.capture(), anyString())
        assertThat(captor.firstValue).isEqualTo(101)
    }

    @Test
    fun `when amount $1 ¢99, then it gets converted to ¢199`() = runBlockingTest {
        val captor = argumentCaptor<Int>()

        manager.acceptPayment(BigDecimal(1.99), USD_CURRENCY).toList()

        verify(createPaymentAction).createPaymentIntent(captor.capture(), anyString())
        assertThat(captor.firstValue).isEqualTo(199)
    }

    // END - Arguments validation and conversion
    // BEGIN - Creating Payment intent
    @Test
    fun `when creating payment intent starts, then InitializingPayment is emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(InitializingPayment).toList()

        assertThat(result.last()).isInstanceOf(InitializingPayment::class.java)
    }

    @Test
    fun `when creating payment intent fails, then InitializingPaymentFailed is emitted`() = runBlockingTest {
        whenever(createPaymentAction.createPaymentIntent(anyInt(), anyString()))
            .thenReturn(flow { emit(CreatePaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(InitializingPaymentFailed::class.java)
    }

    @Test
    fun `given status not REQUIRES_PAYMENT_METHOD, when creating payment finishes, then flow terminates`() =
        runBlockingTest {
            whenever(createPaymentAction.createPaymentIntent(anyInt(), anyString()))
                .thenReturn(flow { emit(CreatePaymentStatus.Success(createPaymentIntent(CANCELED))) })

            val result = withTimeoutOrNull(TIMEOUT) {
                manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()
            }

            assertThat(result).isNotNull // verify the flow did not timeout
            verify(collectPaymentAction, never()).collectPayment(anyOrNull())
        }

    // END - Creating Payment intent
    // BEGIN - Collecting Payment
    @Test
    fun `when collecting payment starts, then CollectingPayment is emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(CollectingPayment).toList()

        assertThat(result.last()).isInstanceOf(CollectingPayment::class.java)
    }

    @Test
    fun `when card reader awaiting input, then WaitingForInput emitted`() = runBlockingTest {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.ReaderInputRequested(mock())) })

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(WaitingForInput::class.java)
    }

    @Test
    fun `when card reader requests to display message, then ShowAdditionalInfo emitted`() = runBlockingTest {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.DisplayMessageRequested(mock())) })

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(ShowAdditionalInfo::class.java)
    }

    @Test
    fun `when collecting payment fails, then CollectingPaymentFailed is emitted`() = runBlockingTest {
        whenever(collectPaymentAction.collectPayment(anyOrNull()))
            .thenReturn(flow { emit(CollectPaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(CollectingPaymentFailed::class.java)
    }

    @Test
    fun `given exception contains PaymentIntent,when collecting payment fails, then PaymentData contain that intent`() =
        runBlockingTest {
            val paymentIntent = mock<PaymentIntent>()
            val terminalException = mock<TerminalException>().also {
                whenever(it.paymentIntent).thenReturn(paymentIntent)
            }
            whenever(collectPaymentAction.collectPayment(anyOrNull()))
                .thenReturn(flow { emit(CollectPaymentStatus.Failure(terminalException)) })

            val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

            assertThat((result.last() as CollectingPaymentFailed).paymentData)
                .isEqualTo(PaymentDataImpl(paymentIntent))
        }

    @Test
    fun `given exception doesn't contain PaymentIntent,when collecting payment fails, then data contain orig intent`() =
        runBlockingTest {
            val terminalException = mock<TerminalException>().also {
                whenever(it.paymentIntent).thenReturn(null)
            }
            whenever(collectPaymentAction.collectPayment(anyOrNull()))
                .thenReturn(flow { emit(CollectPaymentStatus.Failure(terminalException)) })

            val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

            val captor = argumentCaptor<PaymentIntent>()
            verify(collectPaymentAction).collectPayment(captor.capture())
            assertThat((result.last() as CollectingPaymentFailed).paymentData)
                .isEqualTo(PaymentDataImpl(captor.firstValue))
        }

    @Test
    fun `given status not REQUIRES_CONFIRMATION, when collecting payment finishes, then flow terminates`() =
        runBlockingTest {
            whenever(collectPaymentAction.collectPayment(anyOrNull()))
                .thenReturn(flow { emit(CollectPaymentStatus.Success(createPaymentIntent(CANCELED))) })

            val result = withTimeoutOrNull(TIMEOUT) {
                manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()
            }

            assertThat(result).isNotNull // verify the flow did not timeout
            verify(processPaymentAction, never()).processPayment(anyOrNull())
        }

    // END - Collecting Payment
    // BEGIN - Processing Payment
    @Test
    fun `when processing payment starts, then ProcessingPayment is emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(ProcessingPayment).toList()

        assertThat(result.last()).isInstanceOf(ProcessingPayment::class.java)
    }

    @Test
    fun `when processing payment fails, then ProcessingPaymentFailed is emitted`() = runBlockingTest {
        whenever(processPaymentAction.processPayment(anyOrNull()))
            .thenReturn(flow { emit(ProcessPaymentStatus.Failure(mock())) })

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(ProcessingPaymentFailed::class.java)
    }

    @Test
    fun `given exception contains PaymentIntent,when processing payment fails, then PaymentData contain that intent`() =
        runBlockingTest {
            val paymentIntent = mock<PaymentIntent>()
            val terminalException = mock<TerminalException>().also {
                whenever(it.paymentIntent).thenReturn(paymentIntent)
            }
            whenever(processPaymentAction.processPayment(anyOrNull()))
                .thenReturn(flow { emit(ProcessPaymentStatus.Failure(terminalException)) })

            val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

            assertThat((result.last() as ProcessingPaymentFailed).paymentData).isEqualTo(PaymentDataImpl(paymentIntent))
        }

    @Test
    fun `given exception doesn't contain PaymentIntent,when processing payment fails, then data contain orig intent`() =
        runBlockingTest {
            val terminalException = mock<TerminalException>().also {
                whenever(it.paymentIntent).thenReturn(null)
            }
            whenever(processPaymentAction.processPayment(anyOrNull()))
                .thenReturn(flow { emit(ProcessPaymentStatus.Failure(terminalException)) })

            val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

            val captor = argumentCaptor<PaymentIntent>()
            verify(processPaymentAction).processPayment(captor.capture())
            assertThat((result.last() as ProcessingPaymentFailed).paymentData)
                .isEqualTo(PaymentDataImpl(captor.firstValue))
        }

    @Test
    fun `given status not REQUIRES_CAPTURE, when processing payment finishes, then flow terminates`() =
        runBlockingTest {
            whenever(processPaymentAction.processPayment(anyOrNull()))
                .thenReturn(flow { emit(ProcessPaymentStatus.Success(createPaymentIntent(CANCELED))) })

            val result = withTimeoutOrNull(TIMEOUT) {
                manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()
            }

            assertThat(result).isNotNull // verify the flow did not timeout
            verify(cardReaderStore, never()).capturePaymentIntent(anyString())
        }

    // END - Processing Payment
    // BEGIN - Capturing Payment
    @Test
    fun `when capturing payment starts, then CapturingPayment is emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(CapturingPayment).toList()

        assertThat(result.last()).isInstanceOf(CapturingPayment::class.java)
    }

    @Test
    fun `when capturing payment succeeds, then PaymentCompleted is emitted`() = runBlockingTest {
        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY)
            .takeUntil(PaymentCompleted).toList()

        assertThat(result.last()).isInstanceOf(PaymentCompleted::class.java)
    }

    @Test
    fun `when capturing payment fails, then CapturingPaymentFailed is emitted`() = runBlockingTest {
        whenever(cardReaderStore.capturePaymentIntent(anyString())).thenReturn(false)

        val result = manager.acceptPayment(DUMMY_AMOUNT, USD_CURRENCY).toList()

        assertThat(result.last()).isInstanceOf(CapturingPaymentFailed::class.java)
    }
    // END - Capturing Payment

    // BEGIN - Retry
    @Test
    fun `given PaymentStatus REQUIRES_PAYMENT_METHOD, when retrying payment, then flow resumes on collectPayment`() =
        runBlockingTest {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(REQUIRES_PAYMENT_METHOD)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retry(paymentData).first()

            assertThat(result).isInstanceOf(CollectingPayment::class.java)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CONFIRMATION, when retrying payment, then flow resumes on processPayment`() =
        runBlockingTest {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(REQUIRES_CONFIRMATION)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retry(paymentData).first()

            assertThat(result).isInstanceOf(ProcessingPayment::class.java)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CAPTURE, when retrying payment, then flow resumes on capturePayment`() =
        runBlockingTest {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(REQUIRES_CAPTURE)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retry(paymentData).first()

            assertThat(result).isInstanceOf(CapturingPayment::class.java)
        }
    // END - Retry

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
                    throw IllegalStateException(
                        "`PaymentManagerTest.expectedSequence` does not match received " +
                            "events. Please verify that `PaymentManagerTest.expectedSequence` is defined correctly."
                    )
                }
            }
            .map { it.value }
}
