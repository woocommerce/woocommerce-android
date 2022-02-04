package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.*
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.*
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.*
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Server
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.PaymentFlowError.*
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.orders.cardreader.ViewState.*
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.test.assertEquals

private val DUMMY_TOTAL = BigDecimal(10.72)
private const val DUMMY_CURRENCY_SYMBOL = "£"
private const val DUMMY_ORDER_NUMBER = "123"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class CardReaderPaymentViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private lateinit var viewModel: CardReaderPaymentViewModel
    private val cardReaderManager: CardReaderManager = mock()
    private val orderRepository: OrderDetailRepository = mock()
    private var resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooStore: WooCommerceStore = mock()

    private val paymentFailedWithEmptyDataForRetry = PaymentFailed(Generic, null, "dummy msg")
    private val paymentFailedWithValidDataForRetry = PaymentFailed(Generic, mock(), "dummy msg")
    private val paymentFailedWithNoNetwork = PaymentFailed(NoNetwork, mock(), "dummy msg")
    private val paymentFailedWithPaymentDeclined = PaymentFailed(DeclinedByBackendError.Unknown, mock(), "dummy msg")
    private val paymentFailedWithCardReadTimeOut = PaymentFailed(Generic, mock(), "dummy msg")
    private val paymentFailedWithServerError = PaymentFailed(Server, mock(), "dummy msg")
    private val paymentFailedWithAmountTooSmall = PaymentFailed(
        DeclinedByBackendError.AmountTooSmall, mock(), "dummy msg"
    )

    private val savedState: SavedStateHandle = CardReaderPaymentDialogFragmentArgs(ORDER_ID).initSavedStateHandle()

    private val errorMapper: CardReaderPaymentErrorMapper = mock()

    @Before
    fun setUp() = coroutinesTestRule.testDispatcher.runBlockingTest {
        viewModel = CardReaderPaymentViewModel(
            savedState,
            cardReaderManager = cardReaderManager,
            orderRepository = orderRepository,
            resourceProvider = resourceProvider,
            selectedSite = selectedSite,
            paymentCollectibilityChecker = paymentCollectibilityChecker,
            tracker = tracker,
            appPrefsWrapper = appPrefsWrapper,
            currencyFormatter = currencyFormatter,
            errorMapper = errorMapper,
            wooStore = wooStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )

        val mockedOrder = mock<Order>()
        whenever(orderRepository.getOrderById(any())).thenReturn(mockedOrder)
        whenever(mockedOrder.total).thenReturn(DUMMY_TOTAL)
        whenever(mockedOrder.currency).thenReturn("GBP")
        whenever(currencyFormatter.formatAmountWithCurrency("GBP", DUMMY_TOTAL.toDouble()))
            .thenReturn("$DUMMY_CURRENCY_SYMBOL$DUMMY_TOTAL")
        val address = mock<Address>()
        whenever(mockedOrder.billingAddress).thenReturn(address)
        whenever(address.email).thenReturn("test@test.test")
        whenever(address.firstName).thenReturn("Tester")
        whenever(address.lastName).thenReturn("Test")
        whenever(mockedOrder.orderKey).thenReturn("wc_order_j0LMK3bFhalEL")
        whenever(mockedOrder.number).thenReturn(DUMMY_ORDER_NUMBER)
        whenever(mockedOrder.id).thenReturn(ORDER_ID)
        whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(mockedOrder)
        whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))
        whenever(cardReaderManager.collectPayment(any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.retryCollectPayment(any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(selectedSite.get()).thenReturn(SiteModel().apply { name = "testName" }.apply { url = "testUrl.com" })
        whenever(resourceProvider.getString(anyOrNull(), anyOrNull())).thenReturn("")
        whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
        whenever(appPrefsWrapper.getReceiptUrl(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn("test url")
        whenever(appPrefsWrapper.getCardReaderStatementDescriptor(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn("test statement descriptor")
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
        whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
            flow<BluetoothCardReaderMessages> {}
        }
    }

    @Test
    fun `given collect payment shown, when RETRY message received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(RETRY_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_retry_card_prompt)
        }

    @Test
    fun `given collect payment shown, when INSERT_CARD received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when INSERT_OR_SWIPE_CARD received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(INSERT_OR_SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when SWIPE_CARD received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(SWIPE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `given collect payment shown, when REMOVE_CARD received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(REMOVE_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_remove_card_prompt)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_CARD message received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_CARD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_card_prompt)
        }

    @Test
    fun `given collect payment shown, when TRY_OTHER_READ message received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(TRY_ANOTHER_READ_METHOD))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_try_another_read_method_prompt)
        }

    @Test
    fun `given collect payment shown, when MULTIPLE_CARDS_DETECTED received, then collect payment hint updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.displayBluetoothCardReaderMessages).thenAnswer {
                flow {
                    emit(BluetoothCardReaderMessages.CardReaderDisplayMessage(MULTIPLE_CONTACTLESS_CARDS_DETECTED))
                }
            }

            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat((viewModel.viewStateData.value as CollectPaymentState).hintLabel)
                .isEqualTo(R.string.card_reader_payment_multiple_contactless_cards_detected_prompt)
        }

    @Test
    fun `given fetching order fails, when payment screen shown, then FailedPayment state is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
        }

    @Test
    fun `when fetching order fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            verify(tracker).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED), anyOrNull(), anyOrNull(), anyOrNull()
            )
        }

    @Test
    fun `given fetching order fails, when payment screen shown, then correct error message shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrderById(ORDER_ID)).thenReturn(null)

            viewModel.start()

            assertThat((viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel)
                .isEqualTo(R.string.order_error_fetch_generic)
        }

    @Test
    fun `when payment screen shown, then loading data state is shown`() {
        viewModel.start()

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
    }

    @Test
    fun `when payment not collectable, then flow terminated and snackbar shown`() {
        whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.start()

        assertThat((events[0] as ShowSnackbar).message)
            .isEqualTo(R.string.card_reader_payment_order_paid_payment_cancelled)
        assertThat(events[1]).isInstanceOf(Exit::class.java)
    }

    @Test
    fun `when flow started, then correct payment description is propagated to CardReaderManager`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val siteName = "testName"
            val expectedResult = "hooray"
            whenever(selectedSite.get()).thenReturn(
                SiteModel().apply {
                    name = siteName
                    url = ""
                }
            )
            whenever(resourceProvider.getString(R.string.card_reader_payment_description, DUMMY_ORDER_NUMBER, siteName))
                .thenReturn(expectedResult)
            val captor = argumentCaptor<PaymentInfo>()

            viewModel.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.paymentDescription).isEqualTo(expectedResult)
        }

    @Test
    fun `when flow started, then correct statement descriptor is propagated to CardReaderManager`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val expectedResult = "hooray"
            whenever(appPrefsWrapper.getCardReaderStatementDescriptor(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(expectedResult)
            val captor = argumentCaptor<PaymentInfo>()

            viewModel.start()

            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.statementDescriptor).isEqualTo(expectedResult)
        }

    @Test
    fun `when initializing payment, then ui updated to initializing payment state `() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
        }

    @Test
    fun `when collecting payment, then ui updated to collecting payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(CollectPaymentState::class.java)
        }

    @Test
    fun `when processing payment, then ui updated to processing payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ProcessingPaymentState::class.java)
        }

    @Test
    fun `when capturing payment, then ui updated to capturing payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(CapturingPaymentState::class.java)
        }

    @Test
    fun `when payment completed, then ui updated to payment successful state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(PaymentSuccessfulState::class.java)
        }

    @Test
    fun `when payment completed, then cha-ching sound is played`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()
            val events = mutableListOf<Event>()
            viewModel.event.observeForever {
                events.add(it)
            }

            assertThat(events[0]).isInstanceOf(CardReaderPaymentViewModel.PlayChaChing::class.java)
        }

    @Test
    fun `when payment completed, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            verify(tracker).track(CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)
        }

    @Test
    fun `when payment fails, then ui updated to failed state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
        }

    @Test
    fun `when payment fails because of NoNetwork, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `when payment fails because of Unknown, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `when payment fails because of CARD_READ_TIMEOUT, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `when payment fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            verify(tracker).track(eq(CARD_PRESENT_COLLECT_PAYMENT_FAILED), any(), any(), any())
        }

    @Test
    fun `when payment fails because of NO_NETWORK, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithNoNetwork) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.NoNetwork.message
            )
        }

    @Test
    fun `when payment fails because of declined Unknown, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithPaymentDeclined) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                Unknown.message
            )
        }

    @Test
    fun `when payment fails because of CARD_READ_TIME_OUT, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithCardReadTimeOut) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `when payment fails because of GENERIC_ERROR, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.Generic.message
            )
        }

    @Test
    fun `when payment fails because of SERVER_ERROR, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Server)).thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                PaymentFlowError.Server.message
            )
        }

    @Test
    fun `when payment fails because of AMOUNT_TOO_SMALL, then error is mapped correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.AmountTooSmall))
                .thenReturn(AmountTooSmall)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).paymentStateLabel,
                AmountTooSmall.message
            )
        }

    @Test
    fun `when payment fails because of AMOUNT_TOO_SMALL, then failed state has ok button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.AmountTooSmall))
                .thenReturn(AmountTooSmall)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).primaryActionLabel,
                R.string.card_reader_payment_payment_failed_ok
            )
        }

    @Test
    fun `when payment fails not because of AMOUNT_TOO_SMALL, then failed state has Try again button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Server))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithServerError) }
            }

            viewModel.start()

            assertEquals(
                (viewModel.viewStateData.value as FailedPaymentState).primaryActionLabel,
                R.string.try_again
            )
        }

    @Test
    fun `when payment fails because of AMOUNT_TOO_SMALL, then clicking on ok button triggers exit event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.AmountTooSmall))
                .thenReturn(AmountTooSmall)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithAmountTooSmall) }
            }

            viewModel.start()
            (viewModel.viewStateData.value as FailedPaymentState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given user clicks on retry, when payment fails and retryData are null, then flow restarted from scratch`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            viewModel.start()
            clearInvocations(cardReaderManager)

            (viewModel.viewStateData.value as FailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given user clicks on retry, when payment fails, then retryCollectPayment invoked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic))
                .thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as FailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), any())
        }

    @Test
    fun `given user clicks on retry, when payment fails, then flow retried with provided PaymentData`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic)).thenReturn(PaymentFlowError.Generic)
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Generic, paymentData, "dummy msg")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as FailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).retryCollectPayment(any(), eq(paymentData))
        }

    @Test
    fun `when loading data, then only progress is visible`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        viewModel.start()
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isTrue
        assertThat(viewState.headerLabel).describedAs("headerLabel")
            .isEqualTo(R.string.card_reader_payment_collect_payment_loading_header)
        assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel").isNull()
        assertThat(viewState.illustration).describedAs("illustration").isNull()
        assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
            .isEqualTo(R.string.card_reader_payment_collect_payment_loading_payment_state)
        assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
            .isEqualTo(R.dimen.major_275)
        assertThat(viewState.hintLabel).describedAs("hintLabel")
            .isEqualTo(R.string.card_reader_payment_collect_payment_loading_hint)
        assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
        assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
    }

    @Test
    fun `when collecting payment, then progress and buttons are hidden`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when collecting payment, then correct labels and illustration is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                .isEqualTo(R.string.card_reader_payment_collect_payment_state)
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_collect_payment_hint)
        }

    @Test
    fun `when processing payment, then progress and buttons are hidden`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when processing payment, then correct labels and illustration is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                .isEqualTo(R.string.card_reader_payment_processing_payment_state)
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_processing_payment_hint)
        }

    @Test
    fun `when capturing payment, then progress and buttons are hidden`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                .isEqualTo(R.string.card_reader_payment_capturing_payment_state)
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel")
                .isEqualTo(R.string.card_reader_payment_capturing_payment_hint)
        }

    @Test
    fun `when payment fails, then progress and secondary button are hidden`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic)).thenReturn(PaymentFlowError.Generic)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when payment fails, then correct labels, illustration and button are shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic)).thenReturn(PaymentFlowError.Generic)
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
                .isEqualTo(R.string.card_reader_payment_failed_unexpected_error_state)
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_100)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.try_again)
        }

    @Test
    fun `when payment fails with no network error, then correct paymentStateLabel is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork)).thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(R.string.card_reader_payment_failed_no_network_state)
        }

    @Test
    fun `when payment fails with payment unknown error, then correct paymentStateLabel is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(DeclinedByBackendError.Unknown))
                .thenReturn(Unknown)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(DeclinedByBackendError.Unknown, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(R.string.card_reader_payment_failed_unknown)
        }

    @Test
    fun `when payment fails with server error, then correct paymentStateLabel is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Server))
                .thenReturn(PaymentFlowError.Server)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(Server, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(R.string.card_reader_payment_failed_server_error_state)
        }

    @Test
    fun `when payment succeeds, then receiptUrl stored into a persistant storage`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val receiptUrl = "testUrl"
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted(receiptUrl)) }
            }

            viewModel.start()

            verify(appPrefsWrapper).setReceiptUrl(any(), any(), any(), any(), eq(receiptUrl))
        }

    @Test
    fun `when payment succeeds, then correct labels, illustration and buttons are shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `given payment flow already started, when start() is invoked, then flow is not restarted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `when user clicks on print receipt button, then PrintReceipt event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `when user clicks on print receipt button, then printing receipt state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value)
                .isInstanceOf(PrintingReceiptState::class.java)
        }

    @Test
    fun `when print result received, then payment successful state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(CANCELLED)

            assertThat(viewModel.viewStateData.value).isInstanceOf(PaymentSuccessfulState::class.java)
        }

    @Test
    fun `given in printing receipt state, when view recreated, then PrintReceipt event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onViewCreated()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `given not in printing receipt state, when view recreated, then state not changed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `when user clicks on print receipt button, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            verify(tracker).track(RECEIPT_PRINT_TAPPED)
        }

    @Test
    fun `when OS accepts the print request, then print success event tracked`() {
        viewModel.onPrintResult(STARTED)

        verify(tracker).track(RECEIPT_PRINT_SUCCESS)
    }

    @Test
    fun `when OS refuses the print request, then print failed event tracked`() {
        viewModel.onPrintResult(FAILED)

        verify(tracker).track(RECEIPT_PRINT_FAILED)
    }

    @Test
    fun `when manually cancels the print request, then print cancelled event tracked`() {
        viewModel.onPrintResult(CANCELLED)

        verify(tracker).track(RECEIPT_PRINT_CANCELED)
    }

    @Test
    fun `when user clicks on send receipt button, then SendReceipt event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(SendReceipt::class.java)
        }

    @Test
    fun `when user clicks on send receipt button, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onSecondaryActionClicked.invoke()

            verify(tracker).track(RECEIPT_EMAIL_TAPPED)
        }

    @Test
    fun `when user clicks on save for later button, then Exit event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `when email activity not found, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            viewModel.onEmailActivityNotFound()

            verify(tracker).track(RECEIPT_EMAIL_FAILED)
        }

    @Test
    fun `given user presses back button, when re-fetching order, then ReFetchingOrderState shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReFetchingOrderState::class.java)
        }

    @Test
    fun `given payment flow is loading, when user presses back button, then cancel event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(LoadingDataState) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                eq("User manually cancelled the payment during state Loading")
            )
        }

    @Test
    fun `given payment flow is collecting state, when user presses back button, then cancel event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                eq("User manually cancelled the payment during state Collecting")
            )
        }

    @Test
    fun `given payment flow is processing state, when user presses back button, then cancel event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                eq("User manually cancelled the payment during state Processing")
            )
        }

    @Test
    fun `given payment flow is capturing state, when user presses back button, then cancel event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                eq("User manually cancelled the payment during state Capturing")
            )
        }

    @Test
    fun `given payment flow is payment failed, when user presses back button, then cancel event is not tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(NoNetwork))
                .thenReturn(PaymentFlowError.NoNetwork)
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentFailed(NoNetwork, null, "")) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker, never()).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }

    @Test
    fun `given payment flow is success state, when user presses back button, then cancel event is not tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            viewModel.onBackPressed()

            verify(tracker, never()).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }

    @Test
    fun `given payment flow is receipt print state, when user presses back button, then cancel event is not tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()
            viewModel.onBackPressed()

            verify(tracker, never()).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }

    @Test
    fun `given payment flow is refetching order, when user presses back button, then cancel event is not tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            verify(tracker, never()).track(
                eq(CARD_PRESENT_COLLECT_PAYMENT_CANCELLED),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }

    @Test
    fun `given re-fetching order, when user clicks on save for later button, then ReFetchingOrderState shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            simulateFetchOrderJobState(inProgress = true)

            (viewModel.viewStateData.value as PaymentSuccessfulState).onTertiaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReFetchingOrderState::class.java)
        }

    @Test
    fun `given user presses back, when already in ReFetchingOrderState, then snackbar shown and screen dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.onBackPressed()

            assertThat(viewModel.event.value).isNotEqualTo(Exit)
        }

    @Test
    fun `given user presses back button, when not re-fetching order, then screen dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            simulateFetchOrderJobState(inProgress = false)

            viewModel.onBackPressed()

            assertThat(viewModel.event.value).isEqualTo(Exit)
        }

    @Test
    fun `given ReFetchingOrderState shown, when re-fetching order completes, then screen auto-dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            simulateFetchOrderJobState(inProgress = true)
            viewModel.onBackPressed() // show ReFetchingOrderState screen

            advanceUntilIdle()

            assertThat(viewModel.event.value).isEqualTo(Exit)
        }

    @Test
    fun `given ReFetchingOrderState not shown, when re-fetching order completes, then screen not auto-dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            simulateFetchOrderJobState(inProgress = true)

            viewModel.reFetchOrder()

            assertThat(viewModel.event.value).isNotEqualTo(Exit)
        }

    @Test
    fun `when re-fetching order fails, then SnackBar shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            viewModel.start()

            viewModel.onCleared()

            verify(cardReaderManager).cancelPayment(any())
        }

    @Test
    fun `given user leaves the screen, when payment succeeded on retry, then payment NOT canceled`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(errorMapper.mapPaymentErrorToUiError(Generic)).thenReturn(PaymentFlowError.Generic)
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                }
            }

            viewModel.start()

            assertThat((viewModel.event.value)).isNull()
            assertThat(viewModel.viewStateData.value).isInstanceOf(ProcessingPaymentState::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then make sure NOT to initiate payment`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connecting))

            // when
            viewModel.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then make sure NOT to initiate payment`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected))

            // When
            viewModel.start()

            // Then
            verify(cardReaderManager, never()).collectPayment(any())
        }

    @Test
    fun `given reader status is connected, when payment screen is shown, then proceed to initiate payment`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.Connected(mock())))

            // When
            viewModel.start()

            // Then
            verify(cardReaderManager).collectPayment(any())
        }

    @Test
    fun `given reader status is NOT connected, when payment screen is shown, then show error Snackbar`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected))
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            val events = mutableListOf<Event>()
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected))
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            whenever(cardReaderManager.readerStatus).thenReturn(MutableStateFlow(CardReaderStatus.NotConnected))

            // When
            viewModel.start()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.event.value).isInstanceOf(Exit::class.java)
        }

    @Test
    fun `given reader status is connecting, when payment screen is shown, then show error Snackbar`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // Given
            val captor = argumentCaptor<PaymentInfo>()

            // When
            viewModel.start()

            // Then
            verify(cardReaderManager).collectPayment(captor.capture())
            assertThat(captor.firstValue.orderKey).isEqualTo("wc_order_j0LMK3bFhalEL")
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
}
