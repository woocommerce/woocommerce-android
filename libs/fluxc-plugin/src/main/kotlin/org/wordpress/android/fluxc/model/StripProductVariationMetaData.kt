package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.utils.isElementNullOrEmpty
import javax.inject.Inject

class StripProductVariationMetaData @Inject internal constructor(private val gson: Gson) {
    operator fun invoke(metadata: String?): String? {
        if (metadata.isNullOrEmpty()) return null

        return gson.fromJson(metadata, JsonArray::class.java)
            .mapNotNull { it as? JsonObject }
            .asSequence()
            .filter { jsonObject ->
                val isNullOrEmpty = jsonObject[WCMetaData.VALUE].isElementNullOrEmpty()
                jsonObject[WCMetaData.KEY]?.asString.orEmpty() in SUPPORTED_KEYS && isNullOrEmpty.not()
            }.toList()
            .takeIf { it.isNotEmpty() }
            ?.let { gson.toJson(it) }
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            addAll(WCProductModel.SubscriptionMetadataKeys.ALL_KEYS)
        }
    }
}
