package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.*
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_EMAIL_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.GENERIC_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.NO_NETWORK
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.PAYMENT_DECLINED
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.SERVER_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PrintingReceiptState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ReFetchingOrderState
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import java.math.BigDecimal

private val DUMMY_TOTAL = BigDecimal(10.72)
private const val DUMMY_ORDER_NUMBER = "123"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardReaderPaymentViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    private lateinit var viewModel: CardReaderPaymentViewModel
    private val cardReaderManager: CardReaderManager = mock()
    private val orderRepository: OrderDetailRepository = mock()
    private var resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val paymentFailedWithEmptyDataForRetry = PaymentFailed(GENERIC_ERROR, null, "dummy msg")
    private val paymentFailedWithValidDataForRetry = PaymentFailed(GENERIC_ERROR, mock(), "dummy msg")

    private val savedState: SavedStateHandle = CardReaderPaymentDialogArgs(ORDER_IDENTIFIER).initSavedStateHandle()

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
            appPrefsWrapper = appPrefsWrapper
        )

        val mockedOrder = mock<Order>()
        whenever(orderRepository.getOrder(any())).thenReturn(mockedOrder)
        whenever(mockedOrder.total).thenReturn(DUMMY_TOTAL)
        whenever(mockedOrder.currency).thenReturn("USD")
        val address = mock<Address>()
        whenever(mockedOrder.billingAddress).thenReturn(address)
        whenever(address.email).thenReturn("test@test.test")
        whenever(mockedOrder.number).thenReturn(DUMMY_ORDER_NUMBER)
        whenever(orderRepository.fetchOrder(ORDER_IDENTIFIER, false)).thenReturn(mockedOrder)
        whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(cardReaderManager.retryCollectPayment(any(), any())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
        whenever(selectedSite.get()).thenReturn(SiteModel().apply { name = "testName" })
        whenever(resourceProvider.getString(anyOrNull(), anyOrNull())).thenReturn("")
        whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
        whenever(appPrefsWrapper.getReceiptUrl(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn("test url")
    }

    @Test
    fun `given fetching order fails, when payment screen shown, then FailedPayment state is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrder(ORDER_IDENTIFIER, false)).thenReturn(null)

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
        }

    @Test
    fun `when fetching order fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrder(ORDER_IDENTIFIER, false)).thenReturn(null)

            viewModel.start()

            verify(tracker).track(
                eq(AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_FAILED), anyOrNull(), anyOrNull(), anyOrNull()
            )
        }

    @Test
    fun `given fetching order fails, when payment screen shown, then correct error message shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderRepository.fetchOrder(ORDER_IDENTIFIER, false)).thenReturn(null)

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
                }
            )
            whenever(resourceProvider.getString(R.string.card_reader_payment_description, DUMMY_ORDER_NUMBER, siteName))
                .thenReturn(expectedResult)
            val stringCaptor = argumentCaptor<String>()

            viewModel.start()

            verify(cardReaderManager).collectPayment(stringCaptor.capture(), any(), any(), any(), any())
            assertThat(stringCaptor.firstValue).isEqualTo(expectedResult)
        }

    @Test
    fun `when initializing payment, then ui updated to initializing payment state `() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(InitializingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
        }

    @Test
    fun `when collecting payment, then ui updated to collecting payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(CollectPaymentState::class.java)
        }

    @Test
    fun `when processing payment, then ui updated to processing payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ProcessingPaymentState::class.java)
        }

    @Test
    fun `when capturing payment, then ui updated to capturing payment state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(CapturingPaymentState::class.java)
        }

    @Test
    fun `when payment completed, then ui updated to payment successful state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(PaymentSuccessfulState::class.java)
        }

    @Test
    fun `when payment completed, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()

            verify(tracker).track(AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)
        }

    @Test
    fun `when payment fails, then ui updated to failed state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
        }

    @Test
    fun `when payment fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()

            verify(tracker).track(eq(AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_FAILED), any(), any(), any())
        }

    @Test
    fun `given user clicks on retry, when payment fails and retryData are null, then flow restarted from scratch`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }
            viewModel.start()
            clearInvocations(cardReaderManager)

            (viewModel.viewStateData.value as FailedPaymentState).onPrimaryActionClicked.invoke()
            advanceUntilIdle()

            verify(cardReaderManager).collectPayment(any(), any(), any(), any(), any())
        }

    @Test
    fun `given user clicks on retry, when payment fails, then retryCollectPayment invoked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
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
            val paymentData = mock<PaymentData>()
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentFailed(GENERIC_ERROR, paymentData, "dummy msg")) }
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

        assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isTrue()
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when collecting payment, then correct labels and illustration is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CollectingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_collect_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$$DUMMY_TOTAL")
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when processing payment, then correct labels and illustration is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(ProcessingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_processing_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$$DUMMY_TOTAL")
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel").isNull()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when capturing payment, then correct labels and illustration is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(CapturingPayment) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_capturing_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$$DUMMY_TOTAL")
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.isProgressVisible).describedAs("Progress visibility").isFalse()
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel").isNull()
        }

    @Test
    fun `when payment fails, then correct labels, illustration and button are shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithEmptyDataForRetry) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_payment_failed_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$$DUMMY_TOTAL")
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentFailed(NO_NETWORK, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(R.string.card_reader_payment_failed_no_network_state)
        }

    @Test
    fun `when payment fails with payment declined error, then correct paymentStateLabel is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentFailed(PAYMENT_DECLINED, null, "")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel")
                .isEqualTo(R.string.card_reader_payment_failed_card_declined_state)
        }

    @Test
    fun `when payment fails with server error, then correct paymentStateLabel is shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentFailed(SERVER_ERROR, null, "")) }
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted(receiptUrl)) }
            }

            viewModel.start()

            verify(appPrefsWrapper).setReceiptUrl(any(), any(), any(), any(), eq(receiptUrl))
        }

    @Test
    fun `when payment succeeds, then correct labels, illustration and buttons are shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }

            viewModel.start()
            val viewState = viewModel.viewStateData.value!!

            assertThat(viewState.headerLabel).describedAs("headerLabel")
                .isEqualTo(R.string.card_reader_payment_completed_payment_header)
            assertThat(viewState.amountWithCurrencyLabel).describedAs("amountWithCurrencyLabel")
                .isEqualTo("$$DUMMY_TOTAL")
            assertThat(viewState.illustration).describedAs("illustration").isEqualTo(R.drawable.img_celebration)
            assertThat(viewState.paymentStateLabel).describedAs("paymentStateLabel").isNull()
            assertThat(viewState.paymentStateLabelTopMargin).describedAs("paymentStateLabelTopMargin")
                .isEqualTo(R.dimen.major_275)
            assertThat(viewState.hintLabel).describedAs("hintLabel").isNull()
            assertThat(viewState.primaryActionLabel).describedAs("primaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_print_receipt)
            assertThat(viewState.secondaryActionLabel).describedAs("secondaryActionLabel")
                .isEqualTo(R.string.card_reader_payment_send_receipt)
        }

    @Test
    fun `given payment flow already started, when start() is invoked, then flow is not restarted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow<CardPaymentStatus> {}
            }

            viewModel.start()
            viewModel.start()
            viewModel.start()
            viewModel.start()

            verify(cardReaderManager, times(1))
                .collectPayment(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        }

    @Test
    fun `when user clicks on print receipt button, then PrintReceipt event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(PrintReceipt::class.java)
        }

    @Test
    fun `when user clicks on print receipt button, then printing receipt state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()
            (viewModel.viewStateData.value as PaymentSuccessfulState).onPrimaryActionClicked.invoke()

            viewModel.onPrintResult(mock())

            assertThat(viewModel.viewStateData.value).isInstanceOf(PaymentSuccessfulState::class.java)
        }

    @Test
    fun `given in printing receipt state, when view recreated, then PrintReceipt event emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(SendReceipt::class.java)
        }

    @Test
    fun `when user clicks on send receipt button, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(PaymentCompleted("")) }
            }
            viewModel.start()

            (viewModel.viewStateData.value as PaymentSuccessfulState).onSecondaryActionClicked.invoke()

            verify(tracker).track(RECEIPT_EMAIL_TAPPED)
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

            viewModel.reFetchOrder()

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
            whenever(orderRepository.fetchOrder(any(), any())).thenReturn(null)
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
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any())).thenAnswer {
                flow { emit(paymentFailedWithValidDataForRetry) }
            }
            viewModel.start()

            viewModel.onCleared()

            verify(cardReaderManager).cancelPayment(any())
        }

    @Test
    fun `given user leaves the screen, when payment succeeded on retry, then payment NOT canceled`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(cardReaderManager.collectPayment(any(), any(), any(), any(), any()))
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

    private fun simulateFetchOrderJobState(inProgress: Boolean) {
        val job = mock<Job>()
        whenever(job.isActive).thenReturn(inProgress)
        viewModel.refetchOrderJob = job
    }
}
