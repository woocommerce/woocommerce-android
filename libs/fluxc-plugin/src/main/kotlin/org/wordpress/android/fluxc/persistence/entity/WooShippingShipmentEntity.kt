package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@Entity(
    primaryKeys = ["localSiteId", "orderId", "shipmentId"],
)
@TypeConverters(WooShippingShipmentItemListConverter::class)
data class WooShippingShipmentEntity(
    val localSiteId: LocalOrRemoteId.LocalId,
    val orderId: LocalOrRemoteId.RemoteId,
    val shipmentId: String,
    val items: List<Item>
) {
    data class Item(
        val id: Long?,
        val subItems: List<String>?
    )
}

internal class WooShippingShipmentItemListConverter {
    @TypeConverter
    fun listToString(value: List<WooShippingShipmentEntity.Item>?): String? =
        value?.let {
            JsonArray().apply {
                it.forEach { item ->
                    add(
                        JsonObject().apply {
                            addProperty("id", item.id)
                            addProperty("sub_items", item.subItems?.joinToString(separator = ","))
                        }
                    )
                }
            }.toString()
        }

    @TypeConverter
    fun stringToList(value: String?): List<WooShippingShipmentEntity.Item>? =
        value?.let {
            JsonParser.parseString(it).asJsonArray.mapNotNull { jsonElement ->
                val jsonObject = jsonElement as? JsonObject ?: return@mapNotNull null
                val id = jsonObject.get("id")?.asLong
                val subItems = jsonObject.get("sub_items")?.asString?.split(",")
                WooShippingShipmentEntity.Item(id, subItems)
            }
        }
}
