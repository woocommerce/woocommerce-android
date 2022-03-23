package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class OrderCreationViewModelTest: BaseUnitTest() {
    private lateinit var sut: OrderCreationViewModel
    private lateinit var viewState: ViewState
    private lateinit var savedState: SavedStateHandle
    private lateinit var createOrUpdateOrderUseCase: CreateOrUpdateOrderDraft
    private lateinit var createOrderItemUseCase: CreateOrderItem
    private lateinit var parameterRepository: ParameterRepository

    @Before
    fun setUp() {
        viewState = ViewState()
        savedState = mock {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on { getLiveData(Order.EMPTY.javaClass.name, Order.EMPTY) } doReturn MutableLiveData(Order.EMPTY)
        }
        createOrUpdateOrderUseCase = mock()
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn createOrderItem()
        }
        parameterRepository = mock {
            on { getParameters("parameters_key", savedState) } doReturn
                SiteParameters(
                    currencyCode = "",
                    currencySymbol = null,
                    currencyFormattingParameters = null,
                    weightUnit = null,
                    dimensionUnit = null,
                    gmtOffset = 0F
                )
        }
        sut = OrderCreationViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = mock(),
            orderCreationRepository = mock(),
            mapItemToProductUiModel = mock(),
            createOrUpdateOrderDraft = createOrUpdateOrderUseCase,
            createOrderItem = createOrderItemUseCase,
            parameterRepository = parameterRepository
        )
    }

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {
    }

    @Test
    fun `when decreasing product quantity to zero, then call the full product view`() = runBlockingTest {
        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onDecreaseProductsQuantity(123)

        assertThat(lastReceivedEvent).isInstanceOf(ShowProductDetails::class.java)
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

    private fun createOrderItem(withId: Long = 123) = Order.Item.EMPTY.copy(productId = withId)
}
