package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class OrderCreationViewModelTest: BaseUnitTest() {
    private lateinit var sut: OrderCreationViewModel
    private lateinit var orderDraft: MutableStateFlow<Order>
    private lateinit var savedState: SavedStateHandle
    private lateinit var createOrUpdateOrderUseCase: CreateOrUpdateOrderDraft

    @Before
    fun setUp() {
        orderDraft = mock()
        savedState = mock {
            on { getLiveData(any(), Order.EMPTY) } doReturn mock()
        }
        createOrUpdateOrderUseCase = mock()
        sut = OrderCreationViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = mock(),
            orderCreationRepository = mock(),
            mapItemToProductUiModel = mock(),
            createOrUpdateOrderDraft = createOrUpdateOrderUseCase,
            createOrderItem = mock(),
            parameterRepository = mock()
        )
    }

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {
        verify(createOrUpdateOrderUseCase.invoke(orderDraft, any()))
    }

    @Test
    fun `when decreasing product quantity to zero, then call the full product view`() {

    }

    @Test
    fun `when adding the very same product, then increase item quantity by one`() {

    }

    @Test
    fun `when adding customer address with empty shipping, then set shipping as billing`() {

    }

    @Test
    fun `when creating the order fails, then trigger Snackbar with fail message`() {

    }

    @Test
    fun `when creating the order succeed, then call Order details view`() {

    }

    @Test
    fun `when hitting the back button with changes done, then trigger discard warning dialog`() {

    }

    @Test
    fun `when editing a fee, then reuse the existent one with different value`() {

    }

    @Test
    fun `when editing a shipping fee, then reuse the existent one with different value`() {

    }
}
