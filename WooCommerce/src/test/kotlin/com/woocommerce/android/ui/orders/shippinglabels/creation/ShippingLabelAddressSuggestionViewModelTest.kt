package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.EditSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.UseSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ShippingLabelAddressSuggestionViewModelTest : BaseUnitTest() {
    private val enteredAddress = CreateShippingLabelTestUtils.generateAddress()
    private val suggestedAddress = enteredAddress.copy(company = "McDonald's")

    private val initialViewState = ViewState(
        enteredAddress,
        suggestedAddress,
        suggestedAddress,
        R.string.orderdetail_shipping_label_item_shipfrom
    )

    private val savedState = ShippingLabelAddressSuggestionFragmentArgs(enteredAddress, suggestedAddress, ORIGIN)
        .initSavedStateHandle()

    private lateinit var viewModel: ShippingLabelAddressSuggestionViewModel

    @Before
    fun setup() {
        viewModel = ShippingLabelAddressSuggestionViewModel(savedState)
    }

    @Test
    fun `Displays entered and suggested address correctly`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState).isEqualTo(initialViewState)
    }

    @Test
    fun `Updates the selected address`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onSelectedAddressChanged(false)
        assertThat(viewState).isEqualTo(initialViewState.copy(selectedAddress = enteredAddress))

        viewModel.onSelectedAddressChanged(true)
        assertThat(viewState).isEqualTo(initialViewState.copy(selectedAddress = suggestedAddress))

        assertThat(viewState?.areButtonsEnabled).isTrue()
    }

    @Test
    fun `Triggers the edit address event`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onSelectedAddressChanged(false)
        viewModel.onEditSelectedAddressTapped()

        assertThat(event).isEqualTo(EditSelectedAddress(enteredAddress))
    }

    @Test
    fun `Triggers the use address event`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onSelectedAddressChanged(true)
        viewModel.onUseSelectedAddressTapped()

        assertThat(event).isEqualTo(UseSelectedAddress(suggestedAddress))
    }

    @Test
    fun `Exits the screen with a exit event`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onExit()
        assertThat(event).isEqualTo(Exit)
    }
}
