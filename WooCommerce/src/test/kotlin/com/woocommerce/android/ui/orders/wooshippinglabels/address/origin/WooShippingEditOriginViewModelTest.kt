package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditAddressFlow
import com.woocommerce.android.ui.orders.wooshippinglabels.address.EditableAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.InputValue
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressFragmentArgs
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.address.WooShippingEditAddressViewModelTest
import com.woocommerce.android.ui.orders.wooshippinglabels.address.toAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
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
class WooShippingEditOriginViewModelTest : WooShippingEditAddressViewModelTest() {
    override fun createSavedStateHandle(
        address: Address,
        isVerified: Boolean
    ): SavedStateHandle {
        val originAddress = OriginShippingAddress(
            id = "123",
            company = address.company,
            firstName = address.firstName,
            lastName = address.lastName,
            email = address.email,
            address1 = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
            phone = address.phone,
            isDefault = true,
            isVerified = isVerified
        )
        return WooShippingEditAddressFragmentArgs(EditAddressFlow.EditOriginAddress(originAddress)).toSavedStateHandle()
    }

    override suspend fun mockCountries(countries: Result<List<Location>>) {
        whenever(getAcceptedOriginCountries.invoke()).doReturn(countries)
    }

    @Test
    fun `when update address succeed then restart address state`() = testBlocking {
        val initialAddress = Address.EMPTY
        val editableAddress = EditableAddress()

        whenever(addressValidator.validateFieldRequired(any())).doReturn(null)
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.success(OriginShippingAddress.EMPTY))
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(initialAddress)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        sut.onUpdateAddress(editableAddress)

        val result = sut.viewState.value
        assertThat(result.addressValidationState).isEqualTo(AddressValidationState.NotStarted)
        verify(updateDestinationAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when the address selection is expanded with an error then prevent back navigation`() = testBlocking {
        val initialAddress = Address.EMPTY
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
        verify(updateDestinationAddress, never()).invoke(any(), any())
    }

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
        whenever(getAcceptedOriginCountries.invoke()).doReturn(Result.success(countries))
        whenever(getStatesByCountryCode.invoke(any())).doReturn(states)
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.failure(Exception("error")))
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
        verify(updateDestinationAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when update normalized address succeed then restart address state`() = testBlocking {
        val initialAddress = Address.EMPTY
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
        whenever(updateOriginAddress.invoke(any(), any())).doReturn(Result.success(OriginShippingAddress.EMPTY))
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
        verify(updateDestinationAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when update normalized address fails then address state is failure`() = testBlocking {
        val initialAddress = Address.EMPTY
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
        verify(updateDestinationAddress, never()).invoke(any(), any())
    }

    @Test
    fun `when phone is empty phone then error is NOT null`() = testBlocking {
        val address = Address.EMPTY.copy(phone = "")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.phone.error).isNotEmpty
    }

    @Test
    fun `when email is empty then address error is not null`() = testBlocking {
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

        assertThat(result.editableAddress.email.error).isNotEmpty()
    }

    @Test
    fun `when email is NOT empty then address error is null`() = testBlocking {
        val address = Address.EMPTY.copy(email = "This is an email")
        whenever(addressValidator.validateAtLeastOneOf(eq(""), eq(""))).doReturn("error")
        whenever(addressValidator.validateFieldRequired("")).doReturn("error")
        Snapshot.withMutableSnapshot {
            val savedState = createSavedStateHandle(address)
            createViewModel(savedState)
        }

        advanceUntilIdle()

        val result = sut.viewState.value

        assertThat(result).isInstanceOf(WooShippingEditAddressViewModel.ViewState::class.java)

        assertThat(result.editableAddress.email.error).isNull()
    }
}
