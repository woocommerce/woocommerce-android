package com.woocommerce.android.ui.payments.cardreader.payment.controller

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BatteryStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages.CardReaderNoMessage
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CARD_REMOVED_TOO_EARLY
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CHECK_MOBILE_DEVICE
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_OR_SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.MULTIPLE_CONTACTLESS_CARDS_DETECTED
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.REMOVE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.RETRY_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_READ_METHOD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Server
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPaymentCompleted
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundableChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError.AmountTooSmall
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError.Unknown
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.LoadingDataState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent.PlaySuccessfulPaymentSound
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent.ShowErrorMessage
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentSuccessful
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.reflect.KMutableProperty0
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CardReaderPaymentControllerTest : BaseUnitTest() {
    private lateinit var controller: CardReaderPaymentController

    private val cardReaderManager: CardReaderManager = mock()
    private val orderRepository: OrderDetailRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val tracker: PaymentsFlowTracker = mock()
    private val trackCanceledFlow = CardReaderTrackCanceledFlowAction(tracker)
    private val appPrefs: AppPrefs = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooStore: WooCommerceStore = mock()
    private val errorMapper: CardReaderPaymentErrorMapper = mock()
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper = mock()
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper = mock()
    private val interacRefundableChecker: CardReaderInteracRefundableChecker = mock()
    private val paymentStateProvider = CardReaderPaymentStateProvider()
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper = mock()
    private val paymentReceiptHelper: PaymentReceiptHelper = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val paymentReceiptShare: PaymentReceiptShare = mock()

    private var isTTPinProgress = false
    private val isTTPinProgressProp: KMutableProperty0<Boolean> = ::isTTPinProgress

    private val paymentParam = PaymentOrRefund.Payment(ORDER_ID, ORDER)

    private val mockedOrder = mock<Order>()
    private val mockedAddress = mock<Address>()

    private val paymentFailedWithEmptyDataForRetry = PaymentFailed(Generic, null, "dummy msg")
    private val paymentFailedWithValidDataForRetry = PaymentFailed(Generic, mock(), "dummy msg")
    private val paymentFailedWithServerError = PaymentFailed(Server(""), mock(), "dummy msg")

    @OptIn(InternalCoroutinesApi::class)
    @Before
    fun setUp() = testBlocking {
        createController()
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
        whenever(orderRepository.getOrderById(any())).thenReturn(mockedOrder)

        whenever(mockedOrder.total).thenReturn(DUMMY_TOTAL)
        whenever(mockedOrder.currency).thenReturn("GBP")
        whenever(mockedOrder.billingAddress).thenReturn(mockedAddress)
        whenever(mockedAddress.email).thenReturn("")
        whenever(mockedAddress.firstName).thenReturn("Tester")
        whenever(mockedAddress.lastName).thenReturn("Test")
        whenever(mockedOrder.orderKey).thenReturn("wc_order_j0LMK3bFhalEL")
        whenever(mockedOrder.id).thenReturn(ORDER_ID)

        whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
        whenever(appPrefs.getCardReaderStatementDescriptor(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn("test statement descriptor")
        whenever(paymentReceiptHelper.isPluginCanSendReceipt(siteModel)).thenReturn(true)

        whenever(cardReaderPaymentOrderHelper.getAmountLabel(mockedOrder))
            .thenReturn("${DUMMY_CURRENCY_SYMBOL}${DUMMY_TOTAL}")
        whenever(cardReaderManager.batteryStatus).thenAnswer { flow { emit(CardReaderBatteryStatus.Unknown) } }
        whenever(currencyFormatter.formatAmountWithCurrency(DUMMY_TOTAL.toDouble(), "GBP"))
            .thenReturn("${DUMMY_CURRENCY_SYMBOL}${DUMMY_TOTAL}")
        whenever(mockedOrder.billingAddress).thenReturn(mockedAddress)
        whenever(mockedAddress.email).thenReturn("")
        whenever(mockedAddress.firstName).thenReturn("Tester")
        whenever(mockedAddress.lastName).thenReturn("Test")
        whenever(mockedOrder.orderKey).thenReturn("wc_order_j0LMK3bFhalEL")
        whenever(mockedOrder.chargeId).thenReturn("chargeId")
        whenever(cardReaderManager.collectPayment(any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.retryCollectPayment(any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(interacRefundableChecker.isRefundable(any())).thenReturn(true)
        whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
            flow<BluetoothCardReaderMessages> {}
        }
        whenever(paymentReceiptHelper.getReceiptUrl(ORDER_ID)).thenReturn(Result.success("test url"))
        whenever(cardReaderPaymentOrderHelper.getPaymentDescription(mockedOrder)).thenReturn("test description")
        whenever(cardReaderPaymentOrderHelper.getReceiptDocumentName(mockedOrder.id)).thenReturn("receipt-order-1")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createController(
        cardReaderType: CardReaderType = CardReaderType.EXTERNAL,
        cardReaderFlowParam: PaymentOrRefund = paymentParam,
    ) {
        controller = CardReaderPaymentController(
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            selectedSite = selectedSite,
            appPrefs = appPrefs,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            interacRefundableChecker = interacRefundableChecker,
            tracker = tracker,
            trackCancelledFlow = trackCanceledFlow,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            interacRefundErrorMapper = interacRefundErrorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            paymentStateProvider = paymentStateProvider,
            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
            paymentReceiptHelper = paymentReceiptHelper,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            paymentReceiptShare = paymentReceiptShare,
            paymentOrRefund = cardReaderFlowParam,
            cardReaderType = cardReaderType,
            isTTPPaymentInProgress = isTTPinProgressProp,
        )
    }

    @Test
    fun `given collect payment shown, when RETRY message received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_retry_card_prompt)
        }

    @Test
    fun `given collect payment shown, when INSERT_CARD received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when INSERT_OR_SWIPE_CARD received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when SWIPE_CARD received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when REMOVE_CARD received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_remove_card_prompt)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_CARD message received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD)
            advanceUntilIdle()

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_try_another_card_prompt)
        }

    @Test
    fun `given collect payment shown, when CARD_REMOVED_TOO_EARLY message received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(CARD_REMOVED_TOO_EARLY)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_card_removed_too_early)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_READ message received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value = BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_try_another_read_method_prompt)
        }

    @Test
    fun `given collect payment shown, when MULTIPLE_CARDS_DETECTED received, then collect payment hint updated`() =
        testBlocking {
            val paymentStatus = MutableStateFlow<CardPaymentStatus>(InitializingPayment)
            val readerMessages = MutableStateFlow<BluetoothCardReaderMessages>(CardReaderNoMessage)
            whenever(cardReaderManager.collectPayment(any())).thenReturn(paymentStatus)
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenReturn(readerMessages)
            paymentStatus.value = CollectingPayment

            controller.start()

            readerMessages.value =
                BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED)

            assertThat((controller.paymentState.value as CardReaderPaymentState.CollectingPayment).cardReaderHint)
                .isEqualTo(R.string.card_reader_payment_multiple_contactless_cards_detected_prompt)
        }

    @Test
    fun `given fetching order fails, when payment screen shown, then ExternalReaderFailedPayment state is shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(ExternalReaderFailedPayment::class.java)
        }

    @Test
    fun `given fetching order fails and tpp, when payment screen shown, then BuiltInReaderFailedPayment state is shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            createController(cardReaderType = BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(BuiltInReaderFailedPayment::class.java)
        }

    @Test
    fun `when fetching order fails, then event tracked`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            controller.start()

            verify(tracker).trackPaymentFailed(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given fetching order succeeds, when payment screen shown, then order currency stored `() =
        testBlocking {
            controller.start()

            verify(cardReaderTrackingInfoKeeper).setCurrency(("GBP"))
        }

    @Test
    fun `when payment screen shown, then loading data state is emitted`() {
        controller.start()

        assertThat(controller.paymentState.value).isInstanceOf(CardReaderPaymentState.LoadingData::class.java)
    }

    @Test
    fun `when payment not collectable, then error event emitted and flow terminated`() =
        testBlocking {
            whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
            val events = mutableListOf<CardReaderPaymentEvent>()
            val job = launch {
                controller.event.collect {
                    events.add(it)
                }
            }

            controller.start()

            assertThat(
                (events[0] as ShowErrorMessage).message
            ).isEqualTo(
                R.string.card_reader_payment_order_paid_payment_cancelled
            )
            assertThat(events[1]).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
            job.cancel()
        }

    @Test
    fun `when flow started, then correct payment description is propagated to CardReaderManager`() =
        testBlocking {
            val siteName = "testName"
            val siteId = 12345L
            val expectedResult = "hooray"
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    name = siteName
                    url = ""
                    this.siteId = siteId
                }
            )
            whenever(cardReaderPaymentOrderHelper.getPaymentDescription(mockedOrder)).thenReturn(expectedResult)
            val captor = argumentCaptor<PaymentInfo>()

            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.paymentDescription).isEqualTo(expectedResult)
        }

    @Test
    fun `when flow started, then correct statement descriptor is propagated to CardReaderManager`() =
        testBlocking {
            val expectedResult = "hooray"
            whenever(appPrefs.getCardReaderStatementDescriptor(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(expectedResult)
            val captor = argumentCaptor<PaymentInfo>()

            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.statementDescriptor.value).isEqualTo(expectedResult)
        }

    @Test
    fun `given payment type WOO_POS, when start, then pos channel passed`() =
        testBlocking {
            val captor = argumentCaptor<PaymentInfo>()
            createController(
                cardReaderFlowParam = PaymentOrRefund.Payment(
                    ORDER_ID,
                    PaymentOrRefund.Payment.PaymentType.WOO_POS
                )
            )
            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.channel).isEqualTo(PaymentInfo.PaymentChannel.Pos)
        }

    @Test
    fun `given payment type ORDER, when start, then store manager channel passed`() =
        testBlocking {
            val captor = argumentCaptor<PaymentInfo>()
            createController(
                cardReaderFlowParam = PaymentOrRefund.Payment(
                    ORDER_ID,
                    ORDER
                )
            )
            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.channel).isEqualTo(PaymentInfo.PaymentChannel.StoreManager)
        }

    @Test
    fun `given payment type SIMPLE, when start, then store manager channel passed`() =
        testBlocking {
            val captor = argumentCaptor<PaymentInfo>()
            createController(
                cardReaderFlowParam = PaymentOrRefund.Payment(
                    ORDER_ID,
                    PaymentOrRefund.Payment.PaymentType.SIMPLE
                )
            )
            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.channel).isEqualTo(PaymentInfo.PaymentChannel.StoreManager)
        }

    @Test
    fun `given payment type ORDER_CREATION, when paymentCollect, then store manager channel passed`() =
        testBlocking {
            val captor = argumentCaptor<PaymentInfo>()
            createController(
                cardReaderFlowParam = PaymentOrRefund.Payment(
                    ORDER_ID,
                    PaymentOrRefund.Payment.PaymentType.ORDER_CREATION
                )
            )
            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.channel).isEqualTo(PaymentInfo.PaymentChannel.StoreManager)
        }

    @Test
    fun `given payment type TRY_TAP_TO_PAY, when paymentCollect, then store manager channel passed`() =
        testBlocking {
            val captor = argumentCaptor<PaymentInfo>()
            createController(
                cardReaderFlowParam = PaymentOrRefund.Payment(
                    ORDER_ID,
                    PaymentOrRefund.Payment.PaymentType.TRY_TAP_TO_PAY
                )
            )
            controller.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.channel).isEqualTo(PaymentInfo.PaymentChannel.StoreManager)
        }

    @Test
    fun `when initializing payment, then Loading state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }

            controller.start()

            assertThat(controller.paymentState.value).isInstanceOf(CardReaderPaymentState.LoadingData::class.java)
        }

    @Test
    fun `when collecting payment, then CollectingPayment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState::class.java)
        }

    @Test
    fun `given built in reader,when collecting payment, then CollectingPayment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            createController(BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.CollectingPayment.BuiltInReaderCollectPaymentState::class.java)
        }

    @Test
    fun `when processing payment, then ProcessingPayment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment::class.java)
        }

    @Test
    fun `given built in reader, when processing payment, then ProcessingPayment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            createController(BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.ProcessingPayment.BuiltInReaderProcessingPayment::class.java)
        }

    @Test
    fun `when processing payment completed with card present, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.CARD_PRESENT)) }
            }

            controller.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("card")
        }

    @Test
    fun `when processing payment completed with interac present, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.INTERAC_PRESENT)) }
            }

            controller.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("card_interac")
        }

    @Test
    fun `when processing payment completed with interac present, then track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.INTERAC_PRESENT)) }
            }

            controller.start()

            verify(tracker).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with card present, then do not track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.CARD_PRESENT)) }
            }

            controller.start()

            verify(tracker, never()).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with unknown type, then do not track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.UNKNOWN)) }
            }

            controller.start()

            verify(tracker, never()).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with unknown, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.UNKNOWN)) }
            }

            controller.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("unknown")
        }

    @Test
    fun `when capturing payment, then capturing payment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PaymentCapturing.ExternalReaderPaymentCapturing::class.java)
        }

    @Test
    fun `given built in reader, when capturing payment, then capturing payment state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PaymentCapturing.BuiltInReaderPaymentCapturing::class.java)
        }

    @Test
    fun `given billing email empty, when external payment completed, then payment successful state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(PaymentSuccessful.ExternalReaderPaymentSuccessful::class.java)
        }

    @Test
    fun `given billing email empty, when built in payment completed, then payment successful state emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(PaymentSuccessful.BuiltInReaderPaymentSuccessful::class.java)
        }

    @Test
    fun `given billing not empty, when external payment completed, then payment successful receipt sent state emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            controller.start()

            assertThat(controller.paymentState.value).isInstanceOf(
                PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically::class.java
            )
        }

    @Test
    fun `given billing not empty, when built in payment completed, then built in payment successful receipt sent state emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            createController(BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value).isInstanceOf(
                PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically::class.java
            )
        }

    @Test
    fun `when payment completed, then success sound is played`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            val events = mutableListOf<CardReaderPaymentEvent>()
            val job = launch {
                controller.event.collect {
                    events.add(it)
                }
            }
            controller.start()

            assertThat(events[0]).isInstanceOf(PlaySuccessfulPaymentSound::class.java)
            job.cancel()
        }

    @Test
    fun `when payment completed, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            controller.start()

            verify(tracker).trackPaymentSucceeded()
        }

    @Test
    fun `given external reader, when payment fails, then failed state emitted`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(ExternalReaderFailedPayment::class.java)
        }

    @Test
    fun `given external reader fails with Unknown error, when flow starts, then CTA with contact support button is provided`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            val externalReaderFailedPaymentState =
                controller.paymentState.value as ExternalReaderFailedPayment
            assertThat(externalReaderFailedPaymentState.cta).isNotNull
            assertThat(externalReaderFailedPaymentState.cta!!.label).isEqualTo(R.string.support_contact)
        }

    @Test
    fun `given built in reader fails with Unknown error, when view model starts, then CTA with contact support button is provided`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            createController(BUILT_IN)

            controller.start()

            val state = controller.paymentState.value as BuiltInReaderFailedPayment
            assertThat(state.cta).isNotNull
            assertThat(state.cta!!.label).isEqualTo(R.string.support_contact)
        }

    @Test
    fun `given external reader fails with generic error, when contact support clicked, then contact support emitted and flow canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            val events: List<CardReaderPaymentEvent> = controller.event.runAndCaptureValues {
                (controller.paymentState.value as ExternalReaderFailedPayment).cta!!.onCallToActionTapped()
            }

            assertThat(events[0]).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
            assertThat(events[1]).isInstanceOf(CardReaderPaymentEvent.ContactSupportTapped::class.java)
        }

    @Test
    fun `when contact support clicked, then contact support event tracked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).cta!!.onCallToActionTapped()

            verify(tracker).trackPaymentFailedContactSupportTapped()
        }

    @Test
    fun `given built in reader fails with generic error, when contact support clicked, then contact support emitted and flow canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            createController(BUILT_IN)

            controller.start()

            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).cta!!.onCallToActionTapped()
            }

            assertThat(events[0]).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
            assertThat(events[1]).isInstanceOf(CardReaderPaymentEvent.ContactSupportTapped::class.java)
        }

    @Test
    fun `given built in reader, when payment fails, then ui updated to built in failed state`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            createController(BUILT_IN)

            controller.start()

            assertThat(controller.paymentState.value)
                .isInstanceOf(BuiltInReaderFailedPayment::class.java)
        }

    @Test
    fun `when payment fails, then invalidate onboarding cache`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            verify(cardReaderOnboardingChecker).invalidateCache()
        }

    @Test
    fun `when payment fails, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()

            verify(tracker).trackPaymentFailed(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given external reader, when payment fails because of AMOUNT_TOO_SMALL, then failed state is not retryable`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            val errorType = DeclinedByBackendError.AmountTooSmallWooPayments(
                "Amount must be at least US$0.50",
                30,
                "USD"
            )
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    errorType,
                    false
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow {
                    emit(
                        PaymentFailed(
                            errorType,
                            mock(),
                            "dummy msg"
                        )
                    )
                }
            }

            controller.start()

            assertNull(
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry
            )
            assertNotNull(
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onCancel
            )
        }

    @Test
    fun `given external reader, when payment fails not because of AMOUNT_TOO_SMALL, then failed state is retryable`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), false))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }

            controller.start()

            assertNotNull(
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry,
            )
        }

    @Test
    fun `given built in reader, when payment fails not because of AMOUNT_TOO_SMALL, then failed state has Try again button`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), true))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()

            assertNotNull(
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry,
            )
        }

    @Test
    fun `given built in reader, when payment fails PIN_REQUIRED, then failed state has purchase card reader cta`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.CardDeclined.PinRequired,
                    true
                )
            ).thenReturn(PaymentFlowError.BuiltInReader.PinRequired)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.CardDeclined.PinRequired, mock(), "dummy msg")) }
            }
            createController(BUILT_IN)

            controller.start()

            assertEquals(
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).cta!!.label,
                R.string.card_reader_payment_payment_failed_purchase_hardware_reader
            )
        }

    @Test
    fun `given built in reader, when purchase button clicked, then purchase even emmited`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.CardDeclined.PinRequired,
                    true
                )
            ).thenReturn(PaymentFlowError.BuiltInReader.PinRequired)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.CardDeclined.PinRequired, mock(), "dummy msg")) }
            }
            whenever(wooStore.getStoreCountryCode(siteModel)).thenReturn("US")
            createController(cardReaderType = BUILT_IN)
            controller.start()
            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).cta!!.onCallToActionTapped()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PurchaseCardReaderTapped::class.java)
            assertThat((events.last() as CardReaderPaymentEvent.PurchaseCardReaderTapped).url).isEqualTo(
                "https://woocommerce.com/products/hardware/US"
            )
        }

    @Test
    fun `given external reader, when payment fails because of AMOUNT_TOO_SMALL, then clicking on ok button triggers exit event`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            val errorType = DeclinedByBackendError.AmountTooSmallWooPayments(
                "Amount must be at least US$0.50",
                30,
                "USD"
            )
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    errorType,
                    false
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow {
                    emit(
                        PaymentFailed(
                            errorType,
                            mock(),
                            "dummy msg"
                        )
                    )
                }
            }
            controller.start()
            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onCancel!!()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given built in reader, when payment fails because of AMOUNT_TOO_SMALL, then clicking on ok button triggers exit event`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            val errorType = DeclinedByBackendError.AmountTooSmallWooPayments(
                "Amount must be at least US$0.50",
                30,
                "USD"
            )
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    errorType,
                    true
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow {
                    emit(
                        PaymentFailed(
                            errorType,
                            mock(),
                            "dummy msg"
                        )
                    )
                }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()
            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onCancel!!()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given payment fails because of NFC_DISABLED, when primary button clicked, then tracked and enablenfc emitted`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled,
                    true
                )
            ).thenReturn(PaymentFlowError.BuiltInReader.NfcDisabled)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow {
                    emit(
                        PaymentFailed(
                            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled,
                            null,
                            "message"
                        )
                    )
                }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()

            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).cta!!.onCallToActionTapped()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.EnableNfcTapped::class.java)
            verify(tracker).trackPaymentFailedEnabledNfcTapped()
        }

    @Test
    fun `given user clicks on retry and external, when payment fails and retryData are null, then flow restarted from scratch`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            controller.start()
            clearInvocations(cardReaderManager)

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given user clicks on retry and built in, when payment fails and retryData are null, then flow restarted from scratch`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()
            clearInvocations(cardReaderManager)

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given failed payment and external reader, when user retries, then retryCollectPayment invoked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), any())
        }

    @Test
    fun `given failed payment and built-in reader, when user retries, then retryCollectPayment invoked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), any())
        }

    @Test
    fun `given failed payment and external reader, when user retries, then flow retried with provided PaymentData`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), eq(paymentData))
        }

    @Test
    fun `given failed payment and built-in reader, when user retries, then flow retried with provided PaymentData`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onRetry!!()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), eq(paymentData))
        }

    @Test
    fun `given external failed payment, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            controller.start()

            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onCancel!!()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given built in failed payment, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, true))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            createController(cardReaderType = BUILT_IN)

            controller.start()

            val events = controller.event.runAndCaptureValues {
                (controller.paymentState.value as CardReaderPaymentState.PaymentFailed).onCancel!!()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `when loading data, then cancellation is possible`() = testBlocking {
        controller.start()
        val paymentState = controller.paymentState.value

        assertThat(paymentState).isInstanceOf(CardReaderPaymentState.LoadingData::class.java)
        assertNotNull((paymentState as CardReaderPaymentState.LoadingData).onCancel)
    }

    @Test
    fun `when collecting payment, then cancellation is possible`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            controller.start()
            val paymentState =
                controller.paymentState.value as
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState

            assertNotNull(paymentState.onCancel)
        }

    @Test
    fun `when processing payment, then cancellation is possible`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            controller.start()
            val paymentState = controller.paymentState.value as
                CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment
            assertNotNull(paymentState.onCancel)
        }

    @Test
    fun `when payment fails, then cancellation is possible`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            controller.start()
            val paymentState = controller.paymentState.value as
                ExternalReaderFailedPayment

            assertNotNull(paymentState.onCancel)
        }

    @Test
    fun `when payment succeeds, then receiptUrl stored into a persistant storage`() =
        testBlocking {
            val receiptUrl = "testUrl"
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted(receiptUrl)) }
            }

            controller.start()

            verify(paymentReceiptHelper).storeReceiptUrl(eq(ORDER_ID), eq(receiptUrl))
        }

    @Test
    fun `given payment flow already started, when start() is invoked, then flow is not restarted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow<CardPaymentStatus> {}
            }

            controller.start()
            controller.start()
            controller.start()
            controller.start()

            verify(cardReaderManager, times(1))
                .collectPayment(anyOrNull())
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given billing email empty and built in, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
                state.onPrintReceiptClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)
            controller.start()
            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as
                    PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
                state.onPrintReceiptClicked()
            }
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            (controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful).onPrintReceiptClicked()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PrintingReceipt::class.java)
        }

    @Test
    fun `given billing email empty and built-in, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()

            (controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful).onPrintReceiptClicked()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PrintingReceipt::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
                ).onPrintReceiptClicked()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PrintingReceipt::class.java)
        }

    @Test
    fun `given billing email not empty and built-in, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()

            (
                controller.paymentState.value as
                    PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
                ).onPrintReceiptClicked()

            assertThat(controller.paymentState.value)
                .isInstanceOf(CardReaderPaymentState.PrintingReceipt::class.java)
        }

    @Test
    fun `given billing email empty and external, when print result received, then payment successful state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()
            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessful
                ).onPrintReceiptClicked()

            controller.onPrintResult(CANCELLED)

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(PaymentSuccessful.ExternalReaderPaymentSuccessful::class.java)
        }

    @Test
    fun `given billing email empty and built in, when print result received, then payment successful state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(BUILT_IN)

            controller.start()
            (controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful).onPrintReceiptClicked()

            controller.onPrintResult(CANCELLED)

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(PaymentSuccessful.BuiltInReaderPaymentSuccessful::class.java)
        }

    @Test
    fun `given billing email not empty and external, when print result received, then payment success receipt sent state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()
            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
                ).onPrintReceiptClicked()

            controller.onPrintResult(CANCELLED)

            assertThat(controller.paymentState.value)
                .isInstanceOf(
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically::class.java
                )
        }

    @Test
    fun `given billing email not empty and built in, when print result received, then payment success receipt sent state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            controller.start()
            (
                controller.paymentState.value as
                    PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
                ).onPrintReceiptClicked()

            controller.onPrintResult(CANCELLED)

            assertThat(controller.paymentState.value)
                .isInstanceOf(
                    PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically::class.java
                )
        }

    @Test
    fun `given in printing receipt state and external, when view recreated, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()
            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }

            controller.onViewCreated()

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given in printing receipt state and built in, when view recreated, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)
            controller.start()
            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }

            controller.onViewCreated()

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.PrintReceiptTapped::class.java)
        }

    @Test
    fun `given not in printing receipt state, when view recreated, then state not changed`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow<CardPaymentStatus> {}
            }
            controller.start()
            val originalState = controller.paymentState.value
            assertThat(originalState).isNotInstanceOf(CardReaderPaymentState.PrintingReceipt::class.java)

            controller.onViewCreated()

            assertThat(controller.paymentState.value).isEqualTo(originalState)
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessful
                ).onPrintReceiptClicked()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email empty and built in, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            controller.start()

            val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
            state.onPrintReceiptClicked()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
                ).onPrintReceiptClicked()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            controller.start()

            val state = controller.paymentState.value as
                PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
            state.onPrintReceiptClicked()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given get receipt url fails, when user clicks on print receipt button, then error event emitted`() =
        testBlocking {
            // GIVEN
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception()))

            // WHEN
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }

            // THEN
            assertThat(
                (events[events.size - 2] as ShowErrorMessage).message
            ).isEqualTo(R.string.receipt_fetching_error)
            assertThat(
                (events[events.size - 1])
            ).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given fetching receipt URL fails, when startPrintingFlow, then payment flow is canceled`() = testBlocking {
        val errorMessage = "Receipt fetching error"
        whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception(errorMessage)))
        whenever(cardReaderManager.collectPayment(any())).thenAnswer {
            flow { emit(PaymentCompleted("")) }
        }
        controller.start()
        val events = controller.event.runAndCaptureValues {
            (
                controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessful
                ).onPrintReceiptClicked()
        }

        verify(tracker).trackReceiptUrlFetchingFails(errorDescription = errorMessage)
        assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
    }

    @Test
    fun `given get receipt url succeeds, when user clicks on print receipt button, then PrintReceipt emitted`() =
        testBlocking {
            // GIVEN
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            val receiptUrl = "testUrl"
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.success(receiptUrl))

            // WHEN
            controller.start()
            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onPrintReceiptClicked()
            }
            // THEN
            assertThat((events.last() as CardReaderPaymentEvent.PrintReceiptTapped).receiptUrl).isEqualTo(receiptUrl)
            assertThat(
                (events.last() as CardReaderPaymentEvent.PrintReceiptTapped).documentName
            ).isEqualTo("receipt-order-1")
        }

    @Test
    fun `when OS accepts the print request, then print success event tracked`() = testBlocking {
        controller.onPrintResult(STARTED)

        verify(tracker).trackPrintReceiptSucceeded()
    }

    @Test
    fun `when OS refuses the print request, then print failed event tracked`() = testBlocking {
        controller.onPrintResult(FAILED)

        verify(tracker).trackPrintReceiptFailed()
    }

    @Test
    fun `when manually cancels the print request, then print cancelled event tracked`() = testBlocking {
        controller.onPrintResult(CANCELLED)

        verify(tracker).trackPrintReceiptCancelled()
    }

    @Test
    fun `given external reader and receipt fetching and sharing success, when user clicks on send receipt button, then PlayChaChing emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("url")) }
            }
            whenever(paymentReceiptShare("test url", 1L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Success
            )

            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat(events.last()).isEqualTo(PlaySuccessfulPaymentSound)
        }

    @Test
    fun `given built in reader and receipt fetching  and sharing success, when user clicks on send receipt button, then PlayChaChing emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat(events.last()).isEqualTo(PlaySuccessfulPaymentSound)
        }

    @Test
    fun `given receipt fetching success and receipt file not created, when user clicks on send receipt button, then ShowSnackbar emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("url")) }
            }
            whenever(paymentReceiptShare("test url", 1L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Error.FileCreation
            )
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat((events.last() as ShowErrorMessage).message).isEqualTo(
                R.string.card_reader_payment_receipt_can_not_be_stored
            )
            verify(tracker).trackPaymentsReceiptSharingFailed(PaymentReceiptShare.ReceiptShareResult.Error.FileCreation)
        }

    @Test
    fun `given receipt fetching success and receipt file not downloaded, when user clicks on send receipt button, then ShowSnackbar emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("url")) }
            }
            whenever(paymentReceiptShare("test url", 1L)).thenReturn(
                PaymentReceiptShare.ReceiptShareResult.Error.FileDownload
            )
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat((events.last() as ShowErrorMessage).message).isEqualTo(
                R.string.card_reader_payment_receipt_can_not_be_downloaded
            )
            verify(tracker).trackPaymentsReceiptSharingFailed(PaymentReceiptShare.ReceiptShareResult.Error.FileDownload)
        }

    @Test
    fun `given receipt fetching success and receipt file not shared, when user clicks on send receipt button, then ShowSnackbar emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("url")) }
            }
            val sharing = PaymentReceiptShare.ReceiptShareResult.Error.Sharing(Exception())
            whenever(paymentReceiptShare("test url", 1L)).thenReturn(sharing)
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat((events.last() as ShowErrorMessage).message).isEqualTo(
                R.string.card_reader_payment_email_client_not_found
            )
            verify(tracker).trackPaymentsReceiptSharingFailed(sharing)
        }

    @Test
    fun `given external reader and receipt fetching fails, when user clicks on send receipt button, then ShowSnackabar event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception()))
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat(
                (events.last() as ShowErrorMessage).message
            ).isEqualTo(R.string.receipt_fetching_error)
        }

    @Test
    fun `given built reader and receipt fetching fails, when user clicks on send receipt button, then ShowSnackabar event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception()))

            createController(BUILT_IN)
            controller.start()

            val events = controller.event.runAndCaptureValues {
                val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
                state.onSendReceiptClicked()
            }

            assertThat(
                (events.last() as ShowErrorMessage).message
            ).isEqualTo(R.string.receipt_fetching_error)
        }

    @Test
    fun `given external reader, when user clicks on send receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
            state.onSendReceiptClicked()

            verify(tracker).trackEmailReceiptTapped()
        }

    @Test
    fun `given built in reader, when user clicks on send receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(BUILT_IN)

            controller.start()

            val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
            state.onSendReceiptClicked()

            verify(tracker).trackEmailReceiptTapped()
        }

    @Test
    fun `given billing email empty and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful
                state.onSaveUserClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given billing email built in and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(BUILT_IN)
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful
                state.onSaveUserClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
                state.onSaveUserClicked()
            }
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
                state.onSaveUserClicked()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given payment flow is loading, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(LoadingDataState(mock())) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackPaymentCancelled("Loading")
        }

    @Test
    fun `given payment flow is collecting state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackPaymentCancelled("Collecting")
        }

    @Test
    fun `given payment flow is processing state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackPaymentCancelled("Processing")
        }

    @Test
    fun `given payment flow is capturing state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackPaymentCancelled("Capturing")
        }

    @Test
    fun `given payment flow is payment failed, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is success state, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is initializing payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }
            controller.start()

            (controller.paymentState.value as CardReaderPaymentState.LoadingData).onCancel()

            verify(tracker).trackPaymentCancelled("Loading")
        }

    @Test
    fun `given payment flow is initializing payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }

            val events = controller.event.runAndCaptureValues {
                controller.start()
                (controller.paymentState.value as CardReaderPaymentState.LoadingData).onCancel()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given payment flow is collection payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            controller.start()

            val state = controller.paymentState.value as
                CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState
            state.onCancel()

            verify(tracker).trackPaymentCancelled("Collecting")
        }

    @Test
    fun `given payment flow is collection payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState
                state.onCancel()
            }
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given payment flow is processing payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            controller.start()

            val state = controller.paymentState.value as
                CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment
            state.onCancel()

            verify(tracker).trackPaymentCancelled("Processing")
        }

    @Test
    fun `given payment flow is processing payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment
                state.onCancel()
            }
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given payment flow is receipt print state and external, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            (controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful).onPrintReceiptClicked()
            controller.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is receipt print state and built in, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            controller.start()

            (controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful).onPrintReceiptClicked()
            controller.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is refetching order, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()
            simulateFetchOrderJobState(inProgress = true)

            controller.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given user presses back button, when re-fetching order, then ReFetchingOrderState shown`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)

            controller.onBackPressed()

            assertThat(controller.paymentState.value).isInstanceOf(CardReaderPaymentState.ReFetchingOrder::class.java)
        }

    @Test
    fun `given re-fetching order and external, when user clicks on save for later button, then ReFetchingOrderState shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()
            simulateFetchOrderJobState(inProgress = true)

            (controller.paymentState.value as PaymentSuccessful.ExternalReaderPaymentSuccessful).onSaveUserClicked()

            assertThat(controller.paymentState.value).isInstanceOf(CardReaderPaymentState.ReFetchingOrder::class.java)
        }

    @Test
    fun `given re-fetching order and built in, when user clicks on save for later button, then ReFetchingOrderState shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            createController(cardReaderType = BUILT_IN)

            controller.start()
            simulateFetchOrderJobState(inProgress = true)

            (controller.paymentState.value as PaymentSuccessful.BuiltInReaderPaymentSuccessful).onSaveUserClicked()

            assertThat(controller.paymentState.value).isInstanceOf(CardReaderPaymentState.ReFetchingOrder::class.java)
        }

    @Test
    fun `given user presses back, when already in ReFetchingOrderState, then snackbar shown and screen dismissed`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = true)
                controller.onBackPressed() // shows ReFetchingOrderState screen
                controller.onBackPressed()
            }

            assertThat(events[0]).isInstanceOf(ShowErrorMessage::class.java)
            assertThat(events[1]).isEqualTo(CardReaderPaymentEvent.Exit)
        }

    @Test
    fun `given user presses back, when already showing ReFetchingOrderState, then correct snackbar message shown`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = true)
                controller.onBackPressed() // shows ReFetchingOrderState screen
                controller.onBackPressed()
            }

            assertThat((events[0] as ShowErrorMessage).message)
                .isEqualTo(R.string.card_reader_refetching_order_failed)
        }

    @Test
    fun `given user presses back button, when re-fetching order, then screen not dismissed`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = true)
                controller.onBackPressed()
            }
            assertThat(events).isEmpty()
        }

    @Test
    fun `given user presses back button, when not re-fetching order, then screen dismissed`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = false)
                controller.onBackPressed()
            }

            assertThat(events.last()).isEqualTo(CardReaderPaymentEvent.Exit)
        }

    @Test
    fun `given ReFetchingOrderState shown, when re-fetching order completes, then screen auto-dismissed`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = true)
                controller.onBackPressed() // show ReFetchingOrderState screen
                controller.onBackPressed()
            }
            advanceUntilIdle()

            assertThat(events.last()).isEqualTo(CardReaderPaymentEvent.Exit)
        }

    @Test
    fun `given built in payment failed state and connected BI, when user presses back, then disconnect from reader invoked`() =
        testBlocking {
            val cardReader: CardReader = mock {
                on { type }.thenReturn("COTS_DEVICE")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, true))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            createController(cardReaderType = BUILT_IN)
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `given payment failed state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            val cardReader: CardReader = mock {
                on { type }.thenReturn("STRIPE_M2")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given payment processing state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            val cardReader: CardReader = mock {
                on { type }.thenReturn("STRIPE_M2")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given ReFetchingOrderState not shown, when re-fetching order completes, then screen not auto-dismissed`() =
        testBlocking {
            val events = controller.event.runAndCaptureValues {
                simulateFetchOrderJobState(inProgress = true)
                controller.reFetchOrder()
            }
            assertThat(events).isEmpty()
        }

    @Test
    fun `when re-fetching order fails, then SnackBar shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(any())).thenReturn(null)

            val events = controller.event.runAndCaptureValues {
                controller.reFetchOrder()
            }
            assertThat(events[0]).isInstanceOf(ShowErrorMessage::class.java)
        }

    @Test
    fun `given user leaves the screen, when payment fails, then payment canceled`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            controller.start()

            controller.stop()

            verify(cardReaderManager).cancelPayment(any())
        }

    @Test
    fun `given user leaves the screen, when payment succeeded on retry, then payment NOT canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any()))
                .thenAnswer {
                    flow {
                        emit(paymentFailedWithValidDataForRetry)
                        emit(PaymentCompleted(""))
                    }
                }
            controller.start()

            controller.stop()

            verify(cardReaderManager, never()).cancelPayment(any())
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then make sure NOT to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            // when
            controller.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then make sure NOT to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

            // When
            controller.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is connected, when payment screen is shown, then proceed to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then show error Snackbar`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

            val events = controller.event.runAndCaptureValues {
                // When
                controller.start()
            }

            // Then
            assertThat(events[0]).isInstanceOf(ShowErrorMessage::class.java)
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then Snackbar is shown with message`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            val events = controller.event.runAndCaptureValues {
                // When
                controller.start()
            }

            // Then
            assertThat((events[0] as ShowErrorMessage).message)
                .isEqualTo(R.string.card_reader_payment_reader_not_connected)
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then exit event is triggered`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

            // When
            val events = controller.event.runAndCaptureValues {
                controller.start()
                advanceUntilIdle()
            }

            // Then
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then show error Snackbar`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
            val events = controller.event.runAndCaptureValues {
                // When
                controller.start()
            }

            // Then
            assertThat(events[0]).isInstanceOf(ShowErrorMessage::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then Snackbar is shown with the message`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            val events = controller.event.runAndCaptureValues {
                // When
                controller.start()
            }

            // Then
            assertThat((events[0] as ShowErrorMessage).message)
                .isEqualTo(R.string.card_reader_payment_reader_not_connected)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then exit event is triggered`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            val events = controller.event.runAndCaptureValues {
                // When
                controller.start()
            }

            // Then
            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `when flow started, then correct order key is propagated to CardReaderManager`() =
        testBlocking {
            // Given
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.orderKey).isEqualTo("wc_order_j0LMK3bFhalEL")
        }

    @Test
    fun `given plugin can not be send, when flow started, then wc pay can send receipt is false`() =
        testBlocking {
            // Given
            whenever(paymentReceiptHelper.isPluginCanSendReceipt(siteModel)).thenReturn(false)
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.isPluginCanSendReceipt).isFalse()
        }

    @Test
    fun `given plugin can be send, when flow started, then wc pay can send receipt is true`() =
        testBlocking {
            // Given
            whenever(paymentReceiptHelper.isPluginCanSendReceipt(siteModel)).thenReturn(true)
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.isPluginCanSendReceipt).isTrue()
        }

    @Test
    fun `given canada and total 0,58, when flow started, then fee set to 15`() =
        testBlocking {
            // Given
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("CA")
            whenever(mockedOrder.total).thenReturn(BigDecimal(0.58))
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.feeAmount).isEqualTo(15)
        }

    @Test
    fun `given canada and total 135,6, when flow started, then fee set to 15`() =
        testBlocking {
            // Given
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("CA")
            whenever(mockedOrder.total).thenReturn(BigDecimal(145.6))
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.feeAmount).isEqualTo(15)
        }

    @Test
    fun `given us and total 1,49, when flow started, then fee is not set`() =
        testBlocking {
            // Given
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
            whenever(mockedOrder.total).thenReturn(BigDecimal(1.49))
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
            val captor = argumentCaptor<PaymentInfo>()

            // When
            controller.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.feeAmount).isNull()
        }

    @Test
    fun `given collect payment NOT shown, when show additional info event received, then event ignored`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow {
                    emit(ProcessingPayment)
                }
            }
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(CHECK_MOBILE_DEVICE))
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(CARD_REMOVED_TOO_EARLY))
                }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
            }

            assertThat((events)).isEmpty()
            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment::class.java)
        }

    // region - Interac refund
    @Test
    fun `given interac refund shown, when RETRY message received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_retry_card_prompt)
        }

    @Test
    fun `given Unknown refund error, when view model starts, then ui has contact support button`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Unknown)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }

            controller.start()

            val externalReaderFailedPaymentState = controller.paymentState.value as
                CardReaderInteracRefundState.InteracRefundFailure
            assertThat(externalReaderFailedPaymentState.cta!!.label).isEqualTo(R.string.support_contact)
            assertNotNull(externalReaderFailedPaymentState.onCancel)
        }

    @Test
    fun `given unknown error, when contact support clicked, then contact support event emited`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Unknown)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val externalReaderFailedPaymentState = controller.paymentState.value as
                    CardReaderInteracRefundState.InteracRefundFailure
                externalReaderFailedPaymentState.cta!!.onCallToActionTapped()
            }

            assertThat(events.last()).isEqualTo(CardReaderPaymentEvent.ContactSupportTapped)
        }

    @Test
    fun `given interac refund shown, when INSERT_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when INSERT_OR_SWIPE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when SWIPE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when REMOVE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_remove_card_prompt)
        }

    @Test
    fun `given interac refund, when MULTIPLE_CONTACTLESS_CARDS_DETECTED received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_multiple_contactless_cards_detected_prompt)
        }

    @Test
    fun `given interac refund shown, when TRY_ANOTHER_READ_METHOD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_try_another_read_method_prompt)
        }

    @Test
    fun `given interac refund, when TRY_ANOTHER_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_try_another_card_prompt)
        }

    @Test
    fun `given interac refund, when CHECK_MOBILE_DEVICE received, then refund payment hint updated`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(CHECK_MOBILE_DEVICE))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).cardReaderHint
            )
                .isEqualTo(R.string.card_reader_payment_check_mobile_device_prompt)
        }

    @Test
    fun `given interac refund, when payment screen shown, then loading data state is shown`() {
        setupControllerForInteracRefund()

        controller.start()

        assertThat(controller.paymentState.value).isInstanceOf(CardReaderInteracRefundState.LoadingData::class.java)
    }

    @Test
    fun `when initializing interac refund, then ui updated to initializing refund state `() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }

            controller.start()

            assertThat(controller.paymentState.value).isInstanceOf(CardReaderInteracRefundState.LoadingData::class.java)
        }

    @Test
    fun `given fetch order failed, when initializing interac refund, then ui updated to proper error state `() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.InteracRefundFailure::class.java)
        }

    @Test
    fun `when collecting interac refund, then ui updated to collecting refund state`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.CollectingInteracRefund::class.java)
        }

    @Test
    fun `when processing interac refund, then ui updated to processing refund state`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.ProcessingInteracRefund::class.java)
        }

    @Test
    fun `when interac refund completed, then ui updated to refund successful state`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InteracRefundSuccess) }
            }

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.InteracRefundSuccessful::class.java)
        }

    @Test
    fun `when interac refund fails, then ui updated to refund failed state`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.InteracRefundFailure::class.java)
        }

    @Test
    fun `when interac refund fails, then invalidate onboarding cache`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }

            controller.start()

            verify(cardReaderOnboardingChecker).invalidateCache()
        }

    @Test
    fun `given chargeId is null, when interac refund initiated, then proper state is shown`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            controller.start()

            assertThat(
                controller.paymentState.value
            ).isInstanceOf(CardReaderInteracRefundState.InteracRefundFailure::class.java)
        }

    @Test
    fun `given non retryable error, when interac refund initiated, then primary action is back press`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)
            val events = controller.event.runAndCaptureValues {
                controller.start()
                val viewState = controller.paymentState.value
                (viewState as CardReaderInteracRefundState.InteracRefundFailure.Cancelable).onCancel()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given refund flow is initializing state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            controller.start()

            (controller.paymentState.value as CardReaderInteracRefundState.LoadingData).onCancel()

            verify(tracker).trackInteracRefundCancelled("Loading")
        }

    @Test
    fun `given refund flow is initializing state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                (controller.paymentState.value as CardReaderInteracRefundState.LoadingData).onCancel()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `given refund flow is collection state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            controller.start()

            (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).onCancel()

            verify(tracker).trackInteracRefundCancelled("Collecting")
        }

    @Test
    fun `given refund flow is collection state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            val events = controller.event.runAndCaptureValues {
                controller.start()
                (controller.paymentState.value as CardReaderInteracRefundState.CollectingInteracRefund).onCancel()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }

    @Test
    fun `when interac refund fails, then interac refund failed event is triggered`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }

            controller.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `when interac refund fails, then interac refund failed event is triggered with correct data`() =
        testBlocking {
            setupControllerForInteracRefund()
            val expectedOrderId = ORDER_ID
            val expectedErrorMessage = "Error Message"
            val expectedErrorType = CardInteracRefundStatus.RefundStatusErrorType.Cancelled
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            expectedErrorType,
                            expectedErrorMessage,
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "USD"
                            )
                        )
                    )
                }
            }
            val captor = argumentCaptor<Long, String, CardInteracRefundStatus.RefundStatusErrorType>()

            controller.start()

            verify(tracker).trackInteracPaymentFailed(
                captor.first.capture(),
                captor.second.capture(),
                captor.third.capture(),
            )
            assertThat(captor.first.firstValue).isEqualTo(expectedOrderId)
            assertThat(captor.second.firstValue).isEqualTo(expectedErrorMessage)
            assertThat(captor.third.firstValue).isEqualTo(expectedErrorType)
        }

    @Test
    fun `given failed to fetch order, when interac refund fails, then interac refund failed event is triggered`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            controller.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `given failed to fetch order, when interac refund fails, then event is triggered with correct data`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)
            val captor = argumentCaptor<String>()
            val expectedErrorMessage = "Fetching order failed"

            controller.start()

            verify(tracker).trackInteracPaymentFailed(any(), captor.capture(), any())
            assertThat(captor.firstValue).isEqualTo(expectedErrorMessage)
        }

    @Test
    fun `given null chargeid on order, when interac refund fails, then interac refund failed event is triggered`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            controller.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `given null chargeid on order, when interac refund fails, then event is triggered with correct data`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)
            val expectedOrderId = ORDER_ID
            val expectedErrorMessage = "Charge id is null for the order."
            val expectedErrorType = CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
            val captor = argumentCaptor<Long, String, CardInteracRefundStatus.RefundStatusErrorType>()

            controller.start()

            verify(tracker).trackInteracPaymentFailed(
                captor.first.capture(),
                captor.second.capture(),
                captor.third.capture(),
            )
            assertThat(captor.first.firstValue).isEqualTo(expectedOrderId)
            assertThat(captor.second.firstValue).isEqualTo(expectedErrorMessage)
            assertThat(captor.third.firstValue).isEqualTo(expectedErrorType)
        }

    @Test
    fun `given interac refund flow already started, when start() is invoked, then flow is not restarted`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow<CardInteracRefundStatus> {}
            }

            controller.start()
            controller.start()
            controller.start()
            controller.start()

            verify(cardReaderManager, times(1))
                .refundInteracPayment(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given user clicks on retry, when interac refund fails, then refundInteracPayment invoked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "CAD"
                            )
                        )
                    )
                }
            }
            controller.start()

            (controller.paymentState.value as CardReaderInteracRefundState.InteracRefundFailure).onRetry!!()
            advanceUntilIdle()

            // Times 2 because, refundInteracPayment() method gets called when refund is initiated
            // as well as when the refund is retried.
            verify(cardReaderManager, times(2)).refundInteracPayment(any(), any())
        }

    @Test
    fun `given refund flow is loading, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Loading")
        }

    @Test
    fun `given refund flow is collecting, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Collecting")
        }

    @Test
    fun `given refund flow is processing, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }
            controller.start()

            controller.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Processing")
        }

    @Test
    fun `given refund failed state and connected BI, when user presses back, then disconnect from a reader invoked`() =
        testBlocking {
            setupControllerForInteracRefund()
            val cardReader: CardReader = mock {
                on { type }.thenReturn("COTS_DEVICE")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "CAD"
                            )
                        )
                    )
                }
            }
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `given refund failed state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupControllerForInteracRefund()
            val cardReader: CardReader = mock {
                on { type }.thenReturn("WISEPAD_3")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "CAD"
                            )
                        )
                    )
                }
            }
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund failed state and not connected, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.NotConnected())
            )

            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund success state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupControllerForInteracRefund()
            val cardReader: CardReader = mock {
                on { type }.thenReturn("WISEPAD_3")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InteracRefundSuccess) }
            }
            controller.start()

            controller.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund failed state, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            setupControllerForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.Generic
                )
            ).thenReturn(InteracRefundFlowError.Generic)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            CardInteracRefundStatus.RefundStatusErrorType.Generic,
                            "",
                            RefundParams(
                                amount = BigDecimal.TEN,
                                chargeId = "",
                                currency = "CAD"
                            )
                        )
                    )
                }
            }

            val events = controller.event.runAndCaptureValues {
                controller.start()
                val state = controller.paymentState.value as
                    CardReaderInteracRefundState.InteracRefundFailure.Cancelable
                state.onCancel()
            }

            assertThat(events.last()).isInstanceOf(CardReaderPaymentEvent.Exit::class.java)
        }
    // endregion - Interac refund

    @Test
    fun `when new battery status event is received, then tracking is updated with new battery level`() =
        testBlocking {
            val batteryLevel1 = .5F
            val batteryLevel2 = .45F
            whenever(cardReaderManager.batteryStatus).thenAnswer {
                flow {
                    emit(CardReaderBatteryStatus.StatusChanged(batteryLevel1, BatteryStatus.NOMINAL, false))
                    emit(CardReaderBatteryStatus.StatusChanged(batteryLevel2, BatteryStatus.NOMINAL, false))
                }
            }

            controller.start()

            val inOrder = inOrder(cardReaderTrackingInfoKeeper)
            inOrder.verify(cardReaderTrackingInfoKeeper).setCardReaderBatteryLevel(batteryLevel1)
            inOrder.verify(cardReaderTrackingInfoKeeper).setCardReaderBatteryLevel(batteryLevel2)
        }

    @Test
    fun `when new battery status event is received, then tracking is not updated if the battery level didn't change`() =
        testBlocking {
            whenever(cardReaderManager.batteryStatus).thenAnswer {
                flow {
                    emit(CardReaderBatteryStatus.Unknown)
                    emit(CardReaderBatteryStatus.Warning)
                }
            }

            controller.start()

            verify(cardReaderTrackingInfoKeeper, never()).setCardReaderBatteryLevel(anyFloat())
        }

    @Test
    fun `given ttp not in progress and reader connected, when vm starts, then AppKilledWhileInBackground state not emitted`() =
        testBlocking {
            val cardReader: CardReader = mock()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            isTTPinProgress = false
            createController(BUILT_IN)

            controller.start()

            verify(tracker, never()).trackPaymentFailed("VM killed when TTP activity in foreground")
            assertThat(controller.paymentState.value).isNotInstanceOf(BuiltInReaderFailedPaymentState::class.java)
        }

    @Test
    fun `given AppKilledWhileInBackground, when vm starts, then payment collection doesnt start`() =
        testBlocking {
            val cardReader: CardReader = mock()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            isTTPinProgress = true
            createController(cardReaderType = BUILT_IN)

            controller.start()

            verify(cardReaderManager, never()).collectPayment(any())
        }

    private suspend fun simulateFetchOrderJobState(inProgress: Boolean) {
        if (inProgress) {
            whenever(orderRepository.fetchOrderById(any())).doSuspendableAnswer {
                delay(1000)
                mock()
            }
        } else {
            whenever(orderRepository.fetchOrderById(any())).doReturn(mock())
        }
        controller.reFetchOrder()
    }

    private fun setupControllerForInteracRefund() {
        val param = PaymentOrRefund.Refund(ORDER_ID, refundAmount = BigDecimal(10.72))
        controller = CardReaderPaymentController(
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            selectedSite = selectedSite,
            appPrefs = appPrefs,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            interacRefundableChecker = interacRefundableChecker,
            tracker = tracker,
            trackCancelledFlow = trackCanceledFlow,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            interacRefundErrorMapper = interacRefundErrorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            paymentStateProvider = paymentStateProvider,
            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
            paymentReceiptHelper = paymentReceiptHelper,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            paymentReceiptShare = paymentReceiptShare,
            paymentOrRefund = param,
            cardReaderType = CardReaderType.EXTERNAL,
            isTTPPaymentInProgress = isTTPinProgressProp,
        )
    }

    companion object {
        private const val ORDER_ID = 1L
        private val siteModel = SiteModel().apply { name = "testName" }.apply { url = "testUrl.com" }
        private val DUMMY_TOTAL = BigDecimal(10.72)
        private const val DUMMY_CURRENCY_SYMBOL = "£"
    }
}
