package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DiscardSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.EditSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.UseSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ShippingLabelAddressSuggestionViewModelTest : BaseUnitTest() {
    private val enteredAddress = CreateShippingLabelTestUtils.generateAddress()
    private val suggestedAddress = enteredAddress.copy(company = "McDonald's")

    private val initialViewState = ViewState(
        enteredAddress,
        suggestedAddress,
        null,
        R.string.orderdetail_shipping_label_item_shipfrom
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ShippingLabelAddressSuggestionFragmentArgs(enteredAddress, suggestedAddress, ORIGIN)
        )
    )

    private lateinit var viewModel: ShippingLabelAddressSuggestionViewModel

    @Before
    fun setup() {
        viewModel = spy(ShippingLabelAddressSuggestionViewModel(savedState, coroutinesTestRule.testDispatchers))

        clearInvocations(viewModel, savedState)
    }

    @Test
    fun `Displays entered and suggested address correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState).isEqualTo(initialViewState)
    }

    @Test
    fun `Updates the selected address`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState?.areButtonsEnabled).isFalse()

        viewModel.onSelectedAddressChanged(false)
        assertThat(viewState).isEqualTo(initialViewState.copy(selectedAddress = enteredAddress))

        viewModel.onSelectedAddressChanged(true)
        assertThat(viewState).isEqualTo(initialViewState.copy(selectedAddress = suggestedAddress))

        assertThat(viewState?.areButtonsEnabled).isTrue()
    }

    @Test
    fun `Triggers the edit address event`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onSelectedAddressChanged(false)
        viewModel.onEditSelectedAddressTapped()

        assertThat(event).isEqualTo(EditSelectedAddress(enteredAddress))
    }

    @Test
    fun `Triggers the use address event`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onSelectedAddressChanged(true)
        viewModel.onUseSelectedAddressTapped()

        assertThat(event).isEqualTo(UseSelectedAddress(suggestedAddress))
    }

    @Test
    fun `Exits the screen with a discard event`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onExit()
        assertThat(event).isEqualTo(DiscardSuggestedAddress)
    }
}
