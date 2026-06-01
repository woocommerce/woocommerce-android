package com.woocommerce.android.ui.woopos.orders.details.refund

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.orders.WooPosLoadPaymentGateway
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRefundSubmissionProcessorTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val refundStore: WCRefundStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val paymentChargeRepository: PaymentChargeRepository = mock()
    private val loadPaymentGateway: WooPosLoadPaymentGateway = mock()
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val uiStringParser: UiStringParser = mock()

    private val site = SiteModel().apply { id = 1 }
    private val refundAmount = BigDecimal("22.00")
    private val refundItems = listOf(
        RefundRequestItem(
            itemId = 1L,
            quantity = 1,
            refundTotal = BigDecimal("20.00"),
            refundTax = emptyList()
        )
    )
    private val order = OrderTestUtils.generateTestOrder(orderId = 123L).copy(
        chargeId = "ch_123",
        currency = "CAD"
    )
    private val request = WooPosRefundSubmissionRequest(
        order = order,
        refundAmount = refundAmount,
        refundReason = "Customer request",
        refundItems = refundItems
    )
    private val refundModel = WCRefundModel(
        id = 1L,
        dateCreated = Date(),
        amount = refundAmount,
        reason = "",
        automaticGatewayRefund = false,
        items = emptyList(),
        shippingLineItems = emptyList(),
        feeLineItems = emptyList()
    )

    private lateinit var processor: WooPosRefundSubmissionProcessor

    @Before
    fun setUp() = runTest {
        whenever(selectedSite.get()).thenReturn(site)
        whenever(resourceProvider.getString(R.string.error_generic)).thenReturn("Something went wrong")
        whenever(resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found))
            .thenReturn("Unable to process refund.")
        whenever(loadPaymentGateway.invoke(any())).thenReturn(
            Result.success(
                PaymentGateway(
                    title = "WooPayments",
                    description = "",
                    isEnabled = true,
                    methodTitle = "WooPayments",
                    methodDescription = "",
                    supportsRefunds = true
                )
            )
        )
        whenever(
            refundStore.createItemsRefund(
                site = any(),
                orderId = any(),
                amount = any(),
                reason = any(),
                restockItems = any(),
                autoRefund = any(),
                items = any()
            )
        ).thenReturn(WooResult(refundModel))

        processor = WooPosRefundSubmissionProcessor(
            refundStore = refundStore,
            selectedSite = selectedSite,
            paymentChargeRepository = paymentChargeRepository,
            loadPaymentGateway = loadPaymentGateway,
            cardReaderPaymentControllerFactory = cardReaderPaymentControllerFactory,
            resourceProvider = resourceProvider,
            uiStringParser = uiStringParser
        )
    }

    @Test
    fun `given card present refund, when submitted, then backend refund is created directly`() = runTest {
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "visa",
                cardLast4 = "1234",
                paymentMethodType = CARD_PRESENT
            )
        )

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
            awaitComplete()
        }

        verify(cardReaderPaymentControllerFactory, never()).createRefund(any(), any(), any())
        verify(refundStore).createItemsRefund(
            site = eq(site),
            orderId = eq(order.id),
            amount = eq(refundAmount),
            reason = eq("Customer request"),
            restockItems = eq(true),
            autoRefund = eq(true),
            items = eq(refundItems)
        )
    }

    @Test
    fun `given payment gateway does not support refunds, when submitted, then backend refund is created manually`() =
        runTest {
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "visa",
                    cardLast4 = "1234",
                    paymentMethodType = CARD_PRESENT
                )
            )
            whenever(loadPaymentGateway.invoke(any())).thenReturn(
                Result.success(
                    PaymentGateway(
                        title = "Manual gateway",
                        description = "",
                        isEnabled = true,
                        methodTitle = "Manual gateway",
                        methodDescription = "",
                        supportsRefunds = false
                    )
                )
            )

            processor.submit(request).test {
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
                awaitComplete()
            }

            verify(refundStore).createItemsRefund(
                site = eq(site),
                orderId = eq(order.id),
                amount = eq(refundAmount),
                reason = eq("Customer request"),
                restockItems = eq(true),
                autoRefund = eq(false),
                items = eq(refundItems)
            )
        }

    @Test
    fun `given charge metadata lookup fails, when submitted, then failure is emitted`() = runTest {
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
        )

        processor.submit(request).test {
            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Something went wrong")
            assertThat(failure.retryBackendNotificationOnly).isFalse()
            assertThat(failure.canRetry).isTrue()
            awaitComplete()
        }

        verify(cardReaderPaymentControllerFactory, never()).createRefund(any(), any(), any())
        verify(refundStore, never()).createItemsRefund(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `given card refund has no charge id, when submitted, then failure is emitted`() = runTest {
        val requestWithoutChargeId = request.copy(
            order = order.copy(chargeId = null, paymentMethod = "woocommerce_payments")
        )

        processor.submit(requestWithoutChargeId).test {
            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Something went wrong")
            assertThat(failure.retryBackendNotificationOnly).isFalse()
            assertThat(failure.canRetry).isTrue()
            awaitComplete()
        }

        verify(paymentChargeRepository, never()).fetchCardDataUsedForOrderPayment(any())
        verify(cardReaderPaymentControllerFactory, never()).createRefund(any(), any(), any())
        verify(refundStore, never()).createItemsRefund(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `given selected site lookup fails during normal refund, when submitted, then failure is emitted`() = runTest {
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "visa",
                cardLast4 = "1234",
                paymentMethodType = CARD_PRESENT
            )
        )
        whenever(selectedSite.get()).thenThrow(IllegalStateException("missing site"))

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Something went wrong")
            assertThat(failure.retryBackendNotificationOnly).isFalse()
            assertThat(failure.canRetry).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `given submission is cancelled, when submitted, then cancellation is rethrown`() = runTest {
        val cancellation = CancellationException("cancelled")
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "visa",
                cardLast4 = "1234",
                paymentMethodType = CARD_PRESENT
            )
        )
        whenever(selectedSite.get()).thenThrow(cancellation)

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitError())
                .isInstanceOf(CancellationException::class.java)
                .hasMessage("cancelled")
        }
    }

    @Test
    fun `given interac refund, when reader succeeds, then backend is notified once`() = runTest {
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
            CardReaderInteracRefundState.LoadingData {}
        )
        val controller = mockController(paymentState)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "interac",
                cardLast4 = "1234",
                paymentMethodType = INTERAC_PRESENT
            )
        )
        whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
            .thenReturn(controller)

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.PreparingReader)

            paymentState.value = CardReaderInteracRefundState.CollectingInteracRefund("$22.00", {})
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.WaitingForCard())

            paymentState.value = CardReaderInteracRefundState.ProcessingInteracRefund("$22.00")
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.ProcessingReaderRefund)

            paymentState.value = CardReaderInteracRefundState.InteracRefundSuccessful("$22.00")
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.NotifyingStore)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
            awaitComplete()
        }

        verify(controller).start()
        verify(loadPaymentGateway, never()).invoke(any())
        verify(refundStore).createItemsRefund(
            site = eq(site),
            orderId = eq(order.id),
            amount = eq(refundAmount),
            reason = eq("Customer request"),
            restockItems = eq(true),
            autoRefund = eq(false),
            items = eq(refundItems)
        )
    }

    @Test
    fun `given interac refund controller starts with payment loading state, when submitted, then state is ignored`() =
        runTest {
            val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData {}
            )
            val controller = mockController(paymentState)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "interac",
                    cardLast4 = "1234",
                    paymentMethodType = INTERAC_PRESENT
                )
            )
            whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
                .thenReturn(controller)

            processor.submit(request).test {
                advanceUntilIdle()
                expectNoEvents()

                paymentState.value = CardReaderInteracRefundState.LoadingData {}
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.PreparingReader)

                paymentState.value = CardReaderInteracRefundState.InteracRefundSuccessful("$22.00")
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.NotifyingStore)
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
                awaitComplete()
            }
        }

    @Test
    fun `given backend notification fails after interac success, when refund is submitted, then failure is backend retry only`() = runTest {
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
            CardReaderInteracRefundState.LoadingData {}
        )
        val controller = mockController(paymentState)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "interac",
                cardLast4 = "1234",
                paymentMethodType = INTERAC_PRESENT
            )
        )
        whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
            .thenReturn(controller)
        whenever(
            refundStore.createItemsRefund(
                site = any(),
                orderId = any(),
                amount = any(),
                reason = any(),
                restockItems = any(),
                autoRefund = any(),
                items = any()
            )
        ).thenReturn(
            WooResult(
                error = WooError(
                    type = WooErrorType.GENERIC_ERROR,
                    original = GenericErrorType.UNKNOWN,
                    message = "Backend failed"
                )
            )
        )

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.PreparingReader)

            paymentState.value = CardReaderInteracRefundState.InteracRefundSuccessful("$22.00")
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.NotifyingStore)

            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Backend failed")
            assertThat(failure.retryBackendNotificationOnly).isTrue()
            assertThat(failure.retryCardRefund).isFalse()
            assertThat(failure.canRetry).isFalse()
            awaitComplete()
        }

        verify(loadPaymentGateway, never()).invoke(any())
    }

    @Test
    fun `given backend-only notification request, when submitted, then card reader is not started`() = runTest {
        val retryRequest = request.copy(cardRefundAlreadySucceeded = true)

        processor.submit(retryRequest).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.NotifyingStore)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
            awaitComplete()
        }

        verify(cardReaderPaymentControllerFactory, never()).createRefund(any(), any(), any())
        verify(paymentChargeRepository, never()).fetchCardDataUsedForOrderPayment(any())
        verify(loadPaymentGateway, never()).invoke(any())
        verify(refundStore).createItemsRefund(
            site = eq(site),
            orderId = eq(order.id),
            amount = eq(refundAmount),
            reason = eq("Customer request"),
            restockItems = eq(true),
            autoRefund = eq(false),
            items = eq(refundItems)
        )
    }

    @Test
    fun `given backend-only notification throws, when submitted, then backend retry failure is emitted`() = runTest {
        val retryRequest = request.copy(cardRefundAlreadySucceeded = true)
        whenever(selectedSite.get()).thenThrow(IllegalStateException("missing site"))

        processor.submit(retryRequest).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.NotifyingStore)

            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Something went wrong")
            assertThat(failure.retryBackendNotificationOnly).isTrue()
            assertThat(failure.retryCardRefund).isFalse()
            assertThat(failure.canRetry).isFalse()
            awaitComplete()
        }

        verify(cardReaderPaymentControllerFactory, never()).createRefund(any(), any(), any())
        verify(paymentChargeRepository, never()).fetchCardDataUsedForOrderPayment(any())
        verify(loadPaymentGateway, never()).invoke(any())
    }

    @Test
    fun `given interac reader failure, when submitted, then failure can retry card refund`() = runTest {
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
            CardReaderInteracRefundState.LoadingData {}
        )
        val controller = mockController(paymentState)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "interac",
                cardLast4 = "1234",
                paymentMethodType = INTERAC_PRESENT
            )
        )
        whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
            .thenReturn(controller)
        whenever(uiStringParser.asString(InteracRefundFlowError.Generic.message)).thenReturn("Reader failed")

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.PreparingReader)

            paymentState.value = CardReaderInteracRefundState.InteracRefundFailure.Cancelable(
                amountWithCurrencyLabel = "$22.00",
                errorType = InteracRefundFlowError.Generic,
                onRetry = {},
                onCancel = {},
            )

            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Reader failed")
            assertThat(failure.retryBackendNotificationOnly).isFalse()
            assertThat(failure.retryCardRefund).isTrue()
            assertThat(failure.canRetry).isTrue()
            awaitComplete()
        }

        verify(refundStore, never()).createItemsRefund(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `given non retryable interac reader failure, when submitted, then failure cannot retry card refund`() = runTest {
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
            CardReaderInteracRefundState.LoadingData {}
        )
        val controller = mockController(paymentState)
        whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                cardBrand = "interac",
                cardLast4 = "1234",
                paymentMethodType = INTERAC_PRESENT
            )
        )
        whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
            .thenReturn(controller)
        whenever(uiStringParser.asString(InteracRefundFlowError.Generic.message)).thenReturn("Reader failed")

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.PreparingReader)

            paymentState.value = CardReaderInteracRefundState.InteracRefundFailure.Cancelable(
                amountWithCurrencyLabel = "$22.00",
                errorType = InteracRefundFlowError.Generic,
                onRetry = null,
                onCancel = {},
            )

            val failure = awaitItem() as WooPosRefundSubmissionState.Failure
            assertThat(failure.message).isEqualTo("Reader failed")
            assertThat(failure.retryBackendNotificationOnly).isFalse()
            assertThat(failure.retryCardRefund).isFalse()
            assertThat(failure.canRetry).isFalse()
            awaitComplete()
        }

        verify(refundStore, never()).createItemsRefund(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `given controller exit arrives without error, when submitted, then non retryable failure is emitted`() =
        runTest {
            val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData {}
            )
            val controller = mockController(
                paymentState = paymentState,
                eventFlow = flowOf(CardReaderPaymentEvent.Exit)
            )
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "interac",
                    cardLast4 = "1234",
                    paymentMethodType = INTERAC_PRESENT
                )
            )
            whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
                .thenReturn(controller)

            processor.submit(request).test {
                advanceUntilIdle()
                val failure = awaitItem() as WooPosRefundSubmissionState.Failure
                assertThat(failure.message).isEqualTo("Something went wrong")
                assertThat(failure.canRetry).isFalse()
                awaitComplete()
            }
        }

    @Test
    fun `given controller exit arrives before reader not connected error, when submitted, then reader connection is required`() =
        runTest {
            val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData {}
            )
            val controller = mockController(
                paymentState = paymentState,
                eventFlow = flowOf(
                    CardReaderPaymentEvent.Exit,
                    CardReaderPaymentEvent.ShowErrorMessage(R.string.card_reader_payment_reader_not_connected)
                )
            )
            whenever(
                resourceProvider.getString(R.string.card_reader_payment_reader_not_connected)
            ).thenReturn("Please make sure that the card reader is connected.")
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "interac",
                    cardLast4 = "1234",
                    paymentMethodType = INTERAC_PRESENT
                )
            )
            whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
                .thenReturn(controller)

            processor.submit(request).test {
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.ReaderConnectionRequired)
                awaitComplete()
            }
        }

    @Test
    fun `given reader is not connected for interac refund, when submitted, then reader connection is required`() =
        runTest {
            val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData {}
            )
            val controller = mockController(
                paymentState = paymentState,
                eventFlow = flowOf(
                    CardReaderPaymentEvent.ShowErrorMessage(R.string.card_reader_payment_reader_not_connected),
                    CardReaderPaymentEvent.Exit
                )
            )
            whenever(
                resourceProvider.getString(R.string.card_reader_payment_reader_not_connected)
            ).thenReturn("Please make sure that the card reader is connected.")
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment("ch_123")).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "interac",
                    cardLast4 = "1234",
                    paymentMethodType = INTERAC_PRESENT
                )
            )
            whenever(cardReaderPaymentControllerFactory.createRefund(any(), any(), any()))
                .thenReturn(controller)

            processor.submit(request).test {
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.ReaderConnectionRequired)
                awaitComplete()
            }
        }

    private fun mockController(
        paymentState: MutableStateFlow<CardReaderPaymentOrRefundState>,
        eventFlow: Flow<CardReaderPaymentEvent> = MutableSharedFlow()
    ): CardReaderPaymentController {
        return mock {
            on { this.paymentState }.thenReturn(paymentState)
            on { event }.thenReturn(eventFlow)
        }
    }

    private companion object {
        private const val INTERAC_PRESENT = "interac_present"
        private const val CARD_PRESENT = "card_present"
    }
}
