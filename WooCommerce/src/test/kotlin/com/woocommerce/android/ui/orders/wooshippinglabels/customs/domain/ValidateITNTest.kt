package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.ui.orders.wooshippinglabels.customs.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsItem
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.RestrictionType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ValidateITNTest : BaseUnitTest() {

    private val validateITN = ValidateITN()

    // Valid ITN examples
    private val validAesItn = "AES X12345678901234"
    private val validNoEeiItn = "NOEEI 30.36"
    private val validNoEeiWithParenthesesItn = "NOEEI 30.36(a)"
    private val validNoEeiWithNestedParenthesesItn = "NOEEI 30.36(a)(2)"
    private val validItnWithWhitespace = "  AES X12345678901234  "

    // Invalid ITN examples
    private val invalidItn = "INVALID ITN"

    @Test
    fun `when ITN is valid AES format, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = validAesItn)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is valid NOEEI format, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = validNoEeiItn)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is valid NOEEI format with parentheses, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = validNoEeiWithParenthesesItn)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is valid NOEEI format with nested parentheses, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = validNoEeiWithNestedParenthesesItn)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is invalid format, then validation result is InvalidFormat`() {
        // Arrange
        val customsData = createCustomsData(itn = invalidItn)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.InvalidFormat)
    }

    @Test
    fun `when ITN is empty and total value exceeds limit with no HS tariff numbers, then validation result is Missing with TotalValue cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(value = BigDecimal("1000"), quantity = 3f, hsTariffNumber = "") // Total value: 3000
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.TotalValue, result.cause)
    }

    @Test
    fun `when ITN is empty and destination country is exempt, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = "")

        // Act
        val result = validateITN(customsData, "CA") // Canada is exempt

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is empty and total value exceeds limit for single item with HS tariff number, then validation result is Missing with HSTariffValue cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(
                    value = BigDecimal("800"),
                    quantity = 3.2f, // Total value: 2560, above the $2500 limit
                    hsTariffNumber = "1234.56"
                )
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.HSTariffValue("1234.56"), result.cause)
    }

    @Test
    fun `when ITN is empty and destination country requires ITN, then validation result is Missing with DestinationCountry cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(createCustomsItem(hsTariffNumber = "")) // Ensure no HS tariff numbers
        )

        // Act
        val result = validateITN(customsData, "IR") // Iran requires ITN

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.DestinationCountry, result.cause)
    }

    @Test
    fun `when ITN is empty and no conditions require ITN, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(value = BigDecimal("100"), quantity = 1f, hsTariffNumber = "") // Total value: 100
            )
        )

        // Act
        val result = validateITN(customsData, "US") // Not in required or exempt list

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN has whitespace, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(itn = validItnWithWhitespace)

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is empty and total value is exactly at limit with no HS tariff number, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(value = BigDecimal("2500"), quantity = 1f, hsTariffNumber = "") // Total value: 2500
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is empty and total value is slightly below limit with no HS tariff number, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(value = BigDecimal("2499.99"), quantity = 1f, hsTariffNumber = "") // Total value: 2499.99
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is empty and total value is slightly above limit with no HS tariff number, then validation result is Missing with TotalValue cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(value = BigDecimal("2500.01"), quantity = 1f, hsTariffNumber = "") // Total value: 2500.01
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.TotalValue, result.cause)
    }

    @Test
    fun `when ITN is empty and multiple items with different HS tariff numbers but none exceeds limit, then validation result is Valid`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(
                    value = BigDecimal("1300"),
                    quantity = 1f,
                    hsTariffNumber = "1234.56"
                ), // Total value: 1300
                createCustomsItem(
                    value = BigDecimal("1300"),
                    quantity = 1f,
                    hsTariffNumber = "7890.12"
                ) // Total value: 1300
                // Each HS tariff number group is below the $2500 limit
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Valid)
    }

    @Test
    fun `when ITN is empty and a single HS tariff number exceeds limit, then validation result is Missing with HSTariffValue cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                createCustomsItem(
                    value = BigDecimal("2600"),
                    quantity = 1f,
                    hsTariffNumber = "1234.56"
                ) // Total value: 2600, exceeds the $2500 limit
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.HSTariffValue("1234.56"), result.cause)
    }

    @Test
    fun `when ITN is empty and multiple items with same HS tariff number exceed total limit, then validation result is Missing with HSTariffValue cause`() {
        // Arrange
        val customsData = createCustomsData(
            itn = "",
            items = listOf(
                // First HS tariff number (1234.56) with total value of 2600
                createCustomsItem(
                    value = BigDecimal("1300"),
                    quantity = 1f,
                    hsTariffNumber = "1234.56"
                ), // Value: 1300
                createCustomsItem(
                    value = BigDecimal("1300"),
                    quantity = 1f,
                    hsTariffNumber = "1234.56"
                ) // Value: 1300
                // Combined total: 2600, exceeds the $2500 limit
            )
        )

        // Act
        val result = validateITN(customsData, "US")

        // Assert
        assertTrue(result is ValidateITN.ITNValidationResult.Missing)
        assertEquals(ValidateITN.ITNMissingCause.HSTariffValue("1234.56"), result.cause)
    }

    private fun createCustomsData(
        itn: String = "",
        items: List<CustomsItem> = listOf(createCustomsItem())
    ): CustomsData {
        return CustomsData(
            contentType = ContentType.MERCHANDISE,
            contentDescription = "Test merchandise",
            restrictionType = RestrictionType.NONE,
            restrictionDescription = "",
            isReturnToSender = false,
            itn = itn,
            items = items
        )
    }

    private fun createCustomsItem(
        productID: Long = 1L,
        description: String = "Test product",
        quantity: Float = 1f,
        value: BigDecimal = BigDecimal("100"),
        weight: Float = 1f,
        hsTariffNumber: String = "",
        originCountry: String = "United States",
        originCountryCode: String = "US"
    ): CustomsItem {
        return CustomsItem(
            productID = productID,
            description = description,
            quantity = quantity,
            value = value,
            weight = weight,
            hsTariffNumber = hsTariffNumber,
            originCountry = originCountry,
            originCountryCode = originCountryCode
        )
    }
}
