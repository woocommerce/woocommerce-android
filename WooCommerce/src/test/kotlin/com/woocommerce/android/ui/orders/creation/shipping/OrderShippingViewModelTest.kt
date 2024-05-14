package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class OrderShippingViewModelTest : BaseUnitTest() {
    lateinit var viewModel: OrderShippingViewModel

    private val creationArgs
        get() = OrderShippingFragmentArgs(null)

    private val editArgs
        get() = OrderShippingFragmentArgs(
            Order.ShippingLine(
                methodId = "other",
                total = BigDecimal.TEN,
                methodTitle = "Shipping"
            )
        )

    private val getShippingMethodById: GetShippingMethodById = mock()

    private val tracker: AnalyticsTrackerWrapper = mock()

    fun setup(args: OrderShippingFragmentArgs) {
        viewModel = OrderShippingViewModel(
            savedStateHandle = args.toSavedStateHandle(),
            resourceProvider = mock(),
            getShippingMethodById = getShippingMethodById,
            tracker = tracker
        )
    }

    @Test
    fun `given this is creation flow, when the screen loads, make sure data is empty`() = testBlocking {
        setup(creationArgs)
        val viewState = viewModel.viewState.first()
        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingViewModel.ViewState.ShippingState::class.java)
        assertThat((viewState as OrderShippingViewModel.ViewState.ShippingState).name).isNull()
        assertThat(viewState.method).isNull()
        assertThat(viewState.amount).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(viewState.isEditFlow).isFalse
    }

    @Test
    fun `given this is edit flow, when the screen loads, make sure data matches the current shipping line`() = testBlocking {
        setup(editArgs)

        advanceTimeBy(1001)
        // When the screen load and
        val viewState = viewModel.viewState.value

        assertThat(viewState).isNotNull
        assertThat(viewState).isInstanceOf(OrderShippingViewModel.ViewState.ShippingState::class.java)
        assertThat((viewState as OrderShippingViewModel.ViewState.ShippingState).name)
            .isEqualTo(editArgs.currentShippingLine?.methodTitle)
        assertThat(viewState.amount).isEqualByComparingTo(editArgs.currentShippingLine?.total)
        assertThat(viewState.isEditFlow).isTrue
    }

    @Test
    fun `when editing the amount, then update the state`() {
        setup(creationArgs)
        viewModel.onAmountChanged(BigDecimal.TEN)

        val viewState = viewModel.viewState.value
        assertThat((viewState as OrderShippingViewModel.ViewState.ShippingState).amount)
            .isEqualByComparingTo(BigDecimal.TEN)
    }

    @Test
    fun `when editing name, then update the state`() {
        setup(creationArgs)
        val name = "shipping name"

        viewModel.onNameChanged(name)

        val viewState = viewModel.viewState.value
        assertThat((viewState as OrderShippingViewModel.ViewState.ShippingState).name)
            .isEqualTo(name)
    }

    @Test
    fun `when editing method, then update the state`() {
        setup(creationArgs)
        val selected = ShippingMethod(
            id = "other",
            title = "Other"
        )

        viewModel.onMethodSelected(selected)

        val viewState = viewModel.viewState.value
        assertThat((viewState as OrderShippingViewModel.ViewState.ShippingState).method)
            .isEqualTo(selected)
    }

    @Test
    fun `when editing method, then track the method selected`() {
        setup(creationArgs)
        val selected = ShippingMethod(
            id = "other",
            title = "Other"
        )

        viewModel.onMethodSelected(selected)

        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_SELECTED,
            mapOf(AnalyticsTracker.KEY_SHIPPING_METHOD to selected.id)
        )
    }

    @Test
    fun `when done button is clicked, then update shipping line data`() {
        setup(creationArgs)
        val amount = BigDecimal.TEN
        val name = "Shipping name"

        viewModel.onAmountChanged(amount)
        viewModel.onNameChanged(name)
        viewModel.onSaveChanges()

        val lastEvent = viewModel.event.getOrAwaitValue()
        assertThat(lastEvent).isInstanceOf(UpdateShipping::class.java)
        (lastEvent as UpdateShipping).let {
            assertThat(it.shippingUpdate.name).isEqualTo(name)
            assertThat(it.shippingUpdate.amount).isEqualByComparingTo(amount)
        }
    }

    @Test
    fun `when clicking on remove, then remove the shipping line`() {
        setup(editArgs)
        viewModel.onRemove()

        val lastEvent = viewModel.event.getOrAwaitValue()
        assertThat(lastEvent).isInstanceOf(RemoveShipping::class.java)
        assertThat((lastEvent as RemoveShipping).id).isEqualTo(editArgs.currentShippingLine?.itemId)
    }
}
