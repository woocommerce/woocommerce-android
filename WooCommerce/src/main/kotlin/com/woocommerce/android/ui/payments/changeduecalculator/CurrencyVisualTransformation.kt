package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import org.wordpress.android.fluxc.model.WCSettingsModel

class CurrencyVisualTransformation(
    private val currencySymbol: String,
    private val currencyPosition: WCSettingsModel.CurrencyPosition
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = when (currencyPosition) {
            WCSettingsModel.CurrencyPosition.RIGHT, WCSettingsModel.CurrencyPosition.RIGHT_SPACE -> {
                text.text + currencySymbol
            }
            else -> {
                currencySymbol + text.text
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (currencyPosition == WCSettingsModel.CurrencyPosition.LEFT ||
                    currencyPosition == WCSettingsModel.CurrencyPosition.LEFT_SPACE) {
                    offset + currencySymbol.length
                } else {
                    offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (currencyPosition == WCSettingsModel.CurrencyPosition.LEFT ||
                    currencyPosition == WCSettingsModel.CurrencyPosition.LEFT_SPACE) {
                    (offset - currencySymbol.length).coerceAtLeast(0)
                } else {
                    offset.coerceAtMost(text.length)
                }
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}
