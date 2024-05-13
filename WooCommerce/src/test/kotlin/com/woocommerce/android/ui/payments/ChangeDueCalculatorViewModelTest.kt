import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var orderDetailRepository: OrderDetailRepository

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ChangeDueCalculatorViewModel

    // TODO: @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        whenever(savedStateHandle.get<Long>("orderId")).thenReturn(1L)
        viewModel = ChangeDueCalculatorViewModel(savedStateHandle, orderDetailRepository)
    }

    // TODO: @Test
    fun `order details load successfully emits success state`() = runBlockingTest {
        // TODO: Fix this test
    }

    // TODO: @Test
    fun `order details load failure emits error state`() = runBlockingTest {
        whenever(orderDetailRepository.getOrderById(1L)).thenReturn(null)

        viewModel.loadOrderDetails()

        assert(viewModel.uiState.value is UiState.Error)
    }
}