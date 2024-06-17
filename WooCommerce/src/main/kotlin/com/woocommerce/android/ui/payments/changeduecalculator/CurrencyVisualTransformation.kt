package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.wordpress.android.fluxc.model.WCSettingsModel

class CurrencyVisualTransformation(
    private val currencySymbol: String,
    private val currencyPosition: WCSettingsModel.CurrencyPosition
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = when (currencyPosition) {
            WCSettingsModel.CurrencyPosition.RIGHT -> {
                text.text + currencySymbol
            }
            WCSettingsModel.CurrencyPosition.RIGHT_SPACE -> {
                text.text + " " + currencySymbol
            }
            WCSettingsModel.CurrencyPosition.LEFT_SPACE -> {
                currencySymbol + " " + text.text
            }
            else -> {
                currencySymbol + text.text
            }
        }

        val adjustedPrefixLength = when (currencyPosition) {
            WCSettingsModel.CurrencyPosition.LEFT_SPACE -> currencySymbol.length + 1
            WCSettingsModel.CurrencyPosition.RIGHT_SPACE -> currencySymbol.length + 1
            WCSettingsModel.CurrencyPosition.LEFT -> currencySymbol.length
            else -> 0
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (currencyPosition == WCSettingsModel.CurrencyPosition.LEFT ||
                    currencyPosition == WCSettingsModel.CurrencyPosition.LEFT_SPACE
                ) {
                    offset + adjustedPrefixLength
                } else {
                    offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (currencyPosition == WCSettingsModel.CurrencyPosition.LEFT ||
                    currencyPosition == WCSettingsModel.CurrencyPosition.LEFT_SPACE
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
