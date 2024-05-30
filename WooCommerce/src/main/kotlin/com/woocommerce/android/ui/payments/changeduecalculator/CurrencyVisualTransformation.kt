package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CurrencyVisualTransformation(private val currencySymbol: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val prefix = "$currencySymbol "
        val transformedText = prefix + text.text
        val prefixOffset = prefix.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + prefixOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                return (offset - prefixOffset).coerceAtLeast(0)
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}

