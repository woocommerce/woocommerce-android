package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AddressValidationHelperTest : BaseUnitTest() {
    private val fieldRequiredError = "This field is required"
    private val invalidPhoneError = "Please enter a valid phone number"
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.woo_shipping_field_required_error) }.thenReturn(fieldRequiredError)
        on { getString(R.string.shipping_label_destination_address_phone_invalid) }.thenReturn(invalidPhoneError)
    }
    private val sut = AddressValidationHelper(resourceProvider)

    @Test
    fun `when all values are blank, then validateAtLeastOneOf should return error`() {
        val result = sut.validateAtLeastOneOf("", " ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when at least one value is not blank, then validateAtLeastOneOf should return null`() {
        val result = sut.validateAtLeastOneOf("", " ", "value")
        assertThat(result).isNull()
    }

    @Test
    fun `when value is empty validateFieldRequired should return error`() {
        val result = sut.validateFieldRequired("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value is blank validateFieldRequired should return error`() {
        val result = sut.validateFieldRequired("  ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value is not blank, then validateFieldRequired should return null`() {
        val result = sut.validateFieldRequired("value")
        assertThat(result).isNull()
    }

    @Test
    fun `given an optional phone, when it is whitespace only, then validatePhone returns null`() {
        val result = sut.validatePhone("  ", "US", false)
        assertThat(result).isNull()
    }

    @Test
    fun `given a required phone, when it is whitespace only, then validatePhone returns the required error`() {
        val result = sut.validatePhone("  ", "US", true)
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `given any country, when the phone has no digits, then validatePhone returns the invalid error`() {
        val result = sut.validatePhone("abc", "CA", false)
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `given a US phone, when it has 10 digits, then validatePhone returns null`() {
        val result = sut.validatePhone("555-123-4567", "US", true)
        assertThat(result).isNull()
    }

    @Test
    fun `given a US phone, when it has 11 digits starting with 1, then validatePhone returns null`() {
        val result = sut.validatePhone("+1 555 123 4567", "US", true)
        assertThat(result).isNull()
    }

    @Test
    fun `given a US phone, when it has 10 digits starting with 1, then validatePhone returns the invalid error`() {
        val result = sut.validatePhone("1555123456", "US", true)
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `given a non-US phone, when it has a digit, then validatePhone returns null`() {
        val result = sut.validatePhone("123", "CA", true)
        assertThat(result).isNull()
    }

    @Test
    fun `when email is empty, then validateEmail should return required error`() {
        val result = sut.validateEmail("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when email is blank, then validateEmail should return required error`() {
        val result = sut.validateEmail("   ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `given different origin and destination countries, when checking the shipment, then it is international`() {
        val result = sut.isInternationalShipment("US", "CA")
        assertThat(result).isTrue()
    }

    @Test
    fun `given the same country in a different letter case, when checking the shipment, then it is not international`() {
        val result = sut.isInternationalShipment("US", "us")
        assertThat(result).isFalse()
    }

    @Test
    fun `given an unknown origin country, when checking the shipment, then it is not international`() {
        val result = sut.isInternationalShipment("", "CA")
        assertThat(result).isFalse()
    }

    @Test
    fun `when origin email is empty, then isMissingOriginAddress returns true`() {
        val result = sut.isMissingOriginAddress(defaultOriginAddress.copy(email = ""))

        assertThat(result).isTrue()
    }

    @Test
    fun `when origin phone is empty, then isMissingOriginAddress returns true`() {
        val result = sut.isMissingOriginAddress(defaultOriginAddress.copy(phone = ""))

        assertThat(result).isTrue()
    }

    @Test
    fun `when required origin fields are present, then isMissingOriginAddress returns false`() {
        val result = sut.isMissingOriginAddress(defaultOriginAddress)

        assertThat(result).isFalse()
    }

    private val defaultOriginAddress = OriginShippingAddress(
        id = "1",
        company = "Company",
        firstName = "John",
        lastName = "Doe",
        email = "john@example.com",
        address1 = "123 Main St",
        address2 = "",
        city = "City",
        state = "CA",
        postcode = "12345",
        country = "US",
        phone = "1234567890",
        isDefault = true,
        isVerified = true
    )
}
