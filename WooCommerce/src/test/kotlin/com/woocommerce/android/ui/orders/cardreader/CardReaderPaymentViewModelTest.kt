package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
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
        whenever(mockedOrder.total).thenReturn("10.12")
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
            flow { emit(CollectingPaymentFailed) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when processing payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(ProcessingPaymentFailed) }
        }

        viewModel.start(cardReaderManager)

        assertThat(viewModel.viewStateData.value).isInstanceOf(FailedPaymentState::class.java)
    }

    @Test
    fun `when capturing payment fails, then ui updated to failed state`() = runBlockingTest {
        whenever(cardReaderManager.collectPayment(any(), anyString())).thenAnswer {
            flow { emit(CapturingPaymentFailed) }
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

    // TODO cardreader add tests for ViewState fields when they are final
}
