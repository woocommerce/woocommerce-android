package com.woocommerce.android.ui.customfields

import android.os.Parcelable
import androidx.core.util.PatternsCompat
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.util.HtmlUtils
import java.util.regex.Pattern

typealias CustomField = WCMetaData

@Parcelize
data class CustomFieldUiModel(
    val key: String,
    val value: String,
    val id: Long? = null,
    val isJson: Boolean = false
) : Parcelable {
    constructor(customField: CustomField) : this(
        key = customField.key,
        value = customField.valueAsString,
        id = customField.id,
        isJson = customField.isJson
    )

    val valueStrippedHtml: String
        get() = HtmlUtils.fastStripHtml(value)

    val hasHtml: Boolean
        get() = valueStrippedHtml != value

    @IgnoredOnParcel
    val contentType: CustomFieldContentType = CustomFieldContentType.fromMetadataValue(value)

    fun toDomainModel(): CustomField {
        require(!isJson) {
            "Editing JSON custom fields is not supported, this shouldn't be called for JSON custom fields"
        }

        return CustomField(
            id = id ?: 0, // Use 0 for new custom fields
            key = key,
            value = WCMetaDataValue.StringValue(value) // Treat all updates as string values
        )
    }
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
