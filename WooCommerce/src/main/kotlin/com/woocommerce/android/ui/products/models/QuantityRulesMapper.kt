package com.woocommerce.android.ui.products.models

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import javax.inject.Inject

class QuantityRulesMapper @Inject constructor(private val gson: Gson) {
    fun toAppModelFromProductMetadata(metadata: String): QuantityRules? {
        return getQuantityRulesInfo(
            metadata = metadata,
            minKey = WCProductModel.QuantityRulesMetadataKeys.MINIMUM_ALLOWED_QUANTITY,
            maxKey = WCProductModel.QuantityRulesMetadataKeys.MAXIMUM_ALLOWED_QUANTITY,
            groupOfKey = WCProductModel.QuantityRulesMetadataKeys.GROUP_OF_QUANTITY
        )
    }

    fun toAppModelFromVariationMetadata(metadata: String): QuantityRules? {
        return getQuantityRulesInfo(
            metadata = metadata,
            minKey = WCProductVariationModel.QuantityRulesMetadataKeys.MINIMUM_ALLOWED_QUANTITY,
            maxKey = WCProductVariationModel.QuantityRulesMetadataKeys.MAXIMUM_ALLOWED_QUANTITY,
            groupOfKey = WCProductVariationModel.QuantityRulesMetadataKeys.GROUP_OF_QUANTITY
        )
    }

    private fun getQuantityRulesInfo(
        metadata: String,
        minKey: String,
        maxKey: String,
        groupOfKey: String
    ): QuantityRules? {
        val jsonArray = gson.fromJson(metadata, JsonArray::class.java)
        val allKeys = setOf(minKey, maxKey, groupOfKey)
        val quantityRulesInformation = jsonArray
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject -> jsonObject[WCMetaData.KEY].asString in allKeys }
            .associate { jsonObject ->
                jsonObject[WCMetaData.KEY].asString to jsonObject[WCMetaData.VALUE].asString
            }
        val min = quantityRulesInformation[minKey]?.toIntOrNull()
        val max = quantityRulesInformation[maxKey]?.toIntOrNull()
        val group = quantityRulesInformation[groupOfKey]?.toIntOrNull()

        return if (min == null && max == null && group == null) {
            null
        } else {
            QuantityRules(min = min, max = max, groupOf = group)
        }
    }
}
