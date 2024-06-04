package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.model.WCSettingsModel

class CurrencyVisualTransformationTest {

    @Test
    fun `given currency symbol on the left, when transforming text, then prefix is added to the text`() {
        // GIVEN
        val currencySymbol = "$"
        val transformation = CurrencyVisualTransformation(currencySymbol, WCSettingsModel.CurrencyPosition.LEFT)
        val input = AnnotatedString("123.45")
        val expectedTransformedText = AnnotatedString("$123.45")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        assertEquals(expectedTransformedText.text, transformedText.text.text)
    }

    @Test
    fun `given currency symbol on the left, when transforming text, then offset mapping is correct after transformation`() {
        // GIVEN
        val currencySymbol = "$"
        val prefixLength = currencySymbol.length
        val transformation = CurrencyVisualTransformation(currencySymbol, WCSettingsModel.CurrencyPosition.LEFT)
        val input = AnnotatedString("123.45")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        for (i in 0..input.text.length) {
            val transformedIndex = transformedText.offsetMapping.originalToTransformed(i)
            assertEquals(i + prefixLength, transformedIndex)
        }

        for (i in 0..transformedText.text.length) {
            val originalIndex = transformedText.offsetMapping.transformedToOriginal(i)
            if (i < currencySymbol.length) {
                assertEquals(0, originalIndex)
            } else {
                assertEquals((i - prefixLength).coerceAtLeast(0), originalIndex)
            }
        }
    }

    @Test
    fun `given currency symbol on the right, when transforming text, then suffix is added to the text`() {
        // GIVEN
        val currencySymbol = "$"
        val transformation = CurrencyVisualTransformation(currencySymbol, WCSettingsModel.CurrencyPosition.RIGHT)
        val input = AnnotatedString("123.45")
        val expectedTransformedText = AnnotatedString("123.45$")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        assertEquals(expectedTransformedText.text, transformedText.text.text)
    }

    @Test
    fun `given currency symbol on the right, when transforming text, then offset mapping is correct after transformation`() {
        // GIVEN
        val currencySymbol = "$"
        val transformation = CurrencyVisualTransformation(currencySymbol, WCSettingsModel.CurrencyPosition.RIGHT)
        val input = AnnotatedString("123.45")

        // WHEN
        val transformedText = transformation.filter(input)

        // THEN
        for (i in 0 until input.text.length) {
            val transformedIndex = transformedText.offsetMapping.originalToTransformed(i)
            assertEquals(i, transformedIndex)
        }

        for (i in 0 until transformedText.text.length) {
            val originalIndex = transformedText.offsetMapping.transformedToOriginal(i)
            if (i < input.text.length) {
                assertEquals(i, originalIndex)
            } else {
                assertEquals(input.text.length, originalIndex)
            }
        }
    }
}
