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
            WCSettingsModel.CurrencyPosition.RIGHT, WCSettingsModel.CurrencyPosition.RIGHT_SPACE -> {
                text.text + currencySymbol
            }
            else -> {
                currencySymbol + text.text
            }
        }
        return TransformedText(
            text = AnnotatedString(transformedText),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
