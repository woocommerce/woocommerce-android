package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.CardPresentDetails
import com.stripe.stripeterminal.external.models.Charge
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentStatus
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.CANCELED
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.REQUIRES_CAPTURE
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.REQUIRES_CONFIRMATION
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.REQUIRES_PAYMENT_METHOD
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.SUCCEEDED
import com.stripe.stripeterminal.external.models.PaymentMethodDetails
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPaymentCompleted
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.reflect.KClass

private const val TIMEOUT = 1000L
private val DUMMY_AMOUNT = BigDecimal(0)
private const val DUMMY_PAYMENT_DESCRIPTION = "test description"
private const val DUMMY_ORDER_ID = 5L
private const val USD_CURRENCY = "USD"
private const val NONE_USD_CURRENCY = "CZK"
private const val DUMMY_EMAIL = "test@test.test"
private const val DUMMY_CUSTOMER_NAME = "Tester"
private const val DUMMY_SITE_URL = "www.test.test/test"
private const val DUMMY_STORE_NAME = "Test store"

@Suppress("DoNotMockDataClass")
@ExperimentalCoroutinesApi
class PaymentManagerTest : CardReaderBaseUnitTest() {
    private lateinit var manager: PaymentManager
    private val terminalWrapper: TerminalWrapper = mock()
    private val cardReaderStore: CardReaderStore = mock()
    private val createPaymentAction: CreatePaymentAction = mock()
    private val processPaymentIntentAction: ProcessPaymentIntentAction = mock()
    private val cancelPaymentAction: CancelPaymentAction = mock()
    private val paymentErrorMapper: PaymentErrorMapper = mock()
    private val paymentUtils: PaymentUtils = mock()
    private val cardReaderConfigFactory: CardReaderConfigFactory = mock()

    private val expectedSequence = listOf(
        InitializingPayment::class,
        ProcessingPayment::class,
        ProcessingPaymentCompleted::class,
        CapturingPayment::class,
        PaymentCompleted::class
    )

    @Before
    fun setUp() = testBlocking {
        manager = PaymentManager(
            terminalWrapper,
            cardReaderStore,
            createPaymentAction,
            processPaymentIntentAction,
            cancelPaymentAction,
            paymentUtils,
            paymentErrorMapper,
            cardReaderConfigFactory,
        )
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        val createPaymentIntentResponse = createPaymentIntent(REQUIRES_PAYMENT_METHOD)
        val processPaymentIntentResponse = createPaymentIntent(REQUIRES_CAPTURE)

        whenever(createPaymentAction.createPaymentIntent(any()))
            .thenReturn(CreatePaymentStatus.Success(createPaymentIntentResponse))

        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(ProcessPaymentIntentStatus.Success(processPaymentIntentResponse))

        whenever(cardReaderStore.capturePaymentIntent(any(), anyString()))
            .thenReturn(CapturePaymentResponse.Successful.Success)
        whenever(paymentErrorMapper.mapTerminalError(anyOrNull(), anyOrNull()))
            .thenReturn(PaymentFailed(CardPaymentStatusErrorType.Generic, null, ""))
        whenever(paymentErrorMapper.mapCapturePaymentError(anyOrNull(), anyOrNull()))
            .thenReturn(PaymentFailed(CardPaymentStatusErrorType.Generic, null, ""))
        whenever(paymentErrorMapper.mapError(anyOrNull(), anyOrNull()))
            .thenReturn(PaymentFailed(CardPaymentStatusErrorType.Generic, null, ""))
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(
            CardReaderConfigForUSA
        )
        whenever(paymentUtils.isSupportedCurrency(any(), any())).thenReturn(true)
    }

    @Test
    fun `when currency not supported, then error emitted`() = testBlocking {
        whenever(paymentUtils.isSupportedCurrency(any(), any())).thenReturn(false)
        val result = manager.acceptPayment(createPaymentInfo(currency = NONE_USD_CURRENCY)).single()

        assertThat(result).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when currency supported, then flow initiated`() = testBlocking {
        whenever(paymentUtils.isSupportedCurrency(any(), any())).thenReturn(true)
        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(InitializingPayment::class).toList()

        assertThat(result.last()).isInstanceOf(InitializingPayment::class.java)
    }

    @Test
    fun `given Terminal not initialized, when flow started, then error emitted`() = testBlocking {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        val result = manager.acceptPayment(createPaymentInfo()).single()

        assertThat(result).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when creating payment intent starts, then InitializingPayment is emitted`() = testBlocking {
        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(InitializingPayment::class).toList()

        assertThat(result.last()).isInstanceOf(InitializingPayment::class.java)
    }

    @Test
    fun `when creating payment intent fails, then error emitted`() = testBlocking {
        whenever(createPaymentAction.createPaymentIntent(anyOrNull()))
            .thenReturn(CreatePaymentStatus.Failure(mock()))

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when creating payment intent fails, then mapTerminalError invoked`() = testBlocking {
        whenever(createPaymentAction.createPaymentIntent(anyOrNull()))
            .thenReturn(CreatePaymentStatus.Failure(mock()))

        manager
            .acceptPayment(createPaymentInfo()).toList()

        verify(paymentErrorMapper).mapTerminalError(anyOrNull(), anyOrNull())
    }

    @Test
    fun `given status not REQUIRES_PAYMENT_METHOD, when creating payment finishes, then flow terminates`() =
        testBlocking {
            val canceledIntent = createPaymentIntent(CANCELED)
            whenever(createPaymentAction.createPaymentIntent(anyOrNull()))
                .thenReturn(CreatePaymentStatus.Success(canceledIntent))

            val result = withTimeoutOrNull(TIMEOUT) {
                manager
                    .acceptPayment(createPaymentInfo())
                    .toList()
            }

            assertThat(result).isNotNull
            verify(processPaymentIntentAction, never()).processPaymentIntent(anyOrNull())
        }

    @Test
    fun `when processing payment starts, then ProcessingPayment is emitted`() = testBlocking {
        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(ProcessingPayment::class).toList()

        assertThat(result.last()).isInstanceOf(ProcessingPayment::class.java)
    }

    @Test
    fun `when processing payment fails, then error emitted`() = testBlocking {
        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(ProcessPaymentIntentStatus.Failure(mock()))

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when processing payment fails, then mapTerminalError invoked`() = testBlocking {
        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(ProcessPaymentIntentStatus.Failure(mock()))

        manager
            .acceptPayment(createPaymentInfo()).toList()

        verify(paymentErrorMapper).mapTerminalError(anyOrNull(), anyOrNull())
    }

    @Test
    fun `given status not REQUIRES_CAPTURE, when processing payment finishes, then flow terminates`() =
        testBlocking {
            val canceledIntent = createPaymentIntent(CANCELED)
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(canceledIntent))

            val result = withTimeoutOrNull(TIMEOUT) {
                manager
                    .acceptPayment(createPaymentInfo())
                    .toList()
            }

            assertThat(result).isNotNull
            verify(cardReaderStore, never()).capturePaymentIntent(any(), anyString())
        }

    @Test
    fun `given interac payment, when processing payment finishes successfully, then capture payment is emitted`() =
        testBlocking {
            val succeededInteracIntent = createPaymentIntent(SUCCEEDED, interacPresentDetails = mock())
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(succeededInteracIntent))

            val result = manager
                .acceptPayment(createPaymentInfo()).takeUntil(CapturingPayment::class).toList()

            assertThat(result.last()).isInstanceOf(CapturingPayment::class.java)
        }

    @Test
    fun `given interac payment, when processing payment finishes with canceled status, then flow terminates`() =
        testBlocking {
            val canceledInteracIntent = createPaymentIntent(CANCELED, interacPresentDetails = mock())
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(canceledInteracIntent))

            val result = withTimeoutOrNull(TIMEOUT) {
                manager
                    .acceptPayment(createPaymentInfo())
                    .toList()
            }

            assertThat(result).isNotNull
            verify(cardReaderStore, never()).capturePaymentIntent(any(), anyString())
        }

    @Test
    fun `given processing payment suc with card present, when processing, then ProcessingPaymentCompleted emitted`() =
        testBlocking {
            val intent = createPaymentIntent(REQUIRES_CAPTURE)
            val paymentsMethodDetails = mock<PaymentMethodDetails> {
                on { cardPresentDetails }.thenReturn(mock())
            }

            val charge = mock<Charge> {
                on { paymentMethodDetails }.thenReturn(paymentsMethodDetails)
            }
            val charges = listOf(charge)
            whenever(intent.getCharges()).thenReturn(charges)
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(intent))

            val result = manager.acceptPayment(createPaymentInfo()).toList()

            assertThat(result).contains(ProcessingPaymentCompleted(PaymentMethodType.CARD_PRESENT))
        }

    @Test
    fun `given processing payment suc with interact pres, when processing, then ProcessingPaymentCompleted emitted`() =
        testBlocking {
            val intent = createPaymentIntent(REQUIRES_CAPTURE)
            val paymentsMethodDetails = mock<PaymentMethodDetails> {
                on { interacPresentDetails }.thenReturn(mock())
            }

            val charge = mock<Charge> {
                on { paymentMethodDetails }.thenReturn(paymentsMethodDetails)
            }
            val charges = listOf(charge)
            whenever(intent.getCharges()).thenReturn(charges)
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(intent))

            val result = manager.acceptPayment(createPaymentInfo()).toList()

            assertThat(result).contains(ProcessingPaymentCompleted(PaymentMethodType.INTERAC_PRESENT))
        }

    @Test
    fun `given processing payment suc with unknown, when processing, then ProcessingPaymentCompleted emitted`() =
        testBlocking {
            val captureIntent = createPaymentIntent(REQUIRES_CAPTURE)
            whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
                .thenReturn(ProcessPaymentIntentStatus.Success(captureIntent))

            val result = manager.acceptPayment(createPaymentInfo()).toList()

            assertThat(result).contains(ProcessingPaymentCompleted(PaymentMethodType.UNKNOWN))
        }

    @Test
    fun `when receiptUrl is empty, then PaymentFailed emitted`() = testBlocking {
        val noReceiptIntent = createPaymentIntent(REQUIRES_CAPTURE, receiptUrl = null)
        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(
                ProcessPaymentIntentStatus.Success(noReceiptIntent)
            )

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when receiptUrl is empty, then PaymentData for retry are empty`() = testBlocking {
        val noReceiptIntent = createPaymentIntent(REQUIRES_CAPTURE, receiptUrl = null)
        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(ProcessPaymentIntentStatus.Success(noReceiptIntent))

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat((result.last() as PaymentFailed).paymentDataForRetry).isNull()
    }

    @Test
    fun `when capturing payment starts, then CapturingPayment is emitted`() = testBlocking {
        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(CapturingPayment::class).toList()

        assertThat(result.last()).isInstanceOf(CapturingPayment::class.java)
    }

    @Test
    fun `when capturing payment succeeds, then PaymentCompleted is emitted`() = testBlocking {
        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(PaymentCompleted::class).toList()

        assertThat(result.last()).isInstanceOf(PaymentCompleted::class.java)
    }

    @Test
    fun `when capturing payment succeeds, then PaymentCompleted event contains receipt url`() = testBlocking {
        val expectedReceiptUrl = "abcd"
        val intentWithReceipt = createPaymentIntent(REQUIRES_CAPTURE, receiptUrl = expectedReceiptUrl)
        whenever(processPaymentIntentAction.processPaymentIntent(anyOrNull()))
            .thenReturn(ProcessPaymentIntentStatus.Success(intentWithReceipt))

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat((result.last() as PaymentCompleted).receiptUrl).isEqualTo(expectedReceiptUrl)
    }

    @Test
    fun `given payment already captured, when capturing payment, then PaymentCompleted is emitted`() = testBlocking {
        whenever(cardReaderStore.capturePaymentIntent(any(), anyString()))
            .thenReturn(CapturePaymentResponse.Successful.PaymentAlreadyCaptured)

        val result = manager.acceptPayment(createPaymentInfo())
            .takeUntil(PaymentCompleted::class).toList()

        assertThat(result.last()).isInstanceOf(PaymentCompleted::class.java)
    }

    @Test
    fun `when capturing payment fails, then error emitted`() = testBlocking {
        whenever(cardReaderStore.capturePaymentIntent(any(), anyString()))
            .thenReturn(CapturePaymentResponse.Error.GenericError(""))

        val result = manager
            .acceptPayment(createPaymentInfo()).toList()

        assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
    }

    @Test
    fun `when capturing payment fails, then mapCapturePaymentError invoked`() = testBlocking {
        whenever(cardReaderStore.capturePaymentIntent(any(), anyString()))
            .thenReturn(CapturePaymentResponse.Error.GenericError(""))

        manager
            .acceptPayment(createPaymentInfo()).toList()

        verify(paymentErrorMapper).mapCapturePaymentError(anyOrNull(), anyOrNull())
    }

    @Test
    fun `given PaymentStatus REQUIRES_PAYMENT_METHOD, when retrying payment, then flow resumes on processPayment`() =
        testBlocking {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(REQUIRES_PAYMENT_METHOD)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retryPayment(DUMMY_ORDER_ID, paymentData).first()

            assertThat(result).isInstanceOf(ProcessingPayment::class.java)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CONFIRMATION, when retrying payment, then flow resumes on processPayment`() =
        testBlocking {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(REQUIRES_CONFIRMATION)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retryPayment(DUMMY_ORDER_ID, paymentData).first()

            assertThat(result).isInstanceOf(ProcessingPayment::class.java)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CAPTURE, when retrying payment, then flow resumes on capturePayment`() =
        testBlocking {
            val paymentIntent = createPaymentIntent(REQUIRES_CAPTURE)
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager.retryPayment(DUMMY_ORDER_ID, paymentData).first()

            assertThat(result).isInstanceOf(CapturingPayment::class.java)
        }

    @Test
    fun `given PaymentStatus CANCELED, when retrying payment, then PaymentFailed is emitted`() =
        testBlocking {
            val paymentIntent = createPaymentIntent(CANCELED)
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager
                .retryPayment(1, paymentData).toList()

            assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
        }

    @Test
    fun `given PaymentStatus null, when retrying payment, then PaymentFailed is emitted`() =
        testBlocking {
            val paymentIntent = mock<PaymentIntent>().also {
                whenever(it.status).thenReturn(null)
            }
            val paymentData = PaymentDataImpl(paymentIntent)

            val result = manager
                .retryPayment(1, paymentData).toList()

            assertThat(result.last()).isInstanceOf(PaymentFailed::class.java)
        }

    @Test
    fun `given PaymentStatus REQUIRES_PAYMENT_METHOD, when canceling payment, then payment intent canceled`() =
        testBlocking {
            val paymentIntent = createPaymentIntent(REQUIRES_PAYMENT_METHOD)
            val paymentData = PaymentDataImpl(paymentIntent)

            manager.cancelPayment(paymentData)

            verify(cancelPaymentAction).cancelPayment(paymentIntent)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CONFIRMATION, when canceling payment, then payment intent canceled`() =
        testBlocking {
            val paymentIntent = createPaymentIntent(REQUIRES_CONFIRMATION)
            val paymentData = PaymentDataImpl(paymentIntent)

            manager.cancelPayment(paymentData)

            verify(cancelPaymentAction).cancelPayment(paymentIntent)
        }

    @Test
    fun `given PaymentStatus REQUIRES_CAPTURE, when canceling payment, then payment intent NOT canceled`() =
        testBlocking {
            val paymentIntent = createPaymentIntent(REQUIRES_CAPTURE)
            val paymentData = PaymentDataImpl(paymentIntent)

            manager.cancelPayment(paymentData)

            verify(cancelPaymentAction, never()).cancelPayment(paymentIntent)
        }

    @Test
    fun `given supported country, when createPaymentIntentOnly called, then returns Success`() = testBlocking {
        // GIVEN
        val paymentIntent = createPaymentIntent(REQUIRES_PAYMENT_METHOD)
        whenever(paymentIntent.clientSecret).thenReturn("secret")
        whenever(createPaymentAction.createPaymentIntent(any()))
            .thenReturn(CreatePaymentStatus.Success(paymentIntent))

        // WHEN
        val result = manager.createPaymentIntentOnly(createPaymentInfo())

        // THEN
        assertThat(result).isInstanceOf(CreatePaymentIntentResult.Success::class.java)
    }

    @Test
    fun `given unsupported country, when createPaymentIntentOnly called, then returns Failed`() = testBlocking {
        // GIVEN
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any()))
            .thenReturn(CardReaderConfigForUnsupportedCountry)

        // WHEN
        val result = manager.createPaymentIntentOnly(createPaymentInfo(countryCode = "ZZ"))

        // THEN
        assertThat(result).isInstanceOf(CreatePaymentIntentResult.Failed::class.java)
        verify(createPaymentAction, never()).createPaymentIntent(any())
    }

    private fun createPaymentIntent(
        status: PaymentIntentStatus,
        receiptUrl: String? = "test url",
        interacPresentDetails: CardPresentDetails? = null
    ): PaymentIntent =
        mock<PaymentIntent>().also {
            whenever(it.status).thenReturn(status)
            whenever(it.id).thenReturn("dummyId")
            val paymentMethodDetails = mock<PaymentMethodDetails>()
            whenever(paymentMethodDetails.interacPresentDetails).thenReturn(interacPresentDetails)
            val charge = mock<Charge>()
            whenever(charge.receiptUrl).thenReturn(receiptUrl)
            whenever(charge.paymentMethodDetails).thenReturn(paymentMethodDetails)
            whenever(it.getCharges()).thenReturn(listOf(charge))
        }

    private fun <T> Flow<T>.takeUntil(untilStatus: KClass<*>): Flow<T> =
        this.take(expectedSequence.indexOf(untilStatus) + 1)
            .withIndex()
            .onEach {
                if (expectedSequence[it.index] != it.value!!::class) {
                    throw IllegalStateException(
                        "`PaymentManagerTest.expectedSequence` does not match received " +
                            "events. Please verify that `PaymentManagerTest.expectedSequence` is defined correctly."
                    )
                }
            }
            .map { it.value }

    private fun createPaymentInfo(
        paymentDescription: String = DUMMY_PAYMENT_DESCRIPTION,
        orderId: Long = DUMMY_ORDER_ID,
        amount: BigDecimal = DUMMY_AMOUNT,
        currency: String = USD_CURRENCY,
        customerEmail: String? = DUMMY_EMAIL,
        customerName: String? = DUMMY_CUSTOMER_NAME,
        wcpayCanSendReceipt: Boolean = false,
        storeName: String? = DUMMY_STORE_NAME,
        siteUrl: String? = DUMMY_SITE_URL,
        orderKey: String? = null,
        statementDescriptor: String? = null,
        countryCode: String = "US",
        feeAmount: Long? = null,
    ): PaymentInfo =
        PaymentInfo(
            paymentDescription = paymentDescription,
            orderId = orderId,
            amount = amount,
            currency = currency,
            customerEmail = customerEmail,
            isPluginCanSendReceipt = wcpayCanSendReceipt,
            customerName = customerName,
            storeName = storeName,
            siteUrl = siteUrl,
            orderKey = orderKey,
            statementDescriptor = StatementDescriptor(statementDescriptor),
            countryCode = countryCode,
            feeAmount = feeAmount,
            channel = PaymentInfo.PaymentChannel.StoreManager
        )
}
