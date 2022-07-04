package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreationShippingViewModel.RemoveShipping
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreationShippingViewModel.UpdateShipping
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class OrderCreationShippingViewModelTest : BaseUnitTest() {
    lateinit var viewModel: OrderCreationShippingViewModel

    private val creationArgs
        get() = OrderCreationShippingFragmentArgs(null)

    private val editArgs
        get() = OrderCreationShippingFragmentArgs(
            ShippingLine(
                methodId = "other",
                total = BigDecimal.TEN,
                methodTitle = "Shipping"
            )
        )

    fun setup(args: OrderCreationShippingFragmentArgs) {
        viewModel = OrderCreationShippingViewModel(
            savedStateHandle = args.toSavedStateHandle()
        )
    }

    @Test
    fun `given this is creation flow, when the screen loads, make sure data is empty`() {
        setup(creationArgs)

        val viewState = viewModel.viewStateData.liveData.getOrAwaitValue()
        assertThat(viewState.name).isNull()
        assertThat(viewState.amount).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(viewState.isEditFlow).isFalse
    }

    @Test
    fun `given this is edit flow, when the screen loads, make sure data matches the current shipping line`() {
        setup(editArgs)

        val viewState = viewModel.viewStateData.liveData.getOrAwaitValue()
        assertThat(viewState.name).isEqualTo(editArgs.currentShippingLine?.methodTitle)
        assertThat(viewState.amount).isEqualByComparingTo(editArgs.currentShippingLine?.total)
        assertThat(viewState.isEditFlow).isTrue
    }

    @Test
    fun `when editing the amount, then update the state`() {
        setup(creationArgs)

        viewModel.onAmountEdited(BigDecimal.TEN)

        val viewState = viewModel.viewStateData.liveData.getOrAwaitValue()
        assertThat(viewState.amount).isEqualByComparingTo(BigDecimal.TEN)
    }

    @Test
    fun `when editing name, then update the state`() {
        setup(creationArgs)
        val name = "shipping name"

        viewModel.onNameEdited(name)

        val viewState = viewModel.viewStateData.liveData.getOrAwaitValue()
        assertThat(viewState.name).isEqualTo(name)
    }

    @Test
    fun `when done button is clicked, then update shipping line data`() {
        setup(creationArgs)
        val amount = BigDecimal.TEN
        val name = "Shipping name"

        viewModel.onAmountEdited(amount)
        viewModel.onNameEdited(name)
        viewModel.onDoneButtonClicked()

        val lastEvent = viewModel.event.getOrAwaitValue()
        assertThat(lastEvent).isInstanceOf(UpdateShipping::class.java)
        (lastEvent as UpdateShipping).let {
            assertThat(it.name).isEqualTo(name)
            assertThat(it.amount).isEqualByComparingTo(amount)
        }
    }

    @Test
    fun `when clicking on remove, then remove the shipping line`() {
        setup(editArgs)

        viewModel.onRemoveShippingClicked()

        val lastEvent = viewModel.event.getOrAwaitValue()
        assertThat(lastEvent).isInstanceOf(RemoveShipping::class.java)
    }
}
