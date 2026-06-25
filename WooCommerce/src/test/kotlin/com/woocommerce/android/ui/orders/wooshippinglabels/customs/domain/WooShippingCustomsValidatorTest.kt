package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooShippingCustomsValidatorTest : BaseUnitTest() {

    private val validateHSTariffNumber: ValidateHSTariffNumber = mock {
        on { invoke(any(), any()) } doReturn WooShippingCustomsValidator.FieldValidationResult.Valid
    }
    private val validateITN: ValidateITN = mock {
        on { invoke(any(), any()) } doReturn ValidateITN.ITNValidationResult.Valid
    }

    private val validator: WooShippingCustomsValidator = WooShippingCustomsValidator(
        validateHSTariffNumber = validateHSTariffNumber,
        validateITN = validateITN
    )

    private val defaultCustomsData = CustomsData(
        contentType = ContentType.MERCHANDISE,
        contentDescription = "Test content",
        restrictionType = RestrictionType.NONE,
        restrictionDescription = "",
        isReturnToSender = false,
        itn = "123456789",
        items = listOf(
            CustomsItem(
                productID = 1L,
                description = "Test Product",
                quantity = 1.0f,
                value = BigDecimal("10.00"),
                weight = 1.0f,
                hsTariffNumber = "123456",
                originCountry = "United States",
                originCountryCode = "US"
            )
        )
    )

    @Test
    fun `given valid customs data, when validating form, then should return Valid`() = testBlocking {
        val result = validator.validate(defaultCustomsData, "US", "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FormValidationResult.Valid)
    }

    @Test
    fun `given missing ITN, when validating form, then should return ItnMissing`() = testBlocking {
        whenever(validateITN.invoke(any(), any())).thenReturn(
            ValidateITN.ITNValidationResult.Missing(ValidateITN.ITNMissingCause.TotalValue)
        )

        val result = validator.validate(defaultCustomsData, "US", "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FormValidationResult.ItnMissing)
    }

    @Test
    fun `given invalid ITN format, when validating form, then should return Invalid`() = testBlocking {
        whenever(validateITN.invoke(any(), any())).thenReturn(
            ValidateITN.ITNValidationResult.InvalidFormat
        )

        val result = validator.validate(defaultCustomsData, "US", "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FormValidationResult.Invalid)
    }

    @Test
    fun `given invalid product description, when validating form, then should return Invalid`() = testBlocking {
        val customsDataWithBlankDescription = defaultCustomsData.copy(
            items = listOf(
                defaultCustomsData.items.first().copy(description = "")
            )
        )

        val result = validator.validate(customsDataWithBlankDescription, "US", "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FormValidationResult.Invalid)
    }

    @Test
    fun `given valid ITN, when validating ITN field, then should return Valid`() = testBlocking {
        whenever(validateITN.invoke(any(), any())).thenReturn(ValidateITN.ITNValidationResult.Valid)

        val result = validator.validateITN(defaultCustomsData, "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
    }

    @Test
    fun `given missing ITN for total value, when validating ITN field, then should return error with correct message`() =
        testBlocking {
            whenever(validateITN.invoke(any(), any())).thenReturn(
                ValidateITN.ITNValidationResult.Missing(ValidateITN.ITNMissingCause.TotalValue)
            )

            val result = validator.validateITN(defaultCustomsData, "CA")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_total_value)
            )
        }

    @Test
    fun `given missing ITN for destination country, when validating ITN field, then should return error with correct message`() =
        testBlocking {
            whenever(validateITN.invoke(any(), any())).thenReturn(
                ValidateITN.ITNValidationResult.Missing(ValidateITN.ITNMissingCause.DestinationCountry)
            )

            val result = validator.validateITN(defaultCustomsData, "CA")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_destination_country)
            )
        }

    @Test
    fun `given invalid ITN format, when validating ITN field, then should return error with correct message`() =
        testBlocking {
            whenever(validateITN.invoke(any(), any())).thenReturn(
                ValidateITN.ITNValidationResult.InvalidFormat
            )

            val result = validator.validateITN(defaultCustomsData, "CA")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_error_message)
            )
        }

    @Test
    fun `given OTHER content type with description, when validating content type, then should return Valid`() =
        testBlocking {
            val result = validator.validateContentType(ContentType.OTHER, "Test description")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given OTHER content type with blank description, when validating content type, then should return Invalid`() =
        testBlocking {
            val result = validator.validateContentType(ContentType.OTHER, "")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_other_error_message)
            )
        }

    @Test
    fun `given non-OTHER content type, when validating content type, then should return Valid`() = testBlocking {
        val result = validator.validateContentType(ContentType.MERCHANDISE, "")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
    }

    @Test
    fun `given OTHER restriction type with description, when validating restriction type, then should return Valid`() =
        testBlocking {
            val result = validator.validateRestrictionType(RestrictionType.OTHER, "Test restriction")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given OTHER restriction type with blank description, when validating restriction type, then should return Invalid`() =
        testBlocking {
            val result = validator.validateRestrictionType(RestrictionType.OTHER, "")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_other_error_message)
            )
        }

    @Test
    fun `given non-OTHER restriction type, when validating restriction type, then should return Valid`() =
        testBlocking {
            val result = validator.validateRestrictionType(RestrictionType.NONE, "")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given valid HS tariff number, when validating HS tariff, then should return Valid`() = testBlocking {
        whenever(validateHSTariffNumber.invoke("123456", "CA")).thenReturn(
            WooShippingCustomsValidator.FieldValidationResult.Valid
        )

        val result = validator.validateHSTariffNumber("123456", "CA")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
    }

    @Test
    fun `given invalid HS tariff number, when validating HS tariff, then should return Invalid`() = testBlocking {
        val errorMessage = UiString.UiStringText("Invalid HS tariff number")
        whenever(validateHSTariffNumber.invoke("invalid", "CA")).thenReturn(
            WooShippingCustomsValidator.FieldValidationResult.Invalid(errorMessage)
        )

        val result = validator.validateHSTariffNumber("invalid", "CA")

        assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
        val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
        assertThat(invalid.errorMessage).isEqualTo(errorMessage)
    }

    @Test
    fun `given valid product description, when validating product description, then should return Valid`() =
        testBlocking {
            val result = validator.validateProductDescription("Test product", "US", "CA")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given blank product description, when validating product description, then should return Invalid`() =
        testBlocking {
            val result = validator.validateProductDescription("", "US", "CA")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_product_details_description_missing)
            )
        }

    @Test
    fun `given description over 30 chars on USPS domestic mail shipment, when validating description, then should return Invalid`() =
        testBlocking {
            val longDescription = "a".repeat(31)

            val result = validator.validateProductDescription(longDescription, "US", "PR")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(
                    stringRes = R.string.woo_shipping_labels_customs_product_details_description_too_long,
                    params = listOf(UiString.UiStringText("30"))
                )
            )
        }

    @Test
    fun `given description exactly 30 chars on USPS domestic mail shipment, when validating description, then should return Valid`() =
        testBlocking {
            val description = "a".repeat(30)

            val result = validator.validateProductDescription(description, "US", "PR")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given description over 30 chars on international shipment, when validating description, then should return Valid`() =
        testBlocking {
            val longDescription = "a".repeat(50)

            val result = validator.validateProductDescription(longDescription, "US", "CA")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given description over 30 chars but origin is not USPS, when validating description, then should return Valid`() =
        testBlocking {
            val longDescription = "a".repeat(50)

            val result = validator.validateProductDescription(longDescription, "CA", "US")

            assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
        }

    @Test
    fun `given description over 30 chars on territory to territory shipment, when validating description, then should return Invalid`() =
        testBlocking {
            val longDescription = "a".repeat(31)

            val result = validator.validateProductDescription(longDescription, "PR", "GU")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
        }

    @Test
    fun `given valid product value, when validating product value, then should return Valid`() = testBlocking {
        val result = validator.validateProductValue("10.00")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
    }

    @Test
    fun `given blank product value, when validating product value, then should return Invalid`() = testBlocking {
        val result = validator.validateProductValue("")

        assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
        val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
        assertThat(invalid.errorMessage).isEqualTo(
            UiString.UiStringRes(R.string.woo_shipping_labels_customs_product_details_value_required)
        )
    }

    @Test
    fun `given valid product weight, when validating product weight, then should return Valid`() = testBlocking {
        val result = validator.validateProductWeight("1.5")

        assertThat(result).isEqualTo(WooShippingCustomsValidator.FieldValidationResult.Valid)
    }

    @Test
    fun `given blank product weight, when validating product weight, then should return Invalid`() = testBlocking {
        val result = validator.validateProductWeight("")

        assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
        val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
        assertThat(invalid.errorMessage).isEqualTo(
            UiString.UiStringRes(R.string.woo_shipping_labels_customs_product_details_value_required)
        )
    }

    @Test
    fun `given zero product weight, when validating product weight, then should return Invalid`() = testBlocking {
        val result = validator.validateProductWeight("0")

        assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
        val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
        assertThat(invalid.errorMessage).isEqualTo(
            UiString.UiStringRes(R.string.woo_shipping_labels_customs_product_details_weight_invalid)
        )
    }

    @Test
    fun `given invalid product weight format, when validating product weight, then should return Invalid`() =
        testBlocking {
            val result = validator.validateProductWeight("invalid")

            assertThat(result).isInstanceOf(WooShippingCustomsValidator.FieldValidationResult.Invalid::class.java)
            val invalid = result as WooShippingCustomsValidator.FieldValidationResult.Invalid
            assertThat(invalid.errorMessage).isEqualTo(
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_product_details_weight_invalid)
            )
        }
}
