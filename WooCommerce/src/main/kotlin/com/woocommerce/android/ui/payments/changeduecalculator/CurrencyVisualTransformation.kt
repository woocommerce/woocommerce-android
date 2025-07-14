package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT_SPACE

class CurrencyVisualTransformation(
    private val currencySymbol: String,
    private val currencyPosition: CurrencyPosition
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = when (currencyPosition) {
            RIGHT -> {
                text.text + currencySymbol
            }
            RIGHT_SPACE -> {
                text.text + " " + currencySymbol
            }
            LEFT_SPACE -> {
                currencySymbol + " " + text.text
            }
            else -> {
                currencySymbol + text.text
            }
        }

        val adjustedPrefixLength = when (currencyPosition) {
            LEFT_SPACE -> currencySymbol.length + 1
            RIGHT_SPACE -> currencySymbol.length + 1
            LEFT -> currencySymbol.length
            else -> 0
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (currencyPosition == LEFT ||
                    currencyPosition == LEFT_SPACE
                ) {
                    offset + adjustedPrefixLength
                } else {
                    offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (currencyPosition == LEFT ||
                    currencyPosition == LEFT_SPACE
                ) {
                    (offset - adjustedPrefixLength).coerceAtLeast(0)
                } else {
                    offset.coerceAtMost(text.length)
                }
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}
