package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreationViewModelInitializationTest : BaseUnitTest() {
    val orderDetailRepository: OrderDetailRepository = mock()
    val viewState = ViewState()
    val createOrUpdateOrderDraft: CreateOrUpdateOrderDraft = mock {
        onBlocking { invoke(any(), any()) } doReturn flowOf(OrderDraftUpdateStatus.Succeeded(Order.EMPTY))
    }

    lateinit var savedState: SavedStateHandle
    lateinit var sut: OrderCreationViewModel

    @Test
    fun `should not load order from repository when is in creation mode`() {
        withInitialState(Mode.Creation)

        initializeSut()

        verifyNoInteractions(orderDetailRepository)
    }

    @Test
    fun `should load order from repository when is in edit mode`() = testBlocking {
        val id = 123L
        val orderToEdit = Order.EMPTY.copy(id = id)
        whenever(orderDetailRepository.getOrderById(id)).doReturn(orderToEdit)
        withInitialState(Mode.Edit(id))

        initializeSut()

        verify(orderDetailRepository).getOrderById(id)
        assertThat(sut.orderDraft.value).isEqualTo(orderToEdit)
    }

    private fun withInitialState(mode: Mode) {
        savedState = spy(OrderCreationFormFragmentArgs(mode).toSavedStateHandle()) {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on { getLiveData(eq(Order.EMPTY.javaClass.name), any<Order>()) } doReturn MutableLiveData(Order.EMPTY)
        }
    }

    private fun initializeSut() {
        val parameterRepository: ParameterRepository = mock {
            on { getParameters(any(), eq(savedState)) } doReturn
                SiteParameters("", null, null, null, null, 0F)
        }
        sut = OrderCreationViewModel(
            savedState,
            mock(),
            orderDetailRepository,
            mock(),
            mock(),
            createOrUpdateOrderDraft,
            mock(),
            parameterRepository,
        )
        sut.orderDraft.observeForever { }
    }
}
