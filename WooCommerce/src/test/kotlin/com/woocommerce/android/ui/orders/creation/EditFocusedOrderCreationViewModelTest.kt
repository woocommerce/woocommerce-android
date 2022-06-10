package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.Mode.Edit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
// Remove Silent runner when feature is completed
@RunWith(MockitoJUnitRunner.Silent::class)
class EditFocusedOrderCreationViewModelTest : UnifiedOrderEditViewModelTest() {
    override val mode: Mode = Edit(defaultOrderValue.id)

    @Test
    fun `should load order from repository`() = testBlocking {
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(defaultOrderValue)
        }

        createSut()

        var orderDraft: Order? = null

        sut.orderDraft.observeForever { new ->
            orderDraft = new
        }

        assertThat(orderDraft).isEqualTo(defaultOrderValue)
    }

    @Test
    fun `when hitting the back button with no changes, then trigger Exit with no dialog`() {
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(defaultOrderValue)
        }
        createSut()
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onBackButtonClicked()

        assertThat(lastReceivedEvent).isEqualTo(Exit)
    }

    @Test
    fun `when hitting the back button with changes done, then trigger discard warning dialog`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductSelected(123)

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onBackButtonClicked()

        assertThat(lastReceivedEvent).isInstanceOf(Event.ShowDialog::class.java)
    }

    @Test
    fun `when confirming order edit, then dismiss the screen`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCreateOrderClicked(defaultOrderValue)

        assertThat(lastReceivedEvent).isEqualTo(Exit)
    }

    @Test
    fun `when isEditable is true on the edit flow the order is editable`() {
        createOrUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue.copy(isEditable = true)))
        }
        createSut()
        var lastReceivedState: OrderCreationViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(true)
    }

    @Test
    fun `when isEditable is false on the edit flow the order is NOT editable`() {
        createOrUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue.copy(isEditable = false)))
        }
        createSut()
        var lastReceivedState: OrderCreationViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(false)
    }
}
