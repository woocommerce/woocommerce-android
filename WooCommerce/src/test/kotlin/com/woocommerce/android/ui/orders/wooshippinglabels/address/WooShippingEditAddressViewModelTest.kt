package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.UpdateDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.GetAcceptedOriginCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.UpdateOriginAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
abstract class WooShippingEditAddressViewModelTest : BaseUnitTest() {
    protected val addressValidator: AddressValidationHelper = mock()
    protected val getAcceptedOriginCountries: GetAcceptedOriginCountries = mock()
    protected val getStatesByCountryCode: GetStatesByCountryCode = mock()
    protected val resourceProvider: ResourceProvider = mock()
    protected val normalizeAddress: NormalizeAddress = mock()
    protected val updateOriginAddress: UpdateOriginAddress = mock()
    protected val updateDestinationAddress: UpdateDestinationAddress = mock()

    protected val countries = listOf(
        Location("US", "United States"),
        Location("UK", "United Kingdom"),
        Location("AR", "Argentina"),
        Location("BR", "Brazil")
    )

    protected val states = listOf(
        Location("FL", "Florida"),
        Location("CA", "California"),
    )

    protected lateinit var sut: WooShippingEditAddressViewModel

    abstract fun createSavedStateHandle(address: Address, isVerified: Boolean = false): SavedStateHandle

    fun createViewModel(savedState: SavedStateHandle) {
        sut = WooShippingEditAddressViewModel(
            addressValidator = addressValidator,
            savedState = savedState,
            getAcceptedOriginCountries = getAcceptedOriginCountries,
            getStatesByCountryCode = getStatesByCountryCode,
            normalizeAddress = normalizeAddress,
            resourceProvider = resourceProvider,
            updateOriginAddress = updateOriginAddress,
            updateDestinationAddress = updateDestinationAddress
        )
    }

    @Test
    fun `when name is not empty and company is empty, then name error is null`() = testBlocking {
        val name = "name"
        val company = ""
        val address = Address.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNull()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when name is empty and company Not is empty, then name error is null`() = testBlocking {
        val name = ""
        val company = "company"
        val address = Address.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNull()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when name is empty and company is empty, then name error is not null`() = testBlocking {
        val name = ""
        val company = ""
        val address = Address.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNotEmpty()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when address is empty then address error is not null`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.address.error).isNotEmpty()
    }

    @Test
    fun `when address is NOT empty then address error is null`() = testBlocking {
        val address = Address.EMPTY.copy(address1 = "This is an address")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.address.error).isNull()
    }

    @Test
    fun `when city is empty then address error is not null`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.city.error).isNotEmpty()
    }

    @Test
    fun `when city is NOT empty then address error is null`() = testBlocking {
        val address = Address.EMPTY.copy(city = "This is a city")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.city.error).isNull()
    }

    @Test
    fun `when postal code is empty then address error is not null`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.postalCode.error).isNotEmpty()
    }

    @Test
    fun `when postal code is NOT empty then address error is null`() = testBlocking {
        val address = Address.EMPTY.copy(postcode = "This is a post code")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.postalCode.error).isNull()
    }

    @Test
    fun `when phone is NOT empty and valid then phone error is null`() = testBlocking {
        val address = Address.EMPTY.copy(phone = "1234567890")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNull()
    }

    @Test
    fun `when company is NOT empty, then company control is expanded`() = testBlocking {
        val company = "company"
        val address = Address.EMPTY.copy(
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.isCompanyExpanded).isTrue()
    }

    @Test
    fun `when company is empty, then company control is NOT expanded`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.isCompanyExpanded).isFalse()
    }

    @Test
    fun `when get accepted countries succeed then don't display loading or error`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.loading).isInstanceOf(WooShippingEditAddressViewModel.LoadingState.Hidden::class.java)
        assertThat(result.error).isNull()
    }

    @Test
    fun `when get accepted countries fails then display error`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.loading).isInstanceOf(WooShippingEditAddressViewModel.LoadingState.Hidden::class.java)
        assertThat(result.error).isNotNull
    }

    @Test
    fun `when get states is empty then use state input`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isTrue()
    }

    @Test
    fun `when get states is empty then use state selection`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
    }

    @Test
    fun `when the country selected has states then use the first state from the list`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
        assertThat(result.editableAddress.state).isEqualTo(states.first())
    }

    @Test
    fun `when the country selected has NO states then use the empty string`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isTrue()
        assertThat(result.editableAddress.state.name).isEqualTo("")
    }

    @Test
    fun `when received address is verified and displayed address has no changes, display verified`() = testBlocking {
        val address = Address(
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = AmbiguousLocation.Raw("FL"),
            country = AmbiguousLocation.Raw("US").asLocation(),
            firstName = "Name",
            lastName = "",
            company = "",
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.VERIFIED)
    }

    @Test
    fun `when received address is verified and displayed address has changes, display unverified`() = testBlocking {
        val address = Address(
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = AmbiguousLocation.Raw("FL"),
            country = AmbiguousLocation.Raw("US").asLocation(),
            firstName = "Name",
            lastName = "",
            company = ""
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
            sut.onAddressChange("Updated address")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.UNVERIFIED)
    }

    @Test
    fun `when only no address related fields has changes, display save changes`() = testBlocking {
        val address = Address(
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = AmbiguousLocation.Raw("FL"),
            country = AmbiguousLocation.Raw("US").asLocation(),
            firstName = "Name",
            lastName = "",
            company = "",
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
            sut.onEmailChange("email@test.com")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.SAVE_CHANGES)
    }

    @Test
    fun `when received address is not verified and displayed address has no changes, display unverified`() =
        testBlocking {
            val address = Address(
                address1 = "Address",
                address2 = "",
                city = "Miami",
                postcode = "",
                email = "",
                phone = "",
                state = AmbiguousLocation.Raw("FL"),
                country = AmbiguousLocation.Raw("US").asLocation(),
                firstName = "Name",
                lastName = "",
                company = "",
            )
            whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
            whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
            Snapshot.withMutableSnapshot {
                val savedState = createSavedStateHandle(address)
                createViewModel(savedState)
            }

            advanceUntilIdle()

            val result = sut.viewState.value

            assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

            assertThat(result.addressStatus).isEqualTo(AddressStatus.UNVERIFIED)
        }

    @Test
    fun `when there are errors, display missing info`() = testBlocking {
        val address = Address(
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = AmbiguousLocation.Raw("FL"),
            country = AmbiguousLocation.Raw("US").asLocation(),
            firstName = "Name",
            lastName = "",
            company = "",
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn("Field required")
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.MISSING_INFO)
    }

    @Test
    fun `when screen is initialized then normalize address is closed`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when normalize address fails, display error`() = testBlocking {
        val address = Address(
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = AmbiguousLocation.Raw("FL"),
            country = AmbiguousLocation.Raw("US").asLocation(),
            firstName = "Name",
            lastName = "",
            company = "",
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(sut.viewState.value.editableAddress)

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.VerificationFailed::class.java)

        assertThat(result.error).isNotNull()
    }

    @Test
    fun `when normalize address succeed, display address selection`() = testBlocking {
        val address = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val normalizeAddressResponse = AddressNormalizationModel(
            address = Address.EMPTY,
            normalizedAddress = Address.EMPTY,
            isTrivial = true
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.AddressSelection::class.java)
    }

    @Test
    fun `when normalize address succeed then suggested address is selected`() = testBlocking {
        val initialAddress = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")

        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        val result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)
        val addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)
    }

    @Test
    fun `when normalize address selection changes then address selection is updated`() = testBlocking {
        val initialAddress = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")

        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        var result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)
        var addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onAddressSelectionChange(addressSelection.copy(selectedAddress = enteredAddress))

        result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)
        addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(enteredAddress)
    }

    @Test
    fun `when normalize address is closed then close address selection`() = testBlocking {
        val initialAddress = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")

        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        var result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)
        val addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onCloseAddressSelection()

        result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when the address selection is expanded then prevent back navigation`() = testBlocking {
        val initialAddress = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")

        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        var result = sut.viewState.value
        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.AddressSelection::class.java)

        val shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isFalse()
    }

    @Test
    fun `when the address selection is NOT expanded then allow back navigation`() = testBlocking {
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(Address.EMPTY)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.NotStarted::class.java)

        val shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isTrue()
    }
}
