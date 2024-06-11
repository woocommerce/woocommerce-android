package com.woocommerce.android.ui.payments.cardreader

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BatteryStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.DeclinedByBackendError.CardDeclined
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
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.EXTERNAL
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundableChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentDialogFragmentArgs
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentReaderTypeStateProvider
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.ContactSupport
import com.woocommerce.android.ui.payments.cardreader.payment.EnableNfc
import com.woocommerce.android.ui.payments.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError.AmountTooSmall
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError.Unknown
import com.woocommerce.android.ui.payments.cardreader.payment.PlayChaChing
import com.woocommerce.android.ui.payments.cardreader.payment.PrintReceipt
import com.woocommerce.android.ui.payments.cardreader.payment.PurchaseCardReader
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderProcessingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.CollectRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderProcessingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.FailedRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.LoadingDataState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.PrintingReceiptState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ProcessingRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ReFetchingOrderState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.RefundLoadingDataState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.RefundSuccessfulState
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertEquals

private val DUMMY_TOTAL = BigDecimal(10.72)
private const val DUMMY_CURRENCY_SYMBOL = "£"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class CardReaderPaymentViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private val siteModel = SiteModel().apply { name = "testName" }.apply { url = "testUrl.com" }
    }

    private lateinit var viewModel: CardReaderPaymentViewModel
    private val cardReaderManager: CardReaderManager = mock()
    private val orderRepository: OrderDetailRepository = mock()
    private val mockedOrder = mock<Order>()
    private val mockedAddress = mock<Address>()
    private val selectedSite: SelectedSite = mock()
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val tracker: PaymentsFlowTracker = mock()
    private val appPrefs: AppPrefs = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooStore: WooCommerceStore = mock()

    private val paymentFailedWithEmptyDataForRetry = PaymentFailed(Generic, null, "dummy msg")
    private val paymentFailedWithValidDataForRetry = PaymentFailed(Generic, mock(), "dummy msg")
    private val paymentFailedWithNoNetwork = PaymentFailed(NoNetwork, mock(), "dummy msg")
    private val paymentFailedWithPaymentDeclined = PaymentFailed(DeclinedByBackendError.Unknown, mock(), "dummy msg")
    private val paymentFailedWithCardReadTimeOut = PaymentFailed(Generic, mock(), "dummy msg")
    private val paymentFailedWithServerError = PaymentFailed(Server(""), mock(), "dummy msg")
    private val paymentFailedWithAmountTooSmall = PaymentFailed(
        DeclinedByBackendError.AmountTooSmall,
        mock(),
        "dummy msg"
    )

    private val savedState: SavedStateHandle = CardReaderPaymentDialogFragmentArgs(
        CardReaderFlowParam.PaymentOrRefund.Payment(ORDER_ID, ORDER),
        EXTERNAL,
    ).toSavedStateHandle()

    private val interacRefundSavedState: SavedStateHandle = CardReaderPaymentDialogFragmentArgs(
        CardReaderFlowParam.PaymentOrRefund.Refund(ORDER_ID, refundAmount = BigDecimal(10.72)),
        EXTERNAL,
    ).toSavedStateHandle()

    private val errorMapper: CardReaderPaymentErrorMapper = mock()
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper = mock()
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper = mock()
    private val interacRefundableChecker: CardReaderInteracRefundableChecker = mock()
    private val cardReaderPaymentReaderTypeStateProvider = CardReaderPaymentReaderTypeStateProvider()
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper = mock()
    private val paymentReceiptHelper: PaymentReceiptHelper = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val cardReaderConfigProvider: CardReaderCountryConfigProvider = mock()
    private val cardReaderConfig: CardReaderConfigForSupportedCountry = CardReaderConfigForUSA
    private val paymentReceiptShare: PaymentReceiptShare = mock()

    @Suppress("LongMethod")
    @Before
    fun setUp() = testBlocking {
        whenever(cardReaderConfigProvider.provideCountryConfigFor("US"))
            .thenReturn(CardReaderConfigForUSA)

        viewModel = CardReaderPaymentViewModel(
            savedState,
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            selectedSite = selectedSite,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            interacRefundableChecker = interacRefundableChecker,
            tracker = tracker,
            appPrefs = appPrefs,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            interacRefundErrorMapper = interacRefundErrorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            cardReaderPaymentReaderTypeStateProvider = cardReaderPaymentReaderTypeStateProvider,
            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
            paymentReceiptHelper = paymentReceiptHelper,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            cardReaderConfigProvider = cardReaderConfigProvider,
            paymentReceiptShare = paymentReceiptShare,
        )

        whenever(orderRepository.getOrderById(any())).thenReturn(mockedOrder)
        whenever(mockedOrder.total).thenReturn(DUMMY_TOTAL)
        whenever(mockedOrder.currency).thenReturn("GBP")
        whenever(currencyFormatter.formatAmountWithCurrency(DUMMY_TOTAL.toDouble(), "GBP"))
            .thenReturn("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
        whenever(mockedOrder.billingAddress).thenReturn(mockedAddress)
        whenever(mockedAddress.email).thenReturn("")
        whenever(mockedAddress.firstName).thenReturn("Tester")
        whenever(mockedAddress.lastName).thenReturn("Test")
        whenever(mockedOrder.orderKey).thenReturn("wc_order_j0LMK3bFhalEL")
        whenever(mockedOrder.id).thenReturn(ORDER_ID)
        whenever(mockedOrder.chargeId).thenReturn("chargeId")
        whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderManager.collectPayment(any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.retryCollectPayment(any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
        whenever(interacRefundableChecker.isRefundable(any())).thenReturn(true)
        whenever(appPrefs.getCardReaderStatementDescriptor(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn("test statement descriptor")
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
        whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
            flow<BluetoothCardReaderMessages> {}
        }
        whenever(paymentReceiptHelper.isPluginCanSendReceipt(siteModel)).thenReturn(true)
        whenever(paymentReceiptHelper.getReceiptUrl(ORDER_ID)).thenReturn(Result.success("test url"))
        whenever(cardReaderPaymentOrderHelper.getPaymentDescription(mockedOrder)).thenReturn("test description")
        whenever(cardReaderPaymentOrderHelper.getAmountLabel(mockedOrder))
            .thenReturn("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
        whenever(cardReaderPaymentOrderHelper.getReceiptDocumentName(mockedOrder.id)).thenReturn("receipt-order-1")
        whenever(cardReaderManager.batteryStatus).thenAnswer { flow { emit(CardReaderBatteryStatus.Unknown) } }
    }

    //region - Payments tests

    @Test
    fun `given collect payment shown, when RETRY message received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it's run after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_retry_card_prompt)
        }

    @Test
    fun `given collect payment shown, when INSERT_CARD received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when INSERT_OR_SWIPE_CARD received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when SWIPE_CARD received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when REMOVE_CARD received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it's run after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_remove_card_prompt)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_CARD message received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it's run after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_card_prompt)
        }

    @Test
    fun `given collect payment shown, when CARD_REMOVED_TOO_EARLY message received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it runs after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(CARD_REMOVED_TOO_EARLY))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_card_removed_too_early)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_READ message received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it's run after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_read_method_prompt)
        }

    @Test
    fun `given collect payment shown, when MULTIPLE_CARDS_DETECTED received, then collect payment hint updated`() =
        testBlocking {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    delay(1) // make sure it's run after collecting payment starts
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            advanceUntilIdle()

            assertThat((viewModel.viewStateData.value as ExternalReaderCollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_multiple_contactless_cards_detected_prompt)
        }

    @Test
    fun `given fetching order fails, when payment screen shown, then ExternalReaderFailedPaymentState state is shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderFailedPaymentState::class.java)
        }

    @Test
    fun `given fetching order fails and tpp, when payment screen shown, then BuiltInReaderFailedPaymentState state is shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderFailedPaymentState::class.java)
        }

    @Test
    fun `when fetching order fails, then event tracked`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            verify(tracker).trackPaymentFailed(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given fetching order fails, when external payment screen shown, then correct error message shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            assertThat((viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel)
                .isEqualTo(UiStringRes(R.string.order_error_fetch_generic))
        }

    @Test
    fun `given fetching order fails, when built in payment screen shown, then correct error message shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat((viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel)
                .isEqualTo(UiStringRes(R.string.order_error_fetch_generic))
        }

    @Test
    fun `given fetching order succeeds, when payment screen shown, then order currency stored `() =
        testBlocking {
            viewModel.start()

            verify(cardReaderTrackingInfoKeeper).setCurrency(("GBP"))
        }

    @Test
    fun `when payment screen shown, then loading data state is shown`() {
        viewModel.start()

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
    }

    @Test
    fun `when payment not collectable, then flow terminated and snackbar shown`() =
        testBlocking {
            whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            viewModel.start()

            assertThat(
                (events[0] as ShowSnackbar).message
            ).isEqualTo(
                R.string.card_reader_payment_order_paid_payment_cancelled
            )
            assertThat(events[1]).isInstanceOf(Exit::class.java)
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

            viewModel.start()

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

            viewModel.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.statementDescriptor.value).isEqualTo(expectedResult)
        }

    @Test
    fun `when initializing payment, then ui updated to initializing payment state `() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
        }

    @Test
    fun `when collecting payment, then ui updated to collecting payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderCollectPaymentState::class.java)
        }

    @Test
    fun `given built in reader,when collecting payment, then ui updated to collecting payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderCollectPaymentState::class.java)
        }

    @Test
    fun `when processing payment, then ui updated to processing payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderProcessingPaymentState::class.java)
        }

    @Test
    fun `given built in reader, when processing payment, then ui updated to processing payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderProcessingPaymentState::class.java)
        }

    @Test
    fun `when processing payment completed with card present, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.CARD_PRESENT)) }
            }

            viewModel.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("card")
        }

    @Test
    fun `when processing payment completed with interac present, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.INTERAC_PRESENT)) }
            }

            viewModel.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("card_interac")
        }

    @Test
    fun `when processing payment completed with interac present, then track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.INTERAC_PRESENT)) }
            }

            viewModel.start()

            verify(tracker).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with card present, then do not track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.CARD_PRESENT)) }
            }

            viewModel.start()

            verify(tracker, never()).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with unknown type, then do not track interac success`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.UNKNOWN)) }
            }

            viewModel.start()

            verify(tracker, never()).trackInteracPaymentSucceeded()
        }

    @Test
    fun `when processing payment completed with unknown, then tracking keeper stores payment type`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPaymentCompleted(PaymentMethodType.UNKNOWN)) }
            }

            viewModel.start()

            verify(cardReaderTrackingInfoKeeper).setPaymentMethodType("unknown")
        }

    @Test
    fun `when capturing payment, then ui updated to capturing payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderCapturingPaymentState::class.java)
        }

    @Test
    fun `given built in reader, when capturing payment, then ui updated to capturing payment state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderCapturingPaymentState::class.java)
        }

    @Test
    fun `given billing email empty, when external payment completed, then ui updated to external payment successful state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderPaymentSuccessfulState::class.java)
        }

    @Test
    fun `given billing email empty, when built in payment completed, then ui updated to built in payment successful state`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderPaymentSuccessfulState::class.java)
        }

    @Test
    fun `given billing not empty, when external payment completed, then ui updated to external payment successful receipt sent state`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java
            )
        }

    @Test
    fun `given billing not empty, when built in payment completed, then ui updated to built in payment successful receipt sent state`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(
                BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java
            )
        }

    @Test
    fun `when payment completed, then cha-ching sound is played`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            assertThat(events[0]).isInstanceOf(PlayChaChing::class.java)
        }

    @Test
    fun `when payment completed, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            verify(tracker).trackPaymentSucceeded()
        }

    @Test
    fun `given external reader, when payment fails, then ui updated to external failed state`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderFailedPaymentState::class.java)
        }

    @Test
    fun `given external reader fails with Unknown error, when view model starts, then ui has contact support button`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            val externalReaderFailedPaymentState = viewModel.viewStateData.value as ExternalReaderFailedPaymentState
            assertThat(externalReaderFailedPaymentState.primaryActionLabel).isEqualTo(R.string.support_contact)
            assertThat(externalReaderFailedPaymentState.secondaryActionLabel).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given built in reader fails with Unknown error, when view model starts, then ui has contact support button`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            val state = viewModel.viewStateData.value as BuiltInReaderFailedPaymentState
            assertThat(state.primaryActionLabel).isEqualTo(R.string.support_contact)
            assertThat(state.secondaryActionLabel).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given external reader fails with generic error, when contact support clicked, then contact support emitted and flow canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            val events = viewModel.event.captureValues()

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(events[0]).isInstanceOf(Exit::class.java)
            assertThat(events[1]).isInstanceOf(ContactSupport::class.java)
        }

    @Test
    fun `when contact support clicked, then contact support event tracked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            verify(tracker).trackPaymentFailedContactSupportTapped()
        }

    @Test
    fun `given built in reader fails with generic error, when contact support clicked, then contact support emitted and flow canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Declined.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            val events = viewModel.event.captureValues()

            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(events[0]).isInstanceOf(Exit::class.java)
            assertThat(events[1]).isInstanceOf(ContactSupport::class.java)
        }

    @Test
    fun `given built in reader, when payment fails, then ui updated to built in failed state`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderFailedPaymentState::class.java)
        }

    @Test
    fun `when payment fails, then invalidate onboarding cache`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            verify(cardReaderOnboardingChecker).invalidateCache()
        }

    @Test
    fun `given external reader, when payment fails because of NoNetwork, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of NoNetwork, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of Unknown, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown, cardReaderConfig, false))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of Unknown, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown, cardReaderConfig, true))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of CARD_READ_TIMEOUT, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of CARD_READ_TIMEOUT, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `when payment fails, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            verify(tracker).trackPaymentFailed(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given external reader, when payment fails because of NO_NETWORK, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of NO_NETWORK, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of declined Unknown, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown, cardReaderConfig, false))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of declined Unknown, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown, cardReaderConfig, true))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of CARD_READ_TIME_OUT, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of CARD_READ_TIME_OUT, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of GENERIC_ERROR, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of GENERIC_ERROR, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of SERVER_ERROR, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), cardReaderConfig, false)).thenReturn(
                PaymentFlowError.Server
            )
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Server.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of SERVER_ERROR, then error is mapped correctly`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                PaymentFlowError.Server.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of AMOUNT_TOO_SMALL, then error is mapped correctly`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    false
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).paymentStateLabel,
                error.message
            )
        }

    @Test
    fun `given built in reader, when payment fails because of AMOUNT_TOO_SMALL, then error is mapped correctly`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    true
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).paymentStateLabel,
                error.message
            )
        }

    @Test
    fun `given external reader, when payment fails because of AMOUNT_TOO_SMALL, then failed state has ok button`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    false
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).primaryActionLabel,
                R.string.card_reader_payment_payment_failed_ok
            )
        }

    @Test
    fun `given built in reader, when payment fails because of AMOUNT_TOO_SMALL, then failed state has ok button`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    true
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).primaryActionLabel,
                R.string.card_reader_payment_payment_failed_ok
            )
        }

    @Test
    fun `given external reader, when payment fails not because of AMOUNT_TOO_SMALL, then failed state has Try again button`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).primaryActionLabel,
                R.string.try_again
            )
        }

    @Test
    fun `given built in reader, when payment fails not because of AMOUNT_TOO_SMALL, then failed state has Try again button`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).primaryActionLabel,
                R.string.try_again
            )
        }

    @Test
    fun `given built in reader, when payment fails PIN_REQUIRED, then failed state has purchase card reader`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.CardDeclined.PinRequired,
                    cardReaderConfig,
                    true
                )
            ).thenReturn(PaymentFlowError.BuiltInReader.PinRequired)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.CardDeclined.PinRequired, mock(), "dummy msg")) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).primaryActionLabel,
                R.string.card_reader_payment_payment_failed_purchase_hardware_reader
            )
        }

    @Test
    fun `given built in reader, when purchase button clicked, then purchase even emmited`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.CardDeclined.PinRequired,
                    cardReaderConfig,
                    true
                )
            ).thenReturn(PaymentFlowError.BuiltInReader.PinRequired)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.CardDeclined.PinRequired, mock(), "dummy msg")) }
            }
            whenever(wooStore.getStoreCountryCode(siteModel)).thenReturn("US")
            initViewModel(BUILT_IN)

            viewModel.start()
            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PurchaseCardReader::class.java)
            assertThat((viewModel.event.value as PurchaseCardReader).url).isEqualTo(
                "https://woocommerce.com/products/hardware/US"
            )
        }

    @Test
    fun `given external reader, when payment fails because of AMOUNT_TOO_SMALL, then clicking on ok button triggers exit event`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    false
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()
            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given built in reader, when payment fails because of AMOUNT_TOO_SMALL, then clicking on ok button triggers exit event`() =
        testBlocking {
            val error = AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    DeclinedByBackendError.AmountTooSmall,
                    cardReaderConfig,
                    true
                )
            ).thenReturn(error)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()
            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given built in reader, when payment fails because of NFC_DISABLED, then error is mapped correcty`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled,
                    cardReaderConfig,
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
            initViewModel(BUILT_IN)

            viewModel.start()

            val state = viewModel.viewStateData.value as BuiltInReaderFailedPaymentState
            assertThat(state.paymentStateLabel).isEqualTo(
                UiStringRes(R.string.card_reader_payment_failed_nfc_disabled)
            )
            assertThat(state.primaryActionLabel).isEqualTo(R.string.card_reader_payment_failed_nfc_disabled_enable_nfc)
            assertThat(state.secondaryActionLabel).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given payment fails because of NFC_DISABLED, when primary button clicked, then tracked and enablenfc emitted`() =
        testBlocking {
            whenever(
                errorMapper.mapPaymentErrorToUiError(
                    CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled,
                    cardReaderConfig,
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
            initViewModel(BUILT_IN)

            viewModel.start()

            val events = viewModel.event.captureValues()
            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(events.last()).isInstanceOf(EnableNfc::class.java)
            verify(tracker).trackPaymentFailedEnabledNfcTapped()
        }

    @Test
    fun `given user clicks on retry and external, when payment fails and retryData are null, then flow restarted from scratch`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            viewModel.start()
            clearInvocations(cardReaderManager)

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given user clicks on retry and built in, when payment fails and retryData are null, then flow restarted from scratch`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            initViewModel(BUILT_IN)
            viewModel.start()
            clearInvocations(cardReaderManager)

            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given user clicks on retry and external, when payment fails, then retryCollectPayment invoked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), any())
        }

    @Test
    fun `given user clicks on retry and built in, when payment fails, then retryCollectPayment invoked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            initViewModel(BUILT_IN)
            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), any())
        }

    @Test
    fun `given user clicks on retry and external, when payment fails, then flow retried with provided PaymentData`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), eq(paymentData))
        }

    @Test
    fun `given user clicks on retry and built in, when payment fails, then flow retried with provided PaymentData`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            initViewModel(BUILT_IN)
            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), eq(paymentData))
        }

    @Test
    fun `given external failed payment, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }

            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderFailedPaymentState).onSecondaryActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given built in failed payment, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderFailedPaymentState).onSecondaryActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `when loading data, then only progress and cancel button is visible`() = testBlocking {
        viewModel.start()
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isTrue
        assertThat(viewState.headerLabel).describedAs("headerLabel")
            .isEqualTo(R.string.card_reader_payment_collect_payment_loading_header)
        assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel").isNull()
        assertThat(viewState.illustration).describedAs("illustration").isNull()
        assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
            .isEqualTo(UiStringRes(R.string.card_reader_payment_collect_payment_loading_payment_state))
        assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
            .isEqualTo(R.dimen.major_275)
        assertThat(viewState.hintLabel).describedAs("hintLabel")
            .isEqualTo(R.string.card_reader_payment_collect_payment_loading_hint)
        assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
        assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
            .isEqualTo(R.string.cancel)
    }

    @Test
    fun `when collecting payment, then progress and cancel button is visible`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.cancel)
        }

    @Test
    fun `when collecting payment, then correct labels and illustration is shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_collect_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_available)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_collect_payment_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `when processing payment, then progress and cancel button is visible`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.cancel)
        }

    @Test
    fun `when processing payment, then correct labels and illustration is shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_processing_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_available)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_processing_payment_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_processing_payment_hint)
        }

    @Test
    fun `when capturing payment, then progress and buttons are hidden`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when capturing payment, then correct labels and illustration is shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_capturing_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_available)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_capturing_payment_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_capturing_payment_hint)
        }

    @Test
    fun `when payment fails, then progress and secondary button is visible`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.cancel)
        }

    @Test
    fun `given external reader, when payment fails, then correct labels, illustration and button are shown`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_payment_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_products_error)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unexpected_error_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.try_again)
        }

    @Test
    fun `given built in reader, when payment fails, then correct labels, illustration and button are shown`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_payment_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_tpp_payment_failed)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unexpected_error_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.try_again)
        }

    @Test
    fun `when payment fails with no network error, then correct paymentStateLabel is shown`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_no_network_state))
        }

    @Test
    fun `when payment fails with payment unknown error, then correct paymentStateLabel is shown`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown, cardReaderConfig, false))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.Unknown, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unknown))
        }

    @Test
    fun `when payment fails with server error, then correct paymentStateLabel is shown`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Server(""), cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Server(""), null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_server_error_state))
        }

    @Test
    fun `when payment succeeds, then receiptUrl stored into a persistant storage`() =
        testBlocking {
            val receiptUrl = "testUrl"
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted(receiptUrl)) }
            }

            viewModel.start()

            verify(paymentReceiptHelper).storeReceiptUrl(eq(ORDER_ID), eq(receiptUrl))
        }

    @Test
    fun `given external reader, when payment succeeds, then correct labels, illustration and buttons are shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_completed_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_celebration)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel").isNull()
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_print_receipt)
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_send_receipt)
            assertThat(viewState.tertiaryActionLabel).describedAs("tertiaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_save_for_later)
        }

    @Test
    fun `given built in reader, when payment succeeds, then correct labels, illustration and buttons are shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_completed_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_tpp_successful_payment)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel").isNull()
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_print_receipt)
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_send_receipt)
            assertThat(viewState.tertiaryActionLabel).describedAs("tertiaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_save_for_later)
        }

    @Test
    fun `given payment flow already started, when start() is invoked, then flow is not restarted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow<CardPaymentStatus> {}
            }

            viewModel.start()
            viewModel.start()
            viewModel.start()
            viewModel.start()

            verify(cardReaderManager, times(1))
                .collectPayment(anyOrNull())
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given billing email empty and built in, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on print receipt button, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(PrintingReceiptState::class.java)
        }

    @Test
    fun `given billing email empty and built in, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(PrintingReceiptState::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(PrintingReceiptState::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on print receipt button, then printing receipt state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(PrintingReceiptState::class.java)
        }

    @Test
    fun `given billing email empty and external, when print result received, then payment successful state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(CANCELLED)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderPaymentSuccessfulState::class.java)
        }

    @Test
    fun `given billing email empty and built in, when print result received, then payment successful state shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(CANCELLED)

            assertThat(viewModel.viewStateData.value).isInstanceOf(BuiltInReaderPaymentSuccessfulState::class.java)
        }

    @Test
    fun `given billing email not empty and external, when print result received, then payment success receipt sent state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(CANCELLED)

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when print result received, then payment success receipt sent state shown`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(CANCELLED)

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java)
        }

    @Test
    fun `given in printing receipt state and external, when view recreated, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onViewCreated()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given in printing receipt state and built in, when view recreated, then PrintReceipt event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onViewCreated()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given not in printing receipt state, when view recreated, then state not changed`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow<CardPaymentStatus> {}
            }
            viewModel.start()
            val originalState = viewModel.viewStateData.value
            assertThat(originalState).isNotInstanceOf(PrintingReceiptState::class.java)

            viewModel.onViewCreated()

            assertThat(viewModel.viewStateData.value).isEqualTo(originalState)
        }

    @Test
    fun `given billing email empty and external, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email empty and built in, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email not empty and external, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on print receipt button, then event tracked`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onPrimaryActionClicked.invoke()

            verify(tracker).trackPrintReceiptTapped()
        }

    @Test
    fun `given get receipt url fails, when user clicks on print receipt button, then ShowSnackbar emitted`() =
        testBlocking {
            // GIVEN
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception()))

            // WHEN
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            // THEN
            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(R.string.receipt_fetching_error)
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
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()

            // THEN
            assertThat((viewModel.event.value as PrintReceipt).receiptUrl).isEqualTo(receiptUrl)
            assertThat((viewModel.event.value as PrintReceipt).documentName).isEqualTo("receipt-order-1")
        }

    @Test
    fun `when OS accepts the print request, then print success event tracked`() = testBlocking {
        viewModel.onPrintResult(STARTED)

        verify(tracker).trackPrintReceiptSucceeded()
    }

    @Test
    fun `when OS refuses the print request, then print failed event tracked`() = testBlocking {
        viewModel.onPrintResult(FAILED)

        verify(tracker).trackPrintReceiptFailed()
    }

    @Test
    fun `when manually cancels the print request, then print cancelled event tracked`() = testBlocking {
        viewModel.onPrintResult(CANCELLED)

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
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(PlayChaChing)
        }

    @Test
    fun `given built in reader and receipt fetching  and sharing success, when user clicks on send receipt button, then PlayChaChing emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(PlayChaChing)
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
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(
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
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(
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
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(
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

            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(R.string.receipt_fetching_error)
        }

    @Test
    fun `given built reader and receipt fetching fails, when user clicks on send receipt button, then ShowSnackabar event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            whenever(paymentReceiptHelper.getReceiptUrl(any())).thenReturn(Result.failure(Exception()))

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(R.string.receipt_fetching_error)
        }

    @Test
    fun `given external reader, when user clicks on send receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            verify(tracker).trackEmailReceiptTapped()
        }

    @Test
    fun `given built in reader, when user clicks on send receipt button, then event tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onSecondaryActionClicked.invoke()

            verify(tracker).trackEmailReceiptTapped()
        }

    @Test
    fun `given billing email empty and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given billing email built in and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given billing email not empty and external, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onTertiaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given billing email not empty and built in, when user clicks on save for later button, then Exit event emitted`() =
        testBlocking {
            whenever(mockedAddress.email).thenReturn("nonemptyemail")
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState)
                .onTertiaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given user presses back button, when re-fetching order, then ReFetchingOrderState shown`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReFetchingOrderState::class.java)
        }

    @Test
    fun `given payment flow is loading, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(LoadingDataState(mock())) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackPaymentCancelled("Loading")
        }

    @Test
    fun `given payment flow is collecting state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackPaymentCancelled("Collecting")
        }

    @Test
    fun `given payment flow is processing state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackPaymentCancelled("Processing")
        }

    @Test
    fun `given payment flow is capturing state, when user presses back button, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackPaymentCancelled("Capturing")
        }

    @Test
    fun `given payment flow is payment failed, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is success state, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is initializing payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as LoadingDataState).onSecondaryActionClicked.invoke()

            verify(tracker).trackPaymentCancelled("Loading")
        }

    @Test
    fun `given payment flow is initializing payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as LoadingDataState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given payment flow is collection payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderCollectPaymentState).onSecondaryActionClicked.invoke()

            verify(tracker).trackPaymentCancelled("Collecting")
        }

    @Test
    fun `given payment flow is collection payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderCollectPaymentState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given payment flow is processing payment state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderProcessingPaymentState).onSecondaryActionClicked.invoke()

            verify(tracker).trackPaymentCancelled("Processing")
        }

    @Test
    fun `given payment flow is processing payment state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderProcessingPaymentState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given payment flow is receipt print state and external, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()
            viewModel.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is receipt print state and built in, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onPrimaryActionClicked.invoke()
            viewModel.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given payment flow is refetching order, when user presses back button, then cancel event is not tracked`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            verify(tracker, never()).trackPaymentCancelled(anyOrNull())
        }

    @Test
    fun `given re-fetching order and external, when user clicks on save for later button, then ReFetchingOrderState shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            simulateFetchOrderJobState(inProgress = true)

            (viewModel.viewStateData.value as ExternalReaderPaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReFetchingOrderState::class.java)
        }

    @Test
    fun `given re-fetching order and built in, when user clicks on save for later button, then ReFetchingOrderState shown`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            initViewModel(BUILT_IN)

            viewModel.start()
            simulateFetchOrderJobState(inProgress = true)

            (viewModel.viewStateData.value as BuiltInReaderPaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReFetchingOrderState::class.java)
        }

    @Test
    fun `given user presses back, when already in ReFetchingOrderState, then snackbar shown and screen dismissed`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)
            viewModel.onBackPressed() // shows ReFetchingOrderState screen
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            viewModel.onBackPressed()

            assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
            assertThat(events[1]).isEqualTo(Exit)
        }

    @Test
    fun `given user presses back, when already showing ReFetchingOrderState, then correct snackbar message shown`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)
            viewModel.onBackPressed() // shows ReFetchingOrderState screen
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            viewModel.onBackPressed()

            assertThat((events[0] as ShowSnackbar).message)
                .isEqualTo(R.string.card_reader_refetching_order_failed)
        }

    @Test
    fun `given user presses back button, when re-fetching order, then screen not dismissed`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            assertThat(viewModel.event.value).isNotEqualTo(Exit)
        }

    @Test
    fun `given user presses back button, when not re-fetching order, then screen dismissed`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = false)

            viewModel.onBackPressed()

            assertThat(viewModel.event.value).isEqualTo(Exit)
        }

    @Test
    fun `given ReFetchingOrderState shown, when re-fetching order completes, then screen auto-dismissed`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)
            viewModel.onBackPressed() // show ReFetchingOrderState screen

            advanceUntilIdle()

            assertThat(viewModel.event.value).isEqualTo(Exit)
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
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, true))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            initViewModel(BUILT_IN)
            viewModel.start()

            viewModel.onBackPressed()

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
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            viewModel.start()

            viewModel.onBackPressed()

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
            viewModel.start()

            viewModel.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given ReFetchingOrderState not shown, when re-fetching order completes, then screen not auto-dismissed`() =
        testBlocking {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.reFetchOrder()

            assertThat(viewModel.event.value).isNotEqualTo(Exit)
        }

    @Test
    fun `when re-fetching order fails, then SnackBar shown`() =
        testBlocking {
            whenever(orderRepository.fetchOrderById(any())).thenReturn(null)
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            viewModel.reFetchOrder()

            assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
        }

    @Test
    fun `given user leaves the screen, when payment fails, then payment canceled`() =
        testBlocking {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            viewModel.start()

            viewModel.onCleared()

            verify(cardReaderManager).cancelPayment(any())
        }

    @Test
    fun `given user leaves the screen, when payment succeeded on retry, then payment NOT canceled`() =
        testBlocking {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic, cardReaderConfig, false))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any()))
                .thenAnswer {
                    flow {
                        emit(paymentFailedWithValidDataForRetry)
                        emit(PaymentCompleted(""))
                    }
                }
            viewModel.start()

            viewModel.onCleared()

            verify(cardReaderManager, never()).cancelPayment(any())
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

            viewModel.start()

            assertThat((viewModel.event.value)).isNull()
            assertThat(viewModel.viewStateData.value).isInstanceOf(ExternalReaderProcessingPaymentState::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then make sure NOT to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            // when
            viewModel.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then make sure NOT to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

            // When
            viewModel.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is connected, when payment screen is shown, then proceed to initiate payment`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))

            // When
            viewModel.start()

            // Then
            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then show error Snackbar`() =
        testBlocking {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            viewModel.event.observeForever {
                events.add(it)
            }

            // When
            viewModel.start()

            // Then
            assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then Snackbar is shown with message`() =
        testBlocking {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
            viewModel.event.observeForever {
                events.add(it)
            }

            // When
            viewModel.start()

            // Then
            assertThat((events[0] as ShowSnackbar).message)
                .isEqualTo(R.string.card_reader_payment_reader_not_connected)
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then exit event is triggered`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))

            // When
            viewModel.start()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then show error Snackbar`() =
        testBlocking {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
            viewModel.event.observeForever {
                events.add(it)
            }

            // When
            viewModel.start()

            // Then
            assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then Snackbar is shown with the message`() =
        testBlocking {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))
            viewModel.event.observeForever {
                events.add(it)
            }

            // When
            viewModel.start()

            // Then
            assertThat((events[0] as ShowSnackbar).message)
                .isEqualTo(R.string.card_reader_payment_reader_not_connected)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then exit event is triggered`() =
        testBlocking {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            // When
            viewModel.start()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `when flow started, then correct order key is propagated to CardReaderManager`() =
        testBlocking {
            // Given
            val captor = argumentCaptor<PaymentInfo>()

            // When
            viewModel.start()

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
            viewModel.start()

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
            viewModel.start()

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
            viewModel.start()

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
            viewModel.start()

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
            viewModel.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.feeAmount).isNull()
        }

    //endregion - Payments tests

    //region - Interac Refund tests

    @Test
    fun `given interac refund shown, when RETRY message received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_retry_card_prompt)
        }

    @Test
    fun `given Unknown refund error, when view model starts, then ui has contact support button`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

            val externalReaderFailedPaymentState = viewModel.viewStateData.value as FailedRefundState
            assertThat(externalReaderFailedPaymentState.primaryActionLabel).isEqualTo(R.string.support_contact)
            assertThat(externalReaderFailedPaymentState.secondaryActionLabel).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given unknown error, when contact support clicked, then contact support event emited`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

            val externalReaderFailedPaymentState = viewModel.viewStateData.value as FailedRefundState
            externalReaderFailedPaymentState.onPrimaryActionClicked.invoke()
            assertThat(viewModel.event.value).isEqualTo(ContactSupport)
        }

    @Test
    fun `given interac refund shown, when INSERT_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when INSERT_OR_SWIPE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when SWIPE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `given interac refund shown, when REMOVE_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_remove_card_prompt)
        }

    @Test
    fun `given interac refund, when MULTIPLE_CONTACTLESS_CARDS_DETECTED received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_multiple_contactless_cards_detected_prompt)
        }

    @Test
    fun `given interac refund shown, when TRY_ANOTHER_READ_METHOD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_read_method_prompt)
        }

    @Test
    fun `given interac refund, when TRY_ANOTHER_CARD received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_card_prompt)
        }

    @Test
    fun `given interac refund, when CHECK_MOBILE_DEVICE received, then refund payment hint updated`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(CHECK_MOBILE_DEVICE))
                }
            }

            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectRefundState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_check_mobile_device_prompt)
        }

    @Test
    fun `given interac refund, when payment screen shown, then loading data state is shown`() {
        setupViewModelForInteracRefund()

        viewModel.start()

        assertThat(viewModel.viewStateData.value).isInstanceOf(RefundLoadingDataState::class.java)
    }

    @Test
    fun `when initializing interac refund, then ui updated to initializing refund state `() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(RefundLoadingDataState::class.java)
        }

    @Test
    fun `given fetch order failed, when initializing interac refund, then ui updated to proper error state `() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedRefundState::class.java)
        }

    @Test
    fun `when collecting interac refund, then ui updated to collecting refund state`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(CollectRefundState::class.java)
        }

    @Test
    fun `when processing interac refund, then ui updated to processing refund state`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ProcessingRefundState::class.java)
        }

    @Test
    fun `when interac refund completed, then ui updated to refund successful state`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InteracRefundSuccess) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(RefundSuccessfulState::class.java)
        }

    @Test
    fun `when interac refund fails, then ui updated to refund failed state`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedRefundState::class.java)
        }

    @Test
    fun `when interac refund fails, then invalidate onboarding cache`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

            verify(cardReaderOnboardingChecker).invalidateCache()
        }

    @Test
    fun `given chargeId is null, when interac refund initiated, then proper state is shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedRefundState::class.java)
        }

    @Test
    fun `given non retryable error, when interac refund initiated, then correct labels and illustration is shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_products_error)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_interac_refund_refund_failed_unexpected_error_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_failed_ok)
        }

    @Test
    fun `given non retryable error, when interac refund initiated, then primary action is back press`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!
            (viewState as FailedRefundState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `when collecting interac refund, then progress and cancel button is visible`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.cancel)
        }

    @Test
    fun `when collecting interac refund, then correct labels and illustration is shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_available)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_payment_collect_payment_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment_hint)
        }

    @Test
    fun `when processing interac refund, then progress and buttons are hidden`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when processing interac refund, then correct labels and illustration is shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_payment)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration")
                .isEqualTo(R.drawable.img_card_reader_available)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_interac_refund_refund_processing_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_processing_payment_hint)
        }

    @Test
    fun `when interac refund fails, then progress and secondary button is visible`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.cancel)
        }

    @Test
    fun `given refund flow is initializing state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as RefundLoadingDataState).onSecondaryActionClicked.invoke()

            verify(tracker).trackInteracRefundCancelled("Loading")
        }

    @Test
    fun `given refund flow is initializing state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as RefundLoadingDataState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given refund flow is collection state, when user presses cancel, then cancel event is tracked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as CollectRefundState).onSecondaryActionClicked.invoke()

            verify(tracker).trackInteracRefundCancelled("Collecting")
        }

    @Test
    fun `given refund flow is collection state, when user presses cancel, then exit event emitted`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as CollectRefundState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `when interac refund fails, then correct labels, illustration and button are shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_products_error)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_interac_refund_refund_failed_unexpected_error_state))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.try_again)
        }

    @Test
    fun `when interac refund fails of non retryable error, then correct labels, illustration and button are shown`() =
        testBlocking {
            val nonRetryableError = CardDeclined.InvalidAccount
            setupViewModelForInteracRefund()
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(CardDeclined.InvalidAccount)
            ).thenReturn(InteracRefundFlowError.Declined.InvalidAmount)
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow {
                    emit(
                        CardInteracRefundStatus.InteracRefundFailure(
                            nonRetryableError,
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

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_products_error)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(UiStringRes(R.string.card_reader_interac_refund_refund_failed_invalid_amount))
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_failed_ok)
        }

    @Test
    fun `when interac refund fails, then interac refund failed event is triggered`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `when interac refund fails, then interac refund failed event is triggered with correct data`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

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
            setupViewModelForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `given failed to fetch order, when interac refund fails, then event is triggered with correct data`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)
            val captor = argumentCaptor<String>()
            val expectedErrorMessage = "Fetching order failed"

            viewModel.start()

            verify(tracker).trackInteracPaymentFailed(any(), captor.capture(), any())
            assertThat(captor.firstValue).isEqualTo(expectedErrorMessage)
        }

    @Test
    fun `given null chargeid on order, when interac refund fails, then interac refund failed event is triggered`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(mockedOrder.chargeId).thenReturn(null)
            whenever(
                interacRefundErrorMapper.mapRefundErrorToUiError(
                    CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
                )
            ).thenReturn(InteracRefundFlowError.NonRetryableGeneric)

            viewModel.start()

            verify(tracker).trackInteracPaymentFailed(any(), any(), any())
        }

    @Test
    fun `given null chargeid on order, when interac refund fails, then event is triggered with correct data`() =
        testBlocking {
            setupViewModelForInteracRefund()
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

            viewModel.start()

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
    fun `when interac refund succeeds, then correct labels, illustration and buttons are shown`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InteracRefundSuccess) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_interac_refund_refund_completed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_celebration)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel").isNull()
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
        }

    @Test
    fun `given interac refund flow already started, when start() is invoked, then flow is not restarted`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow<CardInteracRefundStatus> {}
            }

            viewModel.start()
            viewModel.start()
            viewModel.start()
            viewModel.start()

            verify(cardReaderManager, times(1))
                .refundInteracPayment(anyOrNull(), anyOrNull())
        }

    @Test
    fun `given user clicks on retry, when interac refund fails, then refundInteracPayment invoked`() =
        testBlocking {
            setupViewModelForInteracRefund()
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
            viewModel.start()

            (viewModel.viewStateData.value as FailedRefundState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            // Times 2 because, refundInteracPayment() method gets called when refund is initiated
            // as well as when the refund is retried.
            verify(cardReaderManager, times(2)).refundInteracPayment(any(), any())
        }

    @Test
    fun `given refund flow is loading, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InitializingInteracRefund) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Loading")
        }

    @Test
    fun `given refund flow is collecting, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.CollectingInteracRefund) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Collecting")
        }

    @Test
    fun `given refund flow is processing, when user presses back button, then refund cancel event is tracked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.ProcessingInteracRefund) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).trackInteracRefundCancelled("Processing")
        }

    @Test
    fun `given refund failed state and connected BI, when user presses back, then disconnect from a reader invoked`() =
        testBlocking {
            setupViewModelForInteracRefund()
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
            viewModel.start()

            viewModel.onBackPressed()

            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `given refund failed state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupViewModelForInteracRefund()
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
            viewModel.start()

            viewModel.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund failed state and not connected, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.NotConnected())
            )

            viewModel.start()

            viewModel.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund success state and connected BT, when user presses back, then disconnect not invoked`() =
        testBlocking {
            setupViewModelForInteracRefund()
            val cardReader: CardReader = mock {
                on { type }.thenReturn("WISEPAD_3")
            }
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            whenever(cardReaderManager.refundInteracPayment(any(), any())).thenAnswer {
                flow { emit(CardInteracRefundStatus.InteracRefundSuccess) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(cardReaderManager, never()).disconnectReader()
        }

    @Test
    fun `given refund failed state, when user clicks on secondary button, then exit event is triggered`() =
        testBlocking {
            setupViewModelForInteracRefund()
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
            viewModel.start()

            (viewModel.viewStateData.value as FailedRefundState).onSecondaryActionClicked!!.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    //endregion - Interac Refund tests

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

            viewModel.start()

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

            viewModel.start()

            verify(cardReaderTrackingInfoKeeper, never()).setCardReaderBatteryLevel(anyFloat())
        }

    @Test
    fun `given ttp in progress and reader connected, when vm starts, then AppKilledWhileInBackground state emitted`() =
        testBlocking {
            val cardReader: CardReader = mock()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            initViewModel(BUILT_IN, ("ttp_payment_in_progress" to true))

            viewModel.start()

            verify(tracker).trackPaymentFailed("VM killed when TTP activity in foreground")
            val state = viewModel.viewStateData.value as BuiltInReaderFailedPaymentState
            assertThat(state.primaryActionLabel).isEqualTo(R.string.support_contact)
            assertThat(state.secondaryActionLabel).isEqualTo(R.string.cancel)
            assertThat(state.paymentStateLabel).isEqualTo(
                UiStringRes(R.string.card_reader_payment_vm_killed_when_tpp_in_foreground)
            )
            assertThat(state.hintLabel).isNull()
        }

    @Test
    fun `given ttp not in progress and reader connected, when vm starts, then AppKilledWhileInBackground state not emitted`() =
        testBlocking {
            val cardReader: CardReader = mock()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            initViewModel(BUILT_IN, ("ttp_payment_in_progress" to false))

            viewModel.start()

            verify(tracker, never()).trackPaymentFailed("VM killed when TTP activity in foreground")
            assertThat(viewModel.viewStateData.value).isNotInstanceOf(BuiltInReaderFailedPaymentState::class.java)
        }

    @Test
    fun `given AppKilledWhileInBackground, when vm starts, then payment collection doesnt start`() =
        testBlocking {
            val cardReader: CardReader = mock()
            whenever(cardReaderManager.readerStatus).thenReturn(
                MutableStateFlow(CardReaderStatus.Connected(cardReader))
            )
            initViewModel(BUILT_IN, ("ttp_payment_in_progress" to true))

            viewModel.start()

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
        viewModel.reFetchOrder()
    }

    private fun setupViewModelForInteracRefund() {
        viewModel = CardReaderPaymentViewModel(
            interacRefundSavedState,
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            selectedSite = selectedSite,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            interacRefundableChecker = interacRefundableChecker,
            tracker = tracker,
            appPrefs = appPrefs,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            interacRefundErrorMapper = interacRefundErrorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            cardReaderPaymentReaderTypeStateProvider = cardReaderPaymentReaderTypeStateProvider,
            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
            paymentReceiptHelper = paymentReceiptHelper,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            cardReaderConfigProvider = cardReaderConfigProvider,
            paymentReceiptShare = paymentReceiptShare,
        )
    }

    private fun initViewModel(
        readerType: CardReaderType,
        savedStateValue: Pair<String, Any>? = null
    ) {
        viewModel = CardReaderPaymentViewModel(
            CardReaderPaymentDialogFragmentArgs(
                CardReaderFlowParam.PaymentOrRefund.Payment(ORDER_ID, ORDER),
                readerType,
            ).toSavedStateHandle().also {
                if (savedStateValue != null) it[savedStateValue.first] = savedStateValue.second
            },
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            selectedSite = selectedSite,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            interacRefundableChecker = interacRefundableChecker,
            tracker = tracker,
            appPrefs = appPrefs,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            interacRefundErrorMapper = interacRefundErrorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            cardReaderPaymentReaderTypeStateProvider = cardReaderPaymentReaderTypeStateProvider,
            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
            paymentReceiptHelper = paymentReceiptHelper,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            cardReaderConfigProvider = cardReaderConfigProvider,
            paymentReceiptShare = paymentReceiptShare,
        )
    }
}
