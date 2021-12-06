package com.woocommerce.android.widgets

import org.junit.Test
import kotlin.test.assertEquals

class CurrencyEditTextTest {
    @Test
    fun `clean() returns the value without the non-numeric characters`() {
        val text = "US$1345 nope"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 0, lengthBefore = 0, lengthAfter = 0)

        assertEquals(1345.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() returns the value with the expected number of fractional digits`() {
        val text = "US$1345 nope"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2, lengthBefore = 0, lengthAfter = 0)

        assertEquals(13.45.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() returns the expected number of fractional digits after pressing backspace`() {
        val text = "12.3"

        // We're only emulating a backspace by specifying that the [lengthBefore] is larger than the [lengthAfter].
        val cleaned = CurrencyEditText.clean(text = text, decimals = 2, lengthBefore = text.length + 1, lengthAfter = text.length)

        assertEquals(1.23.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() moves the decimal separator if the fractional digits are greater than the decimals argument`() {
        val text = "12.345"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2, lengthBefore = 0, lengthAfter = 0)

        assertEquals(123.45.toBigDecimal(), cleaned)
    }
}
