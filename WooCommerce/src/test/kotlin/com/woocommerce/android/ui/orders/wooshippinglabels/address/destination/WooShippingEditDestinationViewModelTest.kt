package com.woocommerce.android.ui.orders.wooshippinglabels.address.destination

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressFlow
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditableAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressFragmentArgs
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressViewModelTest
import com.woocommerce.android.ui.orders.wooshippinglabels.address.toAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingEditDestinationViewModelTest : WooShippingEditAddressViewModelTest() {
    override fun createSavedStateHandle(
        address: Address,
        isVerified: Boolean
    ): SavedStateHandle {
        val destination = DestinationShippingAddress(address, isVerified)
        return WooShippingEditAddressFragmentArgs(
            EditAddressFlow.EditDestinationAddress(destination, 1L, originCountryCode = "US")
        ).toSavedStateHandle()
    }

    override suspend fun mockCountries(countries: Result<List<Location>>) {
        whenever(getAllCountries.invoke()).doReturn(countries)
    }

    @Test
    fun `when update address succeed then restart address state`() = testBlocking {
        val initialAddress = Address.EMPTY
        val editableAddress = EditableAddress()

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAllCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateDestinationAddress.invoke(any(), any(), any()))
            .doReturn(Result.success(DestinationShippingAddress.EMPTY))
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateAddress(editableAddress)

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
        verify(updateOriginAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when the address selection is expanded with an error then prevent back navigation`() = testBlocking {
        val initialAddress = Address.EMPTY
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")
        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAllCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateDestinationAddress.invoke(any(), any(), any())).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateNormalizedAddress(
            AddressValidationState.AddressSelection(
                addressNormalization = normalizeAddressResponse,
                selectedAddress = suggestedAddress
            )
        )

        val result = sut.viewState.value
        assertThat(result.addressValidationState)
            .isInstanceOf(AddressValidationState.NormalizedAddressUpdateFailed::class.java)

        assertThat(result.error).isNotNull()

        val shouldNavigateBack = sut.allowBackNavigation()
        assertThat(shouldNavigateBack).isFalse()
        verify(updateOriginAddress, never()).invoke(any(), any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when update address fails then address state is failure`() = testBlocking {
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
        whenever(getAllCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateDestinationAddress.invoke(any(), any(), any())).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateAddress(sut.viewState.value.editableAddress)

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isInstanceOf(AddressValidationState.AddressUpdateFailed::class.java)
        assertThat(result.error).isNotNull()
        verify(updateOriginAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when update normalized address succeed then restart address state`() = testBlocking {
        val initialAddress = Address.EMPTY
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")
        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAllCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateDestinationAddress.invoke(any(), any(), any()))
            .doReturn(Result.success(DestinationShippingAddress.EMPTY))
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateNormalizedAddress(
            AddressValidationState.AddressSelection(
                addressNormalization = normalizeAddressResponse,
                selectedAddress = suggestedAddress
            )
        )

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
        verify(updateOriginAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when update normalized address fails then address state is failure`() = testBlocking {
        val initialAddress = Address.EMPTY
        val enteredAddress = EditableAddress(postalCode = InputValue("12345")).toAddress()
        val suggestedAddress = enteredAddress.copy(postcode = "12345-1000")
        val normalizeAddressResponse = AddressNormalizationModel(
            address = enteredAddress,
            normalizedAddress = suggestedAddress,
            isTrivial = false,
            errors = null
        )

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAllCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateDestinationAddress.invoke(any(), any(), any())).doReturn(Result.failure(Exception("error")))
        whenever(resourceProvider.getString(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateNormalizedAddress(
            AddressValidationState.AddressSelection(
                addressNormalization = normalizeAddressResponse,
                selectedAddress = suggestedAddress
            )
        )

        val result = sut.viewState.value
        assertThat(result.addressValidationState)
            .isInstanceOf(AddressValidationState.NormalizedAddressUpdateFailed::class.java)

        assertThat(result.error).isNotNull()
        verify(updateOriginAddress, never()).invoke(any(), any())
    }

    @Test
    fun `given a domestic destination, when the phone is blank, then the phone is optional and the form is valid`() =
        testBlocking {
            whenever(addressValidator.validatePhone(eq(""), any(), eq(false))).doReturn(null)
            initViewModelWithPhone(phone = "", countryCode = "US")

            advanceUntilIdle()

            val result = sut.viewState.value

            assertThat(result).isInstanceOf(EditAddressViewState::class.java)

            assertThat(result.editableAddress.phone.isRequired).isFalse()
            assertThat(result.editableAddress.phone.error).isNull()
            assertThat(result.addressStatus).isNotEqualTo(AddressStatus.MissingInfo)
        }

    @Test
    fun `given a domestic destination with a blank phone, when the country changes to an international one, then the phone becomes required`() =
        testBlocking {
            whenever(addressValidator.validatePhone(eq(""), any(), eq(true))).doReturn("error")
            whenever(addressValidator.isInternationalShipment("US", "AR")).doReturn(true)
            initViewModelWithPhone(phone = "", countryCode = "US")

            advanceUntilIdle()

            sut.onCountryChanged("AR")
            advanceUntilIdle()

            assertThat(sut.viewState.value.editableAddress.phone.isRequired).isTrue()
            assertThat(sut.viewState.value.addressStatus).isEqualTo(AddressStatus.MissingInfo)
        }

    @Test
    fun `when email is empty then address error is not null`() = testBlocking {
        val address = Address.EMPTY
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        whenever(addressValidator.validateEmail(any())).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(EditAddressViewState::class.java)

        assertThat(result.editableAddress.email.error).isNotNull()
    }
}
