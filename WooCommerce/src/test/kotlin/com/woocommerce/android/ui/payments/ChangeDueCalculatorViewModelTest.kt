import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel.UiState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

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
    fun `order details load successfully emits success state`() = testBlocking {
        //TODO fix this test
        whenever(orderDetailRepository.getOrderById(1L)).thenReturn(null)

        viewModel.loadOrderDetails()

        assert(viewModel.uiState.value == UiState.Success(BigDecimal.ZERO, BigDecimal.ZERO))
    }

    @Test
    fun `order details load failure emits error state`() = testBlocking {
        whenever(orderDetailRepository.getOrderById(1L)).thenReturn(null)

        viewModel.loadOrderDetails()

        assert(viewModel.uiState.value is UiState.Error)
    }
}
