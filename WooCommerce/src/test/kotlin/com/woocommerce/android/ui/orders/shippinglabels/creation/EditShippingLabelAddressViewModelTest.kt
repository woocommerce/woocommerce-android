package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DialPhoneNumber
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.OpenMapWithAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowStateSelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCDataStore

@ExperimentalCoroutinesApi
class EditShippingLabelAddressViewModelTest : BaseUnitTest() {
    private val addressValidator = mock<ShippingLabelAddressValidator>()
    private val resourceProvider = mock<ResourceProvider>()
    private val dataStore = mock<WCDataStore>()
    private val site = mock<SelectedSite>()

    private val address = CreateShippingLabelTestUtils.generateAddress()
    private var validationResult: ValidationResult = ValidationResult.Valid

    private val initialViewState = ViewState(
        address = address,
        title = string.orderdetail_shipping_label_item_shipfrom,
        isValidationProgressDialogVisible = false,
        isStateFieldSpinner = true,
        selectedStateName = "Kentucky",
        selectedCountryName = "USA"
    )

    private val countries = listOf(
        WCLocationModel().also {
            it.name = "Slovakia"
            it.code = "SK"
        },
        WCLocationModel().also {
            it.name = "USA"
            it.code = "US"
        },
        WCLocationModel().also {
            it.name = "Canada"
            it.code = "CA"
        }
    )

    private val states = listOf(
        WCLocationModel().also {
            it.name = "New York"
            it.code = "NY"
            it.parentCode = "US"
        },
        WCLocationModel().also {
            it.name = "Kentucky"
            it.code = "KY"
            it.parentCode = "US"
        },
        WCLocationModel().also {
            it.name = "New Jersey"
            it.code = "NJ"
            it.parentCode = "US"
        }
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val savedState
        get() = spy(
            SavedStateWithArgs(
                SavedStateHandle(),
                null,
                EditShippingLabelAddressFragmentArgs(address, ORIGIN, validationResult)
            )
        )

    private lateinit var viewModel: EditShippingLabelAddressViewModel

    @Before
    fun setup() {
        whenever(dataStore.getCountries()).thenReturn(countries)
        whenever(dataStore.getStates("US")).thenReturn(states)
        whenever(dataStore.getStates("SK")).thenReturn(emptyList())
        whenever(resourceProvider.getString(any())).thenAnswer { i -> i.arguments[0].toString() }

        createViewModel()

        clearInvocations(viewModel, savedState, addressValidator, resourceProvider, dataStore, site)
    }

    private fun createViewModel() {
        viewModel = spy(
            EditShippingLabelAddressViewModel(
                savedState,
                coroutinesTestRule.testDispatchers,
                addressValidator,
                resourceProvider,
                dataStore,
                site
            )
        )
    }

    @Test
    fun `Displays entered and suggested address correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState).isEqualTo(initialViewState)
    }

    @Test
    fun `Shows address error for invalid address`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        validationResult = ValidationResult.Invalid("House number is missing")

        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val invalidViewState = initialViewState.copy(
            bannerMessage = string.shipping_label_edit_address_error_warning.toString(),
            addressError = string.shipping_label_error_address_house_number_missing
        )

        assertThat(viewState).isEqualTo(invalidViewState)
    }

    @Test
    fun `Shows address error for address not found`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        validationResult = ValidationResult.NotFound("Address not found")

        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val invalidViewState = initialViewState.copy(
            bannerMessage = string.shipping_label_edit_address_error_warning.toString()
        )

        assertThat(viewState).isEqualTo(invalidViewState)
    }

    @Test
    fun `Shows a snackbar on validation error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        whenever(addressValidator.validateAddress(any(), any())).thenReturn(ValidationResult.Error(GENERIC_ERROR))

        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onDoneButtonClicked(address)

        assertThat(event).isEqualTo(ShowSnackbar(string.shipping_label_edit_address_validation_error))
    }

    @Test
    fun `Shows the right error for an invalid street`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        whenever(addressValidator.validateAddress(any(), any()))
            .thenReturn(ValidationResult.Invalid("Street is invalid"))

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val invalidViewState = initialViewState.copy(
            nameError = 0,
            zipError = 0,
            cityError = 0,
            bannerMessage = "",
            addressError = string.shipping_label_error_address_invalid_street
        )

        viewModel.onDoneButtonClicked(address)

        assertThat(viewState).isEqualTo(invalidViewState)
    }

    @Test
    fun `Returns unvalidated address when tapped on use as is`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onUseAddressAsIsButtonClicked()

        verify(addressValidator, never()).validateAddress(any(), any())

        assertThat(event).isEqualTo(ExitWithResult(address))
    }

    @Test
    fun `Dial phone number event triggered on contact customer tapped`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onContactCustomerTapped()

        assertThat(event).isEqualTo(DialPhoneNumber(address.phone))
    }

    @Test
    fun `Open map event triggered on contact customer tapped`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onOpenMapTapped()

        assertThat(event).isEqualTo(OpenMapWithAddress(address))
    }

    @Test
    fun `Address validated and returned if valid`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        whenever(addressValidator.validateAddress(any(), any())).thenReturn(ValidationResult.Valid)

        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onDoneButtonClicked(address)

        verify(addressValidator, atLeastOnce()).validateAddress(any(), any())

        assertThat(event).isEqualTo(ExitWithResult(address))
    }

    @Test
    fun `Address valid but changes suggested`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val suggestedAddress = address.copy(address1 = "Suggested street")
        whenever(addressValidator.validateAddress(any(), any()))
            .thenReturn(ValidationResult.SuggestedChanges(suggestedAddress))

        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onDoneButtonClicked(address)

        verify(addressValidator, atLeastOnce()).validateAddress(any(), any())

        assertThat(event).isEqualTo(ShowSuggestedAddress(address, suggestedAddress, ORIGIN))
    }

    @Test
    fun `Show the selector when spinner tapped`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onCountrySpinnerTapped()

        assertThat(event).isEqualTo(ShowCountrySelector(countries, initialViewState.address?.country))

        viewModel.onStateSpinnerTapped()

        assertThat(event).isEqualTo(ShowStateSelector(states, initialViewState.address?.state))
    }

    @Test
    fun `Shows the errors for the missing required values`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val emptyAddress = address.copy(
            firstName = "",
            lastName = "",
            address1 = "",
            city = "",
            postcode = ""
        )

        val invalidViewState = initialViewState.copy(
            nameError = string.shipping_label_error_required_field,
            zipError = string.shipping_label_error_required_field,
            cityError = string.shipping_label_error_required_field,
            addressError = string.shipping_label_error_required_field,
            address = emptyAddress
        )

        viewModel.updateAddress(emptyAddress)
        viewModel.onUseAddressAsIsButtonClicked()

        assertThat(viewState).isEqualTo(invalidViewState)
    }

    @Test
    fun `Hides the spinner for a country without states`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onCountrySelected("SK")

        assertThat(viewState).isEqualTo(initialViewState.copy(
            address.copy(country = "SK"),
            selectedCountryName = "Slovakia",
            selectedStateName = "",
            isStateFieldSpinner = false
        ))
    }

    @Test
    fun `Shows the correct state name when state changed`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onStateSelected("NY")

        assertThat(viewState).isEqualTo(initialViewState.copy(
            address.copy(state = "NY"),
            selectedCountryName = "USA",
            selectedStateName = "New York"
        ))
    }
}
