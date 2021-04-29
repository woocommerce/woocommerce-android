package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
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
import com.woocommerce.android.cardreader.CardPaymentStatus.UnexpectedError
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper

private const val DUMMY_TOTAL = "10.12"

@ExperimentalCoroutinesApi
class CardReaderPaymentViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var viewModel: CardReaderPaymentViewModel
    private val loggerWrapper: AppLogWrapper = mock()
    private val orderStore: WCOrderStore = mock()
    private val cardReaderManager: CardReaderManager = mock()

    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            CardReaderPaymentDialogArgs(orderIdentifier = CardReaderPaymentViewModelTest.ORDER_IDENTIFIER)
        )
    )

    @Before
    fun setUp() = runBlockingTest {
        viewModel = CardReaderPaymentViewModel(
            savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            logger = loggerWrapper,
            orderStore = orderStore
        )

        val mockedOrder = mock<WCOrderModel>()
        whenever(mockedOrder.total).thenReturn(DUMMY_TOTAL)
        whenever(mockedOrder.currency).thenReturn("USD")
        whenever(orderStore.getOrderByIdentifier(ORDER_IDENTIFIER)).thenReturn(mockedOrder)
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow<CardPaymentStatus> { }
        }
    }

    @Test
    fun `given Order contains invalid total, when payment screen shown, then FailedPayment state is shown`() {
        val mockedOrder = mock<WCOrderModel>()
        whenever(mockedOrder.total).thenReturn("invalid big decimal")
        whenever(orderStore.getOrderByIdentifier(ORDER_IDENTIFIER)).thenReturn(mockedOrder)

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `given Order not found in database, when payment screen shown, then FailedPayment state is shown`() {
        whenever(orderStore.getOrderByIdentifier(ORDER_IDENTIFIER)).thenReturn(null)

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when payment screen shown, then loading data state is shown`() {
        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
    }

    @Test
    fun `when initializing payment, then ui updated to initializing payment state `() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(InitializingPayment) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(LoadingDataState::class.java)
    }

    @Test
    fun `when collecting payment, then ui updated to collecting payment state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CollectingPayment) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(CollectPaymentState::class.java)
    }

    @Test
    fun `when processing payment, then ui updated to processing payment state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(ProcessingPayment) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(ProcessingPaymentState::class.java)
    }

    @Test
    fun `when capturing payment, then ui updated to capturing payment state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CapturingPayment) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(CapturingPaymentState::class.java)
    }

    @Test
    fun `when payment completed, then ui updated to payment successful state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(PaymentCompleted) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(PaymentSuccessfulState::class.java)
    }

    @Test
    fun `when initializing payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(InitializingPaymentFailed) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when collecting payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CollectingPaymentFailed(mock())) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when processing payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(ProcessingPaymentFailed(mock())) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when capturing payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CapturingPaymentFailed(mock())) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when unexpected error occurs during payment flow, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(UnexpectedError("")) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when loading data, then only progress is visible`() = runBlockingTest {
        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).isTrue()
        assertThat(viewState.headerLabel).isNull()
        assertThat(viewState.amountWithCurrencyLabel).isNull()
        assertThat(viewState.illustration).isNull()
        assertThat(viewState.paymentStateLabel).isNull()
        assertThat(viewState.hintLabel).isNull()
        assertThat(viewState.printReceiptLabel).isNull()
        assertThat(viewState.sendReceiptLabel).isNull()
    }

    @Test
    fun `when collecting payment, then progress and buttons are hidden`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CollectingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).isFalse()
        assertThat(viewState.printReceiptLabel).isNull()
        assertThat(viewState.sendReceiptLabel).isNull()
    }

    @Test
    fun `when collecting payment, then correct labels and illustration is shown`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CollectingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.headerLabel).isEqualTo(R.string.card_reader_payment_collect_payment_header)
        assertThat(viewState.amountWithCurrencyLabel).isEqualTo("$$DUMMY_TOTAL")
        assertThat(viewState.illustration).isEqualTo(R.drawable.ic_card_reader)
        assertThat(viewState.paymentStateLabel).isEqualTo(R.string.card_reader_payment_collect_payment_state)
        assertThat(viewState.hintLabel).isEqualTo(R.string.card_reader_payment_collect_payment_hint)
    }

    @Test
    fun `when processing payment, then progress and buttons are hidden`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(ProcessingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).isFalse()
        assertThat(viewState.printReceiptLabel).isNull()
        assertThat(viewState.sendReceiptLabel).isNull()
    }

    @Test
    fun `when processing payment, then correct labels and illustration is shown`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(ProcessingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.headerLabel).isEqualTo(R.string.card_reader_payment_processing_payment_header)
        assertThat(viewState.amountWithCurrencyLabel).isEqualTo("$$DUMMY_TOTAL")
        assertThat(viewState.illustration).isEqualTo(R.drawable.ic_card_reader)
        assertThat(viewState.paymentStateLabel).isEqualTo(R.string.card_reader_payment_processing_payment_state)
        assertThat(viewState.hintLabel).isEqualTo(R.string.card_reader_payment_processing_payment_hint)
    }

    @Test
    fun `when capturing payment, then progress and buttons are hidden`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CapturingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.isProgressVisible).isFalse()
        assertThat(viewState.printReceiptLabel).isNull()
        assertThat(viewState.sendReceiptLabel).isNull()
    }

    @Test
    fun `when capturing payment, then correct labels and illustration is shown`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CapturingPayment) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.headerLabel).isEqualTo(R.string.card_reader_payment_capturing_payment_header)
        assertThat(viewState.amountWithCurrencyLabel).isEqualTo("$$DUMMY_TOTAL")
        assertThat(viewState.illustration).isEqualTo(R.drawable.ic_card_reader)
        assertThat(viewState.paymentStateLabel).isEqualTo(R.string.card_reader_payment_capturing_payment_state)
        assertThat(viewState.hintLabel).isEqualTo(R.string.card_reader_payment_capturing_payment_hint)
    }

    @Test
    fun `when payment succeeds, then correct labels, illustration and buttons are shown`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(PaymentCompleted) }
        }

        viewModel.start(cardReaderManager)
        val viewState = viewModel.viewStateData.value!!

        assertThat(viewState.headerLabel).isEqualTo(R.string.card_reader_payment_completed_payment_header)
        assertThat(viewState.amountWithCurrencyLabel).isEqualTo("$$DUMMY_TOTAL")
        assertThat(viewState.illustration).isEqualTo(R.drawable.ic_celebration)
        assertThat(viewState.paymentStateLabel).isNull()
        assertThat(viewState.hintLabel).isNull()
        assertThat(viewState.printReceiptLabel).isEqualTo(R.string.card_reader_payment_print_receipt)
        assertThat(viewState.sendReceiptLabel).isEqualTo(R.string.card_reader_payment_send_receipt)
    }

    @Test
    fun `given payment flow already started, when start() is invoked, then flow is not restarted`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer { flow<CardPaymentStatus> {} }

        viewModel.start(cardReaderManager)
        viewModel.start(cardReaderManager)
        viewModel.start(cardReaderManager)
        viewModel.start(cardReaderManager)

        verify(cardReaderManager, times(1)).collectPayment(anyOrNull(), anyString())
    }
}
