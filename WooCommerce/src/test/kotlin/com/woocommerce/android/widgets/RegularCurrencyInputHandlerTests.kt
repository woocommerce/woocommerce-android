package com.woocommerce.android.widgets

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RegularCurrencyInputHandlerTests {
    private lateinit var inputHandler: RegularCurrencyInputHandler

    fun setup(
        supportsEmptyState: Boolean,
        supportsNegativeValues: Boolean,
        decimalSeparator: String = ".",
        numberOfDecimals: Int = 2
    ) {
        inputHandler = RegularCurrencyInputHandler(
            supportsEmptyState = supportsEmptyState,
            supportsNegativeValues = supportsNegativeValues,
            decimalSeparator = decimalSeparator,
            numberOfDecimals = numberOfDecimals
        )
    }

    @Test
    fun `given the field doesn't accept empty state, when deleting last character, then replace content with 0`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = false
        )

        val filteredInput = inputHandler.filter("", 0, 0, "1", 0, 1)

        assertThat(filteredInput).isEqualTo("0")
    }

    @Test
    fun `given the field doesn't accept empty state, when deleting last digit, then replace content with 0`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true
        )

        val filteredInput = inputHandler.filter("", 0, 0, "1", 0, 1)

        assertThat(filteredInput).isEqualTo("0")
    }

    @Test
    fun `given the field accepts empty state, when deleting last character, then empty content`() {
        setup(
            supportsEmptyState = true,
            supportsNegativeValues = true
        )

        val filteredInput = inputHandler.filter("", 0, 0, "1", 0, 1)

        assertThat(filteredInput).isEqualTo("")
    }

    @Test
    fun `given the field doesn't accept empty state, when entering - after 0, then replace accept it`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true
        )

        val filteredInput = inputHandler.filter("-", 0, 1, "0", 1, 2)

        assertThat(filteredInput).isEqualTo("-")
    }

    @Test
    fun `given the field doesn't negative values, when entering -, then reject it`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = false
        )

        val filteredInput = inputHandler.filter("-", 0, 1, "0", 1, 2)

        assertThat(filteredInput).isEqualTo("")
    }

    @Test
    fun `given the field accepts negative values and empty stats, when entering - at the beginning, then accept it`() {
        setup(
            supportsEmptyState = true,
            supportsNegativeValues = true
        )

        val filteredInput = inputHandler.filter("-", 0, 1, "", 0, 0)

        assertThat(filteredInput).isEqualTo("-")
    }

    @Test
    fun `when entering a non valid BigDecimal value, then reject it`() {
        setup(
            supportsEmptyState = true,
            supportsNegativeValues = true
        )

        val filteredInput = inputHandler.filter("a", 0, 1, "20", 2, 3)

        assertThat(filteredInput).isEqualTo("")
    }

    @Test
    fun `when entering fraction digits, then limit it to the allowed number of decimals`() {
        setup(
            supportsEmptyState = true,
            supportsNegativeValues = true,
            numberOfDecimals = 2
        )

        val filteredInput = inputHandler.filter("1", 0, 1, "20.10", 5, 6)

        assertThat(filteredInput).isEqualTo("")
    }

    @Test
    fun `when having 0- as content, then adjust it to -0`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true,
        )

        val adjustedText = inputHandler.adjustText("0-")

        assertThat(adjustedText).isEqualTo("-0")
    }

    @Test
    fun `when having multiple zeros, then adjust text to have a single one`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true,
        )

        val adjustedText = inputHandler.adjustText("00")

        assertThat(adjustedText).isEqualTo("0")
    }

    @Test
    fun `when having unwanted leading zeros, then remove them`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true,
        )

        val adjustedText = inputHandler.adjustText("00999")

        assertThat(adjustedText).isEqualTo("999")
    }

    @Test
    fun `given field doesn't support empty state, when deleting last digit before minus, then update content to 0`() {
        setup(
            supportsEmptyState = false,
            supportsNegativeValues = true,
        )

        val initialText = "-9"
        val filteredInput = inputHandler.filter("", 0, 0, initialText, 1, 2)
        val adjustedText = inputHandler.adjustText(initialText.replaceRange(1, 2, filteredInput))

        assertThat(adjustedText).isEqualTo("0")
    }
}
