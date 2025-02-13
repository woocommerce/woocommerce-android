package org.wordpress.android.fluxc.model.metadata

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue.JsonArrayValue
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue.JsonObjectValue
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue.WCMetaDataValueJsonAdapter
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.HtmlUtils

data class WCMetaData(
    @SerializedName(ID)
    val id: Long,
    @SerializedName(KEY)
    val key: String,
    @JsonAdapter(WCMetaDataValueJsonAdapter::class)
    @SerializedName(VALUE)
    val value: WCMetaDataValue,
    @SerializedName(DISPLAY_KEY)
    val displayKey: String? = null,
    @JsonAdapter(WCMetaDataValueJsonAdapter::class)
    @SerializedName(DISPLAY_VALUE)
    val displayValue: WCMetaDataValue? = null
) {
    constructor(id: Long, key: String, value: String) : this(id, key,
        WCMetaDataValue(value)
    )

    /**
     * Verify if the Metadata key is not null or a internal store attribute
     * @return false if the `key` starts with the `_` character
     * @return true otherwise
     */
    val isDisplayable
        get() = key.startsWith('_').not()

    val valueAsString: String
        get() = value.stringValue.orEmpty()

    val valueStrippedHtml: String
        get() = HtmlUtils.fastStripHtml(valueAsString)

    val isHtml: Boolean
        get() = valueAsString != valueStrippedHtml

    val isJson: Boolean
        get() = value is JsonObjectValue || value is JsonArrayValue

    internal fun toJson(): JsonObject {
        return JsonObject().also {
            it.addProperty(ID, id)
            it.addProperty(KEY, key)
            it.add(VALUE, value.jsonValue)
            displayKey?.let { key -> it.addProperty(DISPLAY_KEY, key) }
            displayValue?.let { value -> it.add(DISPLAY_VALUE, value.jsonValue) }
        }
    }

    companion object {
        const val ID = "id"
        const val KEY = "key"
        const val VALUE = "value"
        private const val DISPLAY_KEY = "display_key"
        private const val DISPLAY_VALUE = "display_value"

        internal fun fromJson(json: JsonElement): WCMetaData? = runCatching {
            val jsonObject = json.asJsonObject
            WCMetaData(
                id = jsonObject[ID].asLong,
                key = jsonObject[KEY].asString,
                value = WCMetaDataValue.fromJsonElement(jsonObject[VALUE]),
                displayKey = jsonObject[DISPLAY_KEY]?.asString,
                displayValue = jsonObject[DISPLAY_VALUE]?.let { WCMetaDataValue.fromJsonElement(it) }
            )
        }.onFailure {
            AppLog.w(AppLog.T.UTILS, "Error parsing WCMetaData from JSON $json, cause: ${it.stackTraceToString()}")
        }.getOrNull()
    }

    object SubscriptionMetadataKeys {
        const val SUBSCRIPTION_RENEWAL = "_subscription_renewal"
    }

    object BundleMetadataKeys {
        const val BUNDLED_ITEM_ID = "_bundled_item_id"
    }

    object OrderAttributionInfoKeys {
        const val SOURCE_TYPE = "_wc_order_attribution_source_type"
        const val CAMPAIGN = "_wc_order_attribution_utm_campaign"
        const val SOURCE = "_wc_order_attribution_utm_source"
        const val MEDIUM = "_wc_order_attribution_utm_medium"
        const val DEVICE_TYPE = "_wc_order_attribution_device_type"
        const val SESSION_PAGE_VIEWS = "_wc_order_attribution_session_pages"

        val ALL_KEYS
            get() = setOf(
                SOURCE_TYPE,
                CAMPAIGN,
                SOURCE,
                MEDIUM,
                DEVICE_TYPE,
                SESSION_PAGE_VIEWS
            )
    }

    object AddOnsMetadataKeys {
        const val ADDONS_METADATA_KEY = "_product_addons"
    }
}

operator fun List<WCMetaData>.get(key: String) = firstOrNull { it.key == key }

