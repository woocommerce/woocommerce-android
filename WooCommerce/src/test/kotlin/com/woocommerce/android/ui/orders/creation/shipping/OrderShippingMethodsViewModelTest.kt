package com.woocommerce.android.ui.orders.creation.shipping


import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderShippingMethodsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: OrderShippingMethodsViewModel
    private val noSelectedArgs
        get() = OrderShippingMethodsFragmentArgs(null)

    private val selectedArgs
        get() = OrderShippingMethodsFragmentArgs("other")

    fun setup(args: OrderShippingMethodsFragmentArgs) {
        viewModel = OrderShippingMethodsViewModel(
            savedStateHandle = args.toSavedStateHandle()
        )
    }

    @Test
    fun `given there is no shipping method selected, make sure selection is empty`() = testBlocking {
        setup(noSelectedArgs)
        advanceTimeBy(1001)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val selectedItems = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
            .methods.filter { it.isSelected }
        assertThat(selectedItems.size).isEqualTo(0)
    }
    @Test
    fun `given there is a shipping method selected, make sure the item is marked as selected`() = testBlocking {
        setup(selectedArgs)
        advanceTimeBy(1001)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val selectedItems = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
            .methods.filter { it.isSelected }
        assertThat(selectedItems.size).isEqualTo(1)
    }

    @Test
    fun `given there is no shipping method selected, if the selection changes, the the item is marked as selected`() = testBlocking {
        setup(noSelectedArgs)
        val selected = OrderShippingMethodsViewModel.ShippingMethodUI(
            ShippingMethod("other","Other"),
            isSelected = false
        )

        advanceTimeBy(1001)
        viewModel.onMethodSelected(selected)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val selectedItem = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
            .methods.firstOrNull { it.method.id ==  selected.method.id && it.isSelected}
        assertThat(selectedItem).isNotNull
    }
}
