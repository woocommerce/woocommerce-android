package com.woocommerce.android.ui.customfields

import androidx.core.util.PatternsCompat
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.util.HtmlUtils
import java.util.regex.Pattern

typealias CustomField = WCMetaData

data class CustomFieldUiModel(
    private val customField: CustomField
) {
    val key
        get() = customField.key
    val value
        get() = customField.valueAsString
    val valueStrippedHtml: String
        get() = HtmlUtils.fastStripHtml(value)

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
                value.startsWith("tel://") || phonePattern.matcher(value).matches() -> PHONE
                else -> TEXT
            }
        }

        private val phonePattern by lazy {
            // Copied from android.util.Patterns.PHONE to make it work with tests
            Pattern.compile(
                // sdd = space, dot, or dash
                "(\\+[0-9]+[\\- .]*)?" + // +<digits><sdd>*
                    "(\\([0-9]+\\)[\\- .]*)?" + // (<digits>)<sdd>*
                    "([0-9][0-9\\- .]+[0-9])" // <digit><digit|sdd>+<digit>
            )
        }
    }
}
