package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OrderShippingMethodsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: OrderShippingMethodsViewModel
    private val noSelectedArgs
        get() = OrderShippingMethodsFragmentArgs(null)

    private val selectedArgs
        get() = OrderShippingMethodsFragmentArgs("other")

    private val getShippingMethodsWithOtherValue: GetShippingMethodsWithOtherValue = mock()

    private val refreshShippingMethods: RefreshShippingMethods = mock()

    private val defaultShippingMethods = listOf(
        ShippingMethod(id = "free_shipping", "Free Shipping"),
        ShippingMethod(id = ShippingMethodsRepository.OTHER_ID, "Other"),
        ShippingMethod(id = "local_pickup", "Local Pick Up"),
    )

    fun setup(args: OrderShippingMethodsFragmentArgs) {
        viewModel = OrderShippingMethodsViewModel(
            savedStateHandle = args.toSavedStateHandle(),
            getShippingMethodsWithOtherValue = getShippingMethodsWithOtherValue,
            refreshShippingMethods = refreshShippingMethods
        )
    }

    @Test
    fun `given there is no shipping method selected, make sure selection is empty`() = testBlocking {
        whenever(getShippingMethodsWithOtherValue.invoke()).doReturn(flowOf(defaultShippingMethods))
        setup(noSelectedArgs)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val selectedItems = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
            .methods.filter { it.isSelected }
        assertThat(selectedItems.size).isEqualTo(0)
    }

    @Test
    fun `given there is a shipping method selected, make sure the item is marked as selected`() = testBlocking {
        whenever(getShippingMethodsWithOtherValue.invoke()).doReturn(flowOf(defaultShippingMethods))
        setup(selectedArgs)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val selectedItems = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
            .methods.filter { it.isSelected }
        assertThat(selectedItems.size).isEqualTo(1)
    }

    @Test
    fun `given there is no shipping method selected, if the selection changes, the the item is marked as selected`() =
        testBlocking {
            whenever(getShippingMethodsWithOtherValue.invoke()).doReturn(flowOf(defaultShippingMethods))

            setup(noSelectedArgs)

            val selected = OrderShippingMethodsViewModel.ShippingMethodUI(
                ShippingMethod("other", "Other"),
                isSelected = false
            )
            viewModel.onMethodSelected(selected)
            val viewState = viewModel.viewState.first()
            assertThat(viewState).isNotNull
            assertThat(viewState).isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
            val selectedItem = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState)
                .methods.firstOrNull { it.method.id == selected.method.id && it.isSelected }
            assertThat(selectedItem).isNotNull
        }

    @Test
    fun `given refresh is called and succeed then update view state`() = testBlocking {
        whenever(getShippingMethodsWithOtherValue.invoke()).doReturn(flowOf(defaultShippingMethods))
        whenever(refreshShippingMethods.invoke()).doReturn(Result.success(emptyList()))
        setup(noSelectedArgs)

        viewModel.refresh()
        val viewState = viewModel.viewState.first()

        assertThat(viewState).isNotNull
        assertThat(viewState)
            .isInstanceOf(OrderShippingMethodsViewModel.ViewState.ShippingMethodsState::class.java)
        val isRefreshing = (viewState as OrderShippingMethodsViewModel.ViewState.ShippingMethodsState).isRefreshing
        assertThat(isRefreshing).isTrue()
    }

    @Test
    fun `verify data is refreshed on init`() = testBlocking {
        whenever(getShippingMethodsWithOtherValue.invoke()).doReturn(flowOf(defaultShippingMethods))
        whenever(refreshShippingMethods.invoke()).doReturn(Result.success(emptyList()))
        setup(noSelectedArgs)

        verify(refreshShippingMethods).invoke()
    }
}
