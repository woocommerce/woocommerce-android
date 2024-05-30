package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyVisualTransformationTest {

    @Test
    fun `given currency symbol, when transforming text, then prefix is added to the text`() {
        // GIVEN
        val currencySymbol = "$"
        val transformation = CurrencyVisualTransformation(currencySymbol)
        val input = AnnotatedString("123.45")
        val expectedTransformedText = AnnotatedString("$ 123.45")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        assertEquals(expectedTransformedText.text, transformedText.text.text)
    }

    @Test
    fun `given currency symbol, when transforming text, then offset mapping is correct after transformation`() {
        // GIVEN
        val currencySymbol = "$"
        val prefixLength = "$ ".length
        val transformation = CurrencyVisualTransformation(currencySymbol)
        val input = AnnotatedString("123.45")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        for (i in 0 until input.text.length) {
            val transformedIndex = transformedText.offsetMapping.originalToTransformed(i)
            assertEquals(i + prefixLength, transformedIndex)
        }

        for (i in 0 until transformedText.text.text.length) {
            val originalIndex = transformedText.offsetMapping.transformedToOriginal(i)
            assertEquals((i - prefixLength).coerceAtLeast(0), originalIndex)
        }
    }
}
