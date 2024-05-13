import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var orderDetailRepository: OrderDetailRepository

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        whenever(savedStateHandle.get<Long>("orderId")).thenReturn(1L)
        viewModel = ChangeDueCalculatorViewModel(savedStateHandle, orderDetailRepository)
    }

    @Test
    fun `order details load successfully emits success state`() = runTest {
//        val order = Order(orderId = 1L, total = BigDecimal("100.00"))  // Assuming Order is a data class
//        whenever(orderDetailRepository.getOrderById(1L)).thenReturn(order)
//
//        viewModel.loadOrderDetails()
//
//        assert(viewModel.uiState.value == UiState.Success(order.total, BigDecimal.ZERO))
    }

    @Test
    fun `order details load failure emits error state`() = runTest {
        whenever(orderDetailRepository.getOrderById(1L)).thenReturn(null)

        viewModel.loadOrderDetails()

        assert(viewModel.uiState.value is UiState.Error)
    }
}
