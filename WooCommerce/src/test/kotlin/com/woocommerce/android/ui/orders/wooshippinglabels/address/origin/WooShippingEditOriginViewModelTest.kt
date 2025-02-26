package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import androidx.compose.runtime.snapshots.Snapshot
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetStatesByCountryCode
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
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
class WooShippingEditOriginViewModelTest : BaseUnitTest() {
    private val addressValidator: AddressValidationHelper = mock()
    private val getAcceptedOriginCountries: GetAcceptedOriginCountries = mock()
    private val getStatesByCountryCode: GetStatesByCountryCode = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val normalizeAddress: NormalizeAddress = mock()
    private val updateOriginAddress: UpdateOriginAddress = mock()

    private val countries = listOf(
        Location("US", "United States"),
        Location("UK", "United Kingdom"),
        Location("AR", "Argentina"),
        Location("BR", "Brazil")
    )

    private val states = listOf(
        Location("FL", "Florida"),
        Location("CA", "California"),
    )

    private lateinit var sut: WooShippingEditOriginViewModel

    fun createViewModel(originAddress: OriginShippingAddress) {
        sut = WooShippingEditOriginViewModel(
            addressValidator = addressValidator,
            savedState = WooShippingEditOriginAddressFragmentArgs(originAddress).toSavedStateHandle(),
            getAcceptedOriginCountries = getAcceptedOriginCountries,
            getStatesByCountryCode = getStatesByCountryCode,
            normalizeAddress = normalizeAddress,
            resourceProvider = resourceProvider,
            updateOriginAddress = updateOriginAddress
        )
    }

    @Test
    fun `when name is not empty and company is empty, then name error is null`() = testBlocking {
        val name = "name"
        val company = ""
        val address = OriginShippingAddress.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNull()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when name is empty and company Not is empty, then name error is null`() = testBlocking {
        val name = ""
        val company = "company"
        val address = OriginShippingAddress.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNull()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when name is empty and company is empty, then name error is not null`() = testBlocking {
        val name = ""
        val company = ""
        val address = OriginShippingAddress.EMPTY.copy(
            firstName = name,
            company = company
        )
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.name.error).isNotEmpty()
        assertThat(result.editableAddress.company.error).isNull()
    }

    @Test
    fun `when address is empty then address error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.address.error).isNotEmpty()
    }

    @Test
    fun `when address is NOT empty then address error is null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(address1 = "This is an address")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.address.error).isNull()
    }

    @Test
    fun `when city is empty then address error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.city.error).isNotEmpty()
    }

    @Test
    fun `when city is NOT empty then address error is null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(city = "This is a city")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.city.error).isNull()
    }

    @Test
    fun `when postal code is empty then address error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.postalCode.error).isNotEmpty()
    }

    @Test
    fun `when postal code is NOT empty then address error is null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(postcode = "This is a post code")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.postalCode.error).isNull()
    }

    @Test
    fun `when email is empty then address error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.email.error).isNotEmpty()
    }

    @Test
    fun `when email is NOT empty then address error is null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(email = "This is an email")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.email.error).isNull()
    }

    @Test
    fun `when phone is empty then phone error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        whenever(addressValidator.validateUSCustomsPhone("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNotEmpty()
    }

    @Test
    fun `when phone is NOT empty and invalid then phone error is not null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(phone = "1234567890")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        whenever(addressValidator.validateUSCustomsPhone("1234567890")).doReturn("error")
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNotEmpty()
    }

    @Test
    fun `when phone is NOT empty and valid then phone error is null`() = testBlocking {
        val address = OriginShippingAddress.EMPTY.copy(phone = "1234567890")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        whenever(addressValidator.validateUSCustomsPhone("1234567890")).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNull()
    }

    @Test
    fun `when company is NOT empty, then company control is expanded`() = testBlocking {
        val company = "company"
        val address = OriginShippingAddress.EMPTY.copy(
            company = company
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.isCompanyExpanded).isTrue()
    }

    @Test
    fun `when company is empty, then company control is NOT expanded`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.isCompanyExpanded).isFalse()
    }

    @Test
    fun `when get accepted countries succeed then don't display loading or error`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.loading).isInstanceOf(WooShippingEditOriginViewModel.LoadingState.Hidden::class.java)
        assertThat(result.shouldDisplayLoadingCountriesError).isFalse()
    }

    @Test
    fun `when get accepted countries fails then display error`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.failure(Exception("error")))
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.loading).isInstanceOf(WooShippingEditOriginViewModel.LoadingState.Hidden::class.java)
        assertThat(result.shouldDisplayLoadingCountriesError).isTrue()
    }

    @Test
    fun `when get states is empty then use state input`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isTrue()
    }

    @Test
    fun `when get states is empty then use state selection`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
    }

    @Test
    fun `when the country selected has states then use the first state from the list`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
        assertThat(result.editableAddress.state).isEqualTo(states.first())
    }

    @Test
    fun `when the country selected has NO states then use the empty string`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.shouldUseStatesInput).isTrue()
        assertThat(result.editableAddress.state.name).isEqualTo("")
    }

    @Test
    fun `when received address is verified and displayed address has no changes, display verified`() = testBlocking {
        val address = OriginShippingAddress(
            id = "1",
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = "FL",
            country = "US",
            firstName = "Name",
            lastName = "",
            company = "",
            isVerified = true,
            isDefault = true
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.VERIFIED)
    }

    @Test
    fun `when received address is verified and displayed address has changes, display unverified`() = testBlocking {
        val address = OriginShippingAddress(
            id = "1",
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = "FL",
            country = "US",
            firstName = "Name",
            lastName = "",
            company = "",
            isVerified = true,
            isDefault = true
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
            sut.onAddressChange("Updated address")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.UNVERIFIED)
    }

    @Test
    fun `when only no address related fields has changes, display save changes`() = testBlocking {
        val address = OriginShippingAddress(
            id = "1",
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = "FL",
            country = "US",
            firstName = "Name",
            lastName = "",
            company = "",
            isVerified = true,
            isDefault = true
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
            sut.onEmailChange("email@test.com")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.SAVE_CHANGES)
    }

    @Test
    fun `when received address is not verified and displayed address has no changes, display unverified`() =
        testBlocking {
            val address = OriginShippingAddress(
                id = "1",
                address1 = "Address",
                address2 = "",
                city = "Miami",
                postcode = "",
                email = "",
                phone = "",
                state = "FL",
                country = "US",
                firstName = "Name",
                lastName = "",
                company = "",
                isVerified = false,
                isDefault = true
            )
            whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
            whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
            Snapshot.withMutableSnapshot {
                createViewModel(address)
            }

            advanceUntilIdle()

            val result = sut.viewState.value

            assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

            assertThat(result.addressStatus).isEqualTo(AddressStatus.UNVERIFIED)
        }

    @Test
    fun `when there are errors, display missing info`() = testBlocking {
        val address = OriginShippingAddress(
            id = "1",
            address1 = "Address",
            address2 = "",
            city = "Miami",
            postcode = "",
            email = "",
            phone = "",
            state = "FL",
            country = "US",
            firstName = "Name",
            lastName = "",
            company = "",
            isVerified = true,
            isDefault = true
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn("Field required")
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.MISSING_INFO)
    }

    @Test
    fun `when screen is initialized then normalize address is closed`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when normalize address fails, display error`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.failure(Exception("error")))
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.VerificationFailed)
    }

    @Test
    fun `when normalize address succeed, display address selection`() = testBlocking {
        val address = OriginShippingAddress.EMPTY
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
        Snapshot.withMutableSnapshot {
            createViewModel(address)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.AddressSelection::class.java)
    }

    @Test
    fun `when normalize address succeed then suggested address is selected`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
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
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        val result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)
        val addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)
    }

    @Test
    fun `when normalize address selection changes then address selection is updated`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
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
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        var result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)
        var addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onAddressSelectionChange(addressSelection.copy(selectedAddress = enteredAddress))

        result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)
        addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(enteredAddress)
    }

    @Test
    fun `when normalize address is closed then close address selection`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
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
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        var result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)
        val addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onCloseAddressSelection()

        result = sut.viewState.value
        assertThat(result).isInstanceOf(WooShippingEditOriginViewModel.ViewState::class.java)
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when update address succeed then restart address state`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
        val editableAddress = EditableAddress()

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.success(initialAddress))
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onUpdateOriginAddress(editableAddress)

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when update address fails then address state is failure`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
        val editableAddress = EditableAddress()

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.failure(Exception("error")))
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onUpdateOriginAddress(editableAddress)

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.AddressUpdateFailed)
    }

    @Test
    fun `when update normalized address succeed then restart address state`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
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
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.success(initialAddress))
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onUpdateNormalizedOriginAddress(
            AddressValidationState.AddressSelection(
                addressNormalization = normalizeAddressResponse,
                selectedAddress = suggestedAddress
            )
        )

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
    }

    @Test
    fun `when update normalized address fails then address state is failure`() = testBlocking {
        val initialAddress = OriginShippingAddress.EMPTY
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
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.failure(Exception("error")))
        Snapshot.withMutableSnapshot {
            createViewModel(initialAddress)
        }

        advanceUntilIdle()

        sut.onUpdateNormalizedOriginAddress(
            AddressValidationState.AddressSelection(
                addressNormalization = normalizeAddressResponse,
                selectedAddress = suggestedAddress
            )
        )

        val result = sut.viewState.value
        assertThat(result.addressValidationState)
            .isInstanceOf(AddressValidationState.NormalizedAddressUpdateFailed::class.java)
    }
}
