package com.woocommerce.android.ui.customfields

import android.os.Parcelable
import androidx.core.util.PatternsCompat
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.util.HtmlUtils
import java.util.regex.Pattern

typealias CustomField = WCMetaData

@Parcelize
data class CustomFieldUiModel(
    val key: String,
    val value: String,
    val id: Long? = null,
) : Parcelable {
    constructor(customField: CustomField) : this(customField.key, customField.valueAsString, customField.id)

    val valueStrippedHtml: String
        get() = HtmlUtils.fastStripHtml(value)

    @IgnoredOnParcel
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
