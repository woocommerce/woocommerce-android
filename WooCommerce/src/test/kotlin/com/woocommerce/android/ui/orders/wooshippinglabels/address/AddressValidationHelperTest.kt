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
    fun `when value is empty validateUSCustomsPhone should return error`() {
        val result = sut.validateUSCustomsPhone("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value is blank validateUSCustomsPhone should return error`() {
        val result = sut.validateUSCustomsPhone(" ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value is not a number validateUSCustomsPhone should return error`() {
        val result = sut.validateUSCustomsPhone("phone")
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `when value is not a valid US phone number validateUSCustomsPhone should return error`() {
        val result = sut.validateUSCustomsPhone("123456789")
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `when value is a valid US phone number, then validateUSCustomsPhone should return null`() {
        val result = sut.validateUSCustomsPhone("12345678910")
        assertThat(result).isNull()
    }

    @Test
    fun `when value is empty or blank, then validatePhoneNumber should return error`() {
        val result = sut.validatePhoneNumber("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value does not contain any digits, then validatePhoneNumber should return error`() {
        val result = sut.validatePhoneNumber("abc")
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `when value contains at least one digit, then validatePhoneNumber should return null`() {
        val result = sut.validatePhoneNumber("123")
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
    fun `when phone is empty, then isPhoneValidForShippingLabel returns false`() {
        val result = sut.isPhoneValidForShippingLabel("")
        assertThat(result).isFalse()
    }

    @Test
    fun `when phone is blank, then isPhoneValidForShippingLabel returns false`() {
        val result = sut.isPhoneValidForShippingLabel("   ")
        assertThat(result).isFalse()
    }

    @Test
    fun `when phone has no digits, then isPhoneValidForShippingLabel returns false`() {
        val result = sut.isPhoneValidForShippingLabel("abc-def")
        assertThat(result).isFalse()
    }

    @Test
    fun `when phone has at least one digit, then isPhoneValidForShippingLabel returns true`() {
        val result = sut.isPhoneValidForShippingLabel("123-456-7890")
        assertThat(result).isTrue()
    }

    @Test
    fun `when phone has minimal digit, then isPhoneValidForShippingLabel returns true`() {
        val result = sut.isPhoneValidForShippingLabel("1")
        assertThat(result).isTrue()
    }

    @Test
    fun `when phone is empty, then validatePhoneNumber returns required error`() {
        val result = sut.validatePhoneNumber("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when phone is blank, then validatePhoneNumber returns required error`() {
        val result = sut.validatePhoneNumber("   ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when phone has no digits, then validatePhoneNumber returns invalid error`() {
        val result = sut.validatePhoneNumber("abc-def")
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `when phone has at least one digit, then validatePhoneNumber returns null`() {
        val result = sut.validatePhoneNumber("123-456-7890")
        assertThat(result).isNull()
    }

    @Test
    fun `when phone has mixed characters with digit, then validatePhoneNumber returns null`() {
        val result = sut.validatePhoneNumber("+1 (555) 123-4567")
        assertThat(result).isNull()
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
