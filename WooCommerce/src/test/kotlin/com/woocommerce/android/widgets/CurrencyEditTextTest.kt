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

    /**
     * We want the last digit to be removed if there is a currency symbol on the right. That's because the `text`
     * argument in [CurrencyEditText.onTextChanged] will have the symbol removed but not the last digit. If we don't
     * handle this manually, it will look like the user _did not delete anything_.
     *
     * Example scenario:
     *
     * 1. If the current text is "123.79CA$" (French Canada places the dollar symbol on the right)
     * 2. The user presses backspace
     * 3. The [CurrencyEditText.onTextChanged] will be called with `text` equal to "123.79CA"
     *
     * If keep this as is and reformat again, we'll end up with same text, "123.79CA$". So it'll look like the backspace
     * was ignored.
     */
    @Test
    fun `given a currency symbol on the right, when pressing backspace, clean() removes the last digit`() {
        val text = "123.79CA"

        // We're only emulating a backspace by specifying that the [lengthBefore] is larger than the [lengthAfter].
        val cleaned = CurrencyEditText.clean(
            text = text,
            decimals = 2,
            lengthBefore = text.length + 1,
            lengthAfter = text.length
        )

        assertEquals(12.37.toBigDecimal(), cleaned)
    }

    @Test
    fun `given a currency symbol with space on the right, when pressing backspace, clean() removes the last digit`() {
        val text = "123.79 $"

        // We're only emulating a backspace by specifying that the [lengthBefore] is larger than the [lengthAfter].
        val cleaned = CurrencyEditText.clean(
            text = text,
            decimals = 2,
            lengthBefore = text.length + 1,
            lengthAfter = text.length
        )

        assertEquals(12.37.toBigDecimal(), cleaned)
    }

    /**
     * In this case, we assume that there was previously a symbol on the right but the backspace deleted it. To emulate
     * proper backspace behavior, the last digit should be removed.
     */
    @Test
    fun `given no currency symbol, when pressing backspace, clean() removes the last digit`() {
        val text = "123.79"

        val cleaned = CurrencyEditText.clean(
            text = text,
            decimals = 2,
            lengthBefore = text.length + 1,
            lengthAfter = text.length
        )

        assertEquals(12.37.toBigDecimal(), cleaned)
    }

    /**
     * We don't want the last digit to be removed in this case because the given `text` argument in
     * [CurrencyEditText.onTextChanged] will already have the last digit removed.
     *
     * Example scenario:
     *
     * 1. If the current text is "123.79"
     * 2. The user presses backspace
     * 3. The [CurrencyEditText.onTextChanged] will be called with `text` equal to "123.7"
     *
     * If we manually remove the last digit, we'd end up with "123", which means we've incorrectly deleted 2 characters!
     */
    @Test
    fun `given a currency symbol on the left, when pressing backspace, clean() does not remove the last digit`() {
        val text = "$1,237,87.39"

        val cleaned = CurrencyEditText.clean(
            text = text,
            decimals = 2,
            lengthBefore = text.length + 1,
            lengthAfter = text.length
        )

        assertEquals(1_237_87.39.toBigDecimal(), cleaned)
    }

    @Test
    fun `given a currency symbol with space on the left, when pressing backspace, clean() does not remove the last digit`() {
        val text = "$ 123.89"

        val cleaned = CurrencyEditText.clean(
            text = text,
            decimals = 2,
            lengthBefore = text.length + 1,
            lengthAfter = text.length
        )

        assertEquals(123.89.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() moves the decimal separator if the fractional digits are greater than the decimals argument`() {
        val text = "12.345"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2, lengthBefore = 0, lengthAfter = 0)

        assertEquals(123.45.toBigDecimal(), cleaned)
    }

    @Test
    fun `clean() returns the same value if it is a valid number with the expected number of fractional digits`() {
        val text = "1.23"

        val cleaned = CurrencyEditText.clean(text = text, decimals = 2, lengthBefore = 0, lengthAfter = 0)

        assertEquals(1.23.toBigDecimal(), cleaned)
    }
}
