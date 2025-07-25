package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateHSTariffNumberTest : BaseUnitTest() {
    private val sut = ValidateHSTariffNumber()

    @Test
    fun `when tariff number is valid, then return Data`() {
        val validHSTariffNumber = "1234567890"

        val result = sut(validHSTariffNumber, "US")

        assertEquals(true, result.isValid)
    }

    @Test
    fun `when tariff number includes dots and has 6 digits, then return Data`() {
        val validHSTariffNumber = "12.34.56"

        val result = sut(validHSTariffNumber, "US")

        assertEquals(true, result.isValid)
    }

    @Test
    fun `when tariff number is invalid, then return Data`() {
        val invalidHSTariffNumber = "12345"

        val result = sut(invalidHSTariffNumber, "US")

        assertEquals(false, result.isValid)
    }

    @Test
    fun `given a non-EU country, when tariff number is empty, then return Data`() {
        val emptyHSTariffNumber = ""

        val result = sut(emptyHSTariffNumber, "US")

        assertEquals(true, result.isValid)
    }

    @Test
    fun `given an EU country, when tariff number is empty, then return Error`() {
        val emptyHSTariffNumber = ""

        val result = sut(emptyHSTariffNumber, "FR")

        assertEquals(false, result.isValid)
    }
}
