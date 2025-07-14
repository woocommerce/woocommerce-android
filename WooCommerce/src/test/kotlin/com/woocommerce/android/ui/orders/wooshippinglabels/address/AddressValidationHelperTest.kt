package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.R
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
    fun `when all values are empty or blank then validateAtLeastOneOf should return error`() {
        val result = sut.validateAtLeastOneOf("", " ")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when at least one value is not empty or blank validateAtLeastOneOf should return null`() {
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
    fun `validateFieldRequired should return null when value is not empty or blank`() {
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
    fun `when value is a valid US phone number validateUSCustomsPhone should return null`() {
        val result = sut.validateUSCustomsPhone("12345678910")
        assertThat(result).isNull()
    }

    @Test
    fun `when value is empty or blank validateCustomsPhone should return error`() {
        val result = sut.validateCustomsPhone("")
        assertThat(result).isEqualTo(fieldRequiredError)
    }

    @Test
    fun `when value does not contain any digits validateCustomsPhone should return error`() {
        val result = sut.validateCustomsPhone("abc")
        assertThat(result).isEqualTo(invalidPhoneError)
    }

    @Test
    fun `when value contains at least one digit validateCustomsPhone should return null`() {
        val result = sut.validateCustomsPhone("123")
        assertThat(result).isNull()
    }
}
