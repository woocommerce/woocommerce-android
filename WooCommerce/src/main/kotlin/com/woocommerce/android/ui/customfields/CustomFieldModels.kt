package com.woocommerce.android.ui.customfields

import android.util.Patterns
import androidx.core.util.PatternsCompat
import org.wordpress.android.fluxc.model.metadata.WCMetaData

typealias CustomField = WCMetaData

data class CustomFieldUiModel(
    private val customField: CustomField
) {
    val key
        get() = customField.key
    val value
        get() = customField.valueAsString
    val valueStrippedHtml
        get() = customField.valueStrippedHtml

    val contentType: CustomFieldContentType = CustomFieldContentType.fromMetadataValue(value)
}

enum class CustomFieldContentType {
    TEXT,
    URL,
    EMAIL,
    PHONE;

    companion object {
        fun fromMetadataValue(value: String): CustomFieldContentType {
            return when {
                PatternsCompat.WEB_URL.matcher(value).matches() -> URL
                PatternsCompat.EMAIL_ADDRESS.matcher(value).matches() -> EMAIL
                value.startsWith("tel://") || Patterns.PHONE.matcher(value).matches() -> PHONE
                else -> TEXT
            }
        }
    }
}
