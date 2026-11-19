package com.woocommerce.android.ui.orders.details

import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData.AdditionalCheckoutFieldsKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue

data class AdditionalCheckoutFields(
    val billing: List<AdditionalCheckoutField>,
    val shipping: List<AdditionalCheckoutField>,
    val other: List<AdditionalCheckoutField>
) {
    val isEmpty: Boolean
        get() = billing.isEmpty() && shipping.isEmpty() && other.isEmpty()
}

data class AdditionalCheckoutField(
    val label: String,
    val value: Value
) {
    sealed interface Value {
        data class Text(val text: String) : Value
        data class Checkbox(val isChecked: Boolean) : Value
    }
}

fun List<WCMetaData>.toAdditionalCheckoutFields() = AdditionalCheckoutFields(
    billing = fieldsWithPrefix(AdditionalCheckoutFieldsKeys.BILLING_PREFIX),
    shipping = fieldsWithPrefix(AdditionalCheckoutFieldsKeys.SHIPPING_PREFIX),
    other = fieldsWithPrefix(AdditionalCheckoutFieldsKeys.OTHER_PREFIX)
)

private fun List<WCMetaData>.fieldsWithPrefix(prefix: String): List<AdditionalCheckoutField> =
    filter { it.key.startsWith(prefix) && !it.isJson }
        .mapNotNull { metaData ->
            val label = metaData.key.removePrefix(prefix).toLabel() ?: return@mapNotNull null
            val value = metaData.toFieldValue() ?: return@mapNotNull null
            AdditionalCheckoutField(label = label, value = value)
        }

private fun String.toLabel(): String? =
    substringAfterLast('/')
        .replace('-', ' ')
        .replace('_', ' ')
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.replaceFirstChar { it.uppercase() }

private fun WCMetaData.toFieldValue(): AdditionalCheckoutField.Value? {
    val metaValue = value
    if (metaValue is WCMetaDataValue.BooleanValue) {
        return AdditionalCheckoutField.Value.Checkbox(metaValue.boolean)
    }
    return when (val text = valueAsString.trim()) {
        "" -> null
        CHECKED_VALUE -> AdditionalCheckoutField.Value.Checkbox(true)
        UNCHECKED_VALUE -> AdditionalCheckoutField.Value.Checkbox(false)
        else -> AdditionalCheckoutField.Value.Text(text)
    }
}

private const val CHECKED_VALUE = "1"
private const val UNCHECKED_VALUE = "0"
