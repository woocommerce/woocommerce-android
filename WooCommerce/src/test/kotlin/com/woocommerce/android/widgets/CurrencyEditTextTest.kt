package com.woocommerce.android.widgets

import org.junit.Test
import kotlin.test.assertEquals

class CurrencyEditTextTest {
    @Test
    fun `clean() returns the value without the non-numeric characters`() {
        val text = "US$1345 nope"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 0)

        assertEquals(1345.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() returns the value with the expected number of fractional digits`() {
        val text = "US$1345 nope"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2)

        assertEquals(13.45.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() returns the same value if it is a valid number with the expected number of fractional digits`() {
        val text = "1.23"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2)

        assertEquals(1.23.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() moves the decimal separator if the fractional digits are greater than the decimals argument`() {
        val text = "12.345"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2)

        assertEquals(123.45.toBigDecimal(), cleaned)
    }
}
