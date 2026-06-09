package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
    protected val getAllCountries: GetAllCountries = mock()
    protected val analyticsTracker: AnalyticsTrackerWrapper = mock()

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
    abstract suspend fun mockCountries(countries: Result<List<Location>>)

    fun createViewModel(savedState: SavedStateHandle) {
        sut = WooShippingEditAddressViewModel(
            addressValidator = addressValidator,
            savedState = savedState,
            getAcceptedOriginCountries = getAcceptedOriginCountries,
            getStatesByCountryCode = getStatesByCountryCode,
            normalizeAddress = normalizeAddress,
            resourceProvider = resourceProvider,
            updateOriginAddress = updateOriginAddress,
            updateDestinationAddress = updateDestinationAddress,
            getAllCountries = getAllCountries,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `when viewmodel initializes, then tracks started event`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        verify(analyticsTracker).track(eq(AnalyticsEvent.WCS_EDITING_ADDRESS_STEP), any())
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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNull()
    }

    private suspend fun initViewModelWithPhone(
        phone: String,
        countryCode: String,
        hasStates: Boolean = countryCode == "US"
    ) {
        val address = Address.EMPTY.copy(
            phone = phone,
            country = AmbiguousLocation.Raw(countryCode).asLocation()
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(if (hasStates) states else emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }
    }

    @Test
    fun `given US country, when phone is too short, then phone error is not null`() = testBlocking {
        whenever(addressValidator.validateUSCustomsPhone("12345")).doReturn("error")
        initViewModelWithPhone(phone = "12345", countryCode = "US")

        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNotEmpty()
    }

    @Test
    fun `given US country, when phone has 10 digits, then phone error is null`() = testBlocking {
        whenever(addressValidator.validateUSCustomsPhone("1234567890")).doReturn(null)
        initViewModelWithPhone(phone = "1234567890", countryCode = "US")

        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNull()
    }

    @Test
    fun `given non-US country, when phone has digits, then phone error is null`() = testBlocking {
        whenever(addressValidator.validatePhoneNumber("12345")).doReturn(null)
        initViewModelWithPhone(phone = "12345", countryCode = "AR")

        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNull()
    }

    @Test
    fun `given US phone error, when country changes to non-US, then phone error is cleared`() = testBlocking {
        whenever(addressValidator.validateUSCustomsPhone("12345")).doReturn("error")
        whenever(addressValidator.validatePhoneNumber("12345")).doReturn(null)
        initViewModelWithPhone(phone = "12345", countryCode = "US")

        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNotEmpty()

        sut.onCountryChanged("AR")
        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNull()
    }

    @Test
    fun `given non-US valid phone, when country changes to US, then phone error is set`() = testBlocking {
        whenever(addressValidator.validatePhoneNumber("12345")).doReturn(null)
        whenever(addressValidator.validateUSCustomsPhone("12345")).doReturn("error")
        initViewModelWithPhone(phone = "12345", countryCode = "AR")

        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNull()

        sut.onCountryChanged("US")
        advanceUntilIdle()

        assertThat(sut.viewState.value.editableAddress.phone.error).isNotEmpty()
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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.isCompanyExpanded).isFalse()
    }

    @Test
    fun `when get accepted countries succeed then don't display loading or error`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.loading).isInstanceOf(LoadingState.Hidden::class.java)
        assertThat(result.error).isNull()
    }

    @Test
    fun `when get accepted countries fails then display error`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.loading).isInstanceOf(LoadingState.Hidden::class.java)
        assertThat(result.error).isNotNull
    }

    @Test
    fun `when get states is empty then use state input`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.shouldUseStatesInput).isTrue()
    }

    @Test
    fun `when get states is NOT empty then use state selection`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
    }

    @Test
    fun `when the country selected has states then use the first state from the list`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.shouldUseStatesInput).isFalse()
        assertThat(result.editableAddress.state).isEqualTo(states.first())
    }

    @Test
    fun `when the country selected changes to a country with states then use the first state from the list`() =
        testBlocking {
            val initialCountryCode = "AR"
            val finalCountryCode = "US"
            val initialStateCode = "BA"
            val address = Address.EMPTY.copy(
                country = AmbiguousLocation.Raw(initialCountryCode).asLocation(),
                state = AmbiguousLocation.Raw("BA")
            )
            whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
            mockCountries(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(initialCountryCode)).doReturn(emptyList())
            whenever(getStatesByCountryCode.invoke(finalCountryCode)).doReturn(states)
            Snapshot.withMutableSnapshot {
                val savedState = createSavedStateHandle(address)
                createViewModel(savedState)
            }

            advanceUntilIdle()

            var result = sut.viewState.value

            assertThat(result.shouldUseStatesInput).isTrue
            assertThat(result.editableAddress.state.code).isEqualTo(initialStateCode)

            sut.onCountryChanged(finalCountryCode)

            result = sut.viewState.value

            assertThat(result.shouldUseStatesInput).isFalse()
            assertThat(result.editableAddress.state).isEqualTo(states.first())
        }

    @Test
    fun `when the country selected changes to initial country then use initial state`() = testBlocking {
        val initialCountryCode = "AR"
        val finalCountryCode = "US"
        val initialStateCode = "BA"
        val address = Address.EMPTY.copy(
            country = AmbiguousLocation.Raw(initialCountryCode).asLocation(),
            state = AmbiguousLocation.Raw("BA")
        )
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(initialCountryCode)).doReturn(emptyList())
        whenever(getStatesByCountryCode.invoke(finalCountryCode)).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        var result = sut.viewState.value

        assertThat(result.shouldUseStatesInput).isTrue
        assertThat(result.editableAddress.state.code).isEqualTo(initialStateCode)

        // Change country for the first time

        sut.onCountryChanged(finalCountryCode)

        result = sut.viewState.value

        assertThat(result.shouldUseStatesInput).isFalse()
        assertThat(result.editableAddress.state).isEqualTo(states.first())

        // Reset country to the initial one

        sut.onCountryChanged(initialCountryCode)

        result = sut.viewState.value

        assertThat(result.shouldUseStatesInput).isTrue
        assertThat(result.editableAddress.state.code).isEqualTo(initialStateCode)
    }

    @Test
    fun `when the country selected has NO states then use the empty string`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(emptyList())
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.Verified)
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
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
            sut.onAddressChange("Updated address")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.Unverified)
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
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address, true)
            createViewModel(savedState)
            sut.onEmailChange("email@test.com")
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.SaveChanges)
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
            mockCountries(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
            Snapshot.withMutableSnapshot {
                val savedState = createSavedStateHandle(address)
                createViewModel(savedState)
            }

            advanceUntilIdle()

            val result = sut.viewState.value

            assertThat(result).isInstanceOf(EditAddressViewState::class.java)

            assertThat(result.addressStatus).isEqualTo(AddressStatus.Unverified)
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
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressStatus).isEqualTo(AddressStatus.MissingInfo)
    }

    @Test
    fun `given a valid form, when email becomes invalid, then address status is MissingInfo before debounce`() =
        testBlocking {
            val address = Address(
                address1 = "Address",
                address2 = "",
                city = "Miami",
                postcode = "12345",
                email = "valid@test.com",
                phone = "1234567890",
                state = AmbiguousLocation.Raw("FL"),
                country = AmbiguousLocation.Raw("US").asLocation(),
                firstName = "Name",
                lastName = "",
                company = ""
            )
            whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
            whenever(addressValidator.validateAtLeastOneOf(any(), any())).doReturn(null)
            whenever(addressValidator.validateEmail("valid@test.com")).doReturn(null)
            whenever(addressValidator.validateEmail("")).doReturn("Required")
            whenever(addressValidator.validateUSCustomsPhone(any())).doReturn(null)
            mockCountries(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
            Snapshot.withMutableSnapshot {
                val savedState = createSavedStateHandle(address, isVerified = true)
                createViewModel(savedState)
            }
            advanceUntilIdle()
            assertThat(sut.viewState.value.addressStatus).isNotEqualTo(AddressStatus.MissingInfo)

            Snapshot.withMutableSnapshot {
                sut.onEmailChange("")
            }
            // Advance by less than DELAY_TIME_MILLIS (500) so the per-field validator does not fire.
            advanceTimeBy(100)

            assertThat(sut.viewState.value.addressStatus).isEqualTo(AddressStatus.MissingInfo)
        }

    @Test
    fun `given an invalid form, when email becomes valid, then address status is not MissingInfo before debounce`() =
        testBlocking {
            val address = Address(
                address1 = "Address",
                address2 = "",
                city = "Miami",
                postcode = "12345",
                email = "",
                phone = "1234567890",
                state = AmbiguousLocation.Raw("FL"),
                country = AmbiguousLocation.Raw("US").asLocation(),
                firstName = "Name",
                lastName = "",
                company = ""
            )
            whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
            whenever(addressValidator.validateAtLeastOneOf(any(), any())).doReturn(null)
            whenever(addressValidator.validateEmail("")).doReturn("Required")
            whenever(addressValidator.validateEmail("valid@test.com")).doReturn(null)
            whenever(addressValidator.validateUSCustomsPhone(any())).doReturn(null)
            mockCountries(Result.success(countries))
            whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
            Snapshot.withMutableSnapshot {
                val savedState = createSavedStateHandle(address, isVerified = true)
                createViewModel(savedState)
            }
            advanceUntilIdle()
            assertThat(sut.viewState.value.addressStatus).isEqualTo(AddressStatus.MissingInfo)

            Snapshot.withMutableSnapshot {
                sut.onEmailChange("valid@test.com")
            }
            advanceTimeBy(100)

            assertThat(sut.viewState.value.addressStatus).isNotEqualTo(AddressStatus.MissingInfo)
        }

    @Test
    fun `when screen is initialized then normalize address is closed`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

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
        mockCountries(Result.success(countries))
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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.VerificationFailed::class.java)

        assertThat(result.error).isNotNull()
    }

    @Test
    fun `when normalize address fails, track event error`() = testBlocking {
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
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(sut.viewState.value.editableAddress)

        verify(analyticsTracker, atLeastOnce()).track(
            eq(AnalyticsEvent.WCS_EDITING_ADDRESS_STEP),
            any(),
            any(),
            any(),
            anyOrNull()
        )
    }

    @Test
    fun `when normalize address succeed, display address selection`() = testBlocking {
        val address = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val normalizeAddressResponse = AddressNormalizationModel(
            address = Address.EMPTY,
            normalizedAddress = Address.EMPTY,
            isTrivial = true,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
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

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.AddressSelection::class.java)
    }

    @Test
    fun `when normalize address succeed, track events`() = testBlocking {
        val address = Address.EMPTY
        val updatedAddress = EditableAddress(postalCode = InputValue("12345"))
        val normalizeAddressResponse = AddressNormalizationModel(
            address = Address.EMPTY,
            normalizedAddress = Address.EMPTY,
            isTrivial = true,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(normalizeAddress.invoke(any())).doReturn(Result.success(normalizeAddressResponse))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onNormalizeAddress(updatedAddress)

        verify(analyticsTracker, times(2)).track(eq(AnalyticsEvent.WCS_EDITING_ADDRESS_STEP), any())
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
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
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
        assertThat(result).isInstanceOf(EditAddressViewState::class.java)
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
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
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
        assertThat(result).isInstanceOf(EditAddressViewState::class.java)
        var addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onAddressSelectionChange(addressSelection.copy(selectedAddress = enteredAddress))

        result = sut.viewState.value
        assertThat(result).isInstanceOf(EditAddressViewState::class.java)
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
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
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
        assertThat(result).isInstanceOf(EditAddressViewState::class.java)
        val addressSelection = result.addressValidationState as AddressValidationState.AddressSelection
        assertThat(addressSelection.selectedAddress).isEqualTo(suggestedAddress)

        sut.onCloseAddressSelection()

        result = sut.viewState.value
        assertThat(result).isInstanceOf(EditAddressViewState::class.java)
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
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        mockCountries(Result.success(countries))
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
        mockCountries(Result.success(countries))
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
