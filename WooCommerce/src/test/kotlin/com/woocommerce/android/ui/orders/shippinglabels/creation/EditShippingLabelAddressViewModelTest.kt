package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R.string
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressViewModel.Field
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.*
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
    private val adjustedAddress = address.copy(
        firstName = "${address.firstName} ${address.lastName}",
        lastName = "",
        email = ""
    )

    private var validationResult: ValidationResult = ValidationResult.Valid
    private var isPhoneRequired = false
    private var addressType = ORIGIN

    private val countries = listOf(
        WCLocationModel().also {
            it.name = "Virgin Islands (US)"
            it.code = "VI"
        },
        WCLocationModel().also {
            it.name = "USA"
            it.code = "US"
        },
        WCLocationModel().also {
            it.name = "Puerto Rico"
            it.code = "PR"
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

    private val savedState
        get() = EditShippingLabelAddressFragmentArgs(
            address = address,
            addressType = addressType,
            validationResult = validationResult,
            requiresPhoneNumber = isPhoneRequired
        ).initSavedStateHandle()

    private lateinit var viewModel: EditShippingLabelAddressViewModel

    @Before
    fun setup() {
        whenever(dataStore.getCountries()).thenReturn(countries)
        whenever(dataStore.getStates("US")).thenReturn(states)
        whenever(dataStore.getStates("VI")).thenReturn(emptyList())
        val resourceProviderAnswer = { i: InvocationOnMock -> i.arguments[0].toString() }
        whenever(resourceProvider.getString(any())).thenAnswer(resourceProviderAnswer)
        whenever(resourceProvider.getString(any(), anyVararg())).thenAnswer(resourceProviderAnswer)

        createViewModel()
    }

    private fun createViewModel() {
        viewModel = EditShippingLabelAddressViewModel(
            savedState,
            addressValidator,
            resourceProvider,
            dataStore,
            site
        )
    }

    @Test
    fun `when the screen loads, then prefill the fields with the passed address`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState?.nameField?.content).isEqualTo("${address.firstName} ${address.lastName}")
        assertThat(viewState?.companyField?.content).isEqualTo(address.company)
        assertThat(viewState?.phoneField?.content).isEqualTo(address.phone)
        assertThat(viewState?.address1Field?.content).isEqualTo(address.address1)
        assertThat(viewState?.address2Field?.content).isEqualTo(address.address2)
        assertThat(viewState?.cityField?.content).isEqualTo(address.city)
        assertThat(viewState?.stateField?.location).isEqualTo(address.state.asLocation())
        assertThat(viewState?.countryField?.location).isEqualTo(address.country)
    }

    @Test
    fun `given the origin address is invalid, when the screen loads, then display an error`() = testBlocking {
        validationResult = ValidationResult.Invalid("House number is missing")

        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState?.bannerMessage)
            .isEqualTo(string.shipping_label_edit_origin_address_error_warning.toString())
        assertThat(viewState?.address1Field?.error)
            .isEqualTo(UiStringRes(string.shipping_label_error_address_house_number_missing))
    }

    @Test
    fun `given the destination address is invalid, when the screen loads, then display an error`() = testBlocking {
        addressType = DESTINATION
        validationResult = ValidationResult.Invalid("House number is missing")

        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState?.bannerMessage).isEqualTo(string.shipping_label_edit_address_error_warning.toString())
        assertThat(viewState?.address1Field?.error)
            .isEqualTo(UiStringRes(string.shipping_label_error_address_house_number_missing))
    }

    @Test
    fun `given the address is not found, when the screen loads, then display an error`() = testBlocking {
        validationResult = ValidationResult.NotFound("Address not found")

        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState?.bannerMessage)
            .isEqualTo(string.shipping_label_validation_error_template.toString())
    }

    @Test
    fun `when validation fails, then display a snackbar`() = testBlocking {
        whenever(addressValidator.validateAddress(any(), any(), any()))
            .thenReturn(ValidationResult.Error(GENERIC_ERROR))

        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onDoneButtonClicked()

        assertThat(event).isEqualTo(ShowSnackbar(string.shipping_label_edit_address_validation_error))
    }

    @Test
    fun `when the address has an invalid stree, then display an error`() = testBlocking {
        whenever(addressValidator.validateAddress(any(), any(), any()))
            .thenReturn(ValidationResult.Invalid("Street is invalid"))

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onDoneButtonClicked()

        assertThat(viewState?.address1Field?.error)
            .isEqualTo(UiStringRes(string.shipping_label_error_address_invalid_street))
    }

    @Test
    fun `when use as is is clicked, then skip address remote validation`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onUseAddressAsIsButtonClicked()

        verify(addressValidator, never()).validateAddress(any(), any(), any())
        assertThat(event).isEqualTo(ExitWithResult(adjustedAddress))
    }

    @Test
    fun `when contact customer tapped, then dial phone number`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onContactCustomerTapped()

        assertThat(event).isEqualTo(DialPhoneNumber(address.phone))
    }

    @Test
    fun `when open map is tapped, then trigger event to open it`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onOpenMapTapped()

        assertThat(event).isEqualTo(OpenMapWithAddress(adjustedAddress))
    }

    @Test
    fun `given address is valid, when done is clicked, then return it`() = testBlocking {
        whenever(addressValidator.validateAddress(any(), any(), any())).thenReturn(ValidationResult.Valid)

        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onDoneButtonClicked()

        verify(addressValidator, atLeastOnce()).validateAddress(any(), any(), any())

        assertThat(event).isEqualTo(ExitWithResult(adjustedAddress))
    }

    @Test
    fun `given there are non trivial suggestions to the address, when done is clicked, then display them`() =
        testBlocking {
            val suggestedAddress = address.copy(address1 = "Suggested street")
            whenever(addressValidator.validateAddress(any(), any(), any()))
                .thenReturn(ValidationResult.SuggestedChanges(suggestedAddress, isTrivial = false))

            var event: Event? = null
            viewModel.event.observeForever { event = it }

            viewModel.onDoneButtonClicked()

            verify(addressValidator, atLeastOnce()).validateAddress(any(), any(), any())

            assertThat(event).isEqualTo(ShowSuggestedAddress(adjustedAddress, suggestedAddress, ORIGIN))
        }

    @Test
    fun `given there are trivial suggestions to the address, when done is clicked, then use them directly`() =
        testBlocking {
            val suggestedAddress = address.copy(address1 = "Suggested street")
            whenever(addressValidator.validateAddress(any(), any(), any()))
                .thenReturn(ValidationResult.SuggestedChanges(suggestedAddress, isTrivial = true))

            var event: Event? = null
            viewModel.event.observeForever { event = it }

            viewModel.onDoneButtonClicked()

            verify(addressValidator, atLeastOnce()).validateAddress(any(), any(), any())

            assertThat(event).isEqualTo(ExitWithResult(suggestedAddress))
        }

    @Test
    fun `when country spinner is tapped, then display country selector`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onCountrySpinnerTapped()

        assertThat(event).isEqualTo(ShowCountrySelector(countries.map { it.toAppModel() }, address.country.code))
    }

    @Test
    fun `when state spinner is tapped, then display state selector`() = testBlocking {
        var event: Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onStateSpinnerTapped()

        assertThat(event).isEqualTo(ShowStateSelector(states.map { it.toAppModel() }, address.state.codeOrRaw))
    }

    @Test
    fun `when country doesn't have states, then hide state spinner`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onCountrySelected("VI")

        assertThat(viewState?.isStateFieldSpinner).isEqualTo(false)
    }

    @Test
    fun `when state is changed, then display its correct name`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onFieldEdited(Field.State, "NY")

        assertThat(viewState?.stateField?.content).isEqualTo("New York")
    }

    @Test
    fun `when name and company are missing, then display an error`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onFieldEdited(Field.Name, "")
        viewModel.onFieldEdited(Field.Company, "")

        assertThat(viewState?.nameField?.error).isEqualTo(UiStringRes(string.error_required_field))
    }

    @Test
    fun `given country has states, when state is missing, then display an error`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onCountrySelected("US")
        viewModel.onFieldEdited(Field.State, "")

        assertThat(viewState?.stateField?.error).isEqualTo(UiStringRes(string.error_required_field))
    }

    @Test
    fun `given phone is required, when it is missing, then display an error`() = testBlocking {
        isPhoneRequired = true
        createViewModel()

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onFieldEdited(Field.Phone, "")

        assertThat(viewState?.phoneField?.error).isEqualTo(UiStringRes(string.shipping_label_address_phone_required))
    }

    @Test
    fun `given there is an address1 remote error, when use address as entered is clicked, then skip the error`() =
        testBlocking {
            validationResult = ValidationResult.Invalid("House number is missing")
            createViewModel()

            viewModel.onUseAddressAsIsButtonClicked()

            val viewState = viewModel.viewStateData.liveData.value!!
            assertThat(viewState.areAllRequiredFieldsValid).isTrue()
        }
}
