package com.woocommerce.android.ui.products.models

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCProductModel.QuantityRulesMetadataKeys
import javax.inject.Inject

class QuantityRulesMapper @Inject constructor(private val gson: Gson) {
    fun toAppModel(metadata: String): QuantityRules? {
        val jsonArray = gson.fromJson(metadata, JsonArray::class.java)
        val quantityRulesInformation = jsonArray
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject -> jsonObject[WCMetaData.KEY].asString in QuantityRulesMetadataKeys.ALL_KEYS }
            .associate { jsonObject ->
                jsonObject[WCMetaData.KEY].asString to jsonObject[WCMetaData.VALUE].asString
            }

        val min = quantityRulesInformation[QuantityRulesMetadataKeys.MINIMUM_ALLOWED_QUANTITY]?.toIntOrNull()
        val max = quantityRulesInformation[QuantityRulesMetadataKeys.MAXIMUM_ALLOWED_QUANTITY]?.toIntOrNull()
        val group = quantityRulesInformation[QuantityRulesMetadataKeys.GROUP_OF_QUANTITY]?.toIntOrNull()

        return if (min == null && max == null && group == null) {
            null
        } else {
            QuantityRules(min = min, max = max, groupOf = group)
        }
    }
}
