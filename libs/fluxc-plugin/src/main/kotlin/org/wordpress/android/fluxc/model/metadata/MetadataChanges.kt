package org.wordpress.android.fluxc.model.metadata

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject

data class MetadataChanges(
    val insertedMetadata: List<WCMetaData> = emptyList(),
    val updatedMetadata: List<WCMetaData> = emptyList(),
    val deletedMetadataIds: List<Long> = emptyList(),
) {
    init {
        // The ID of inserted metadata is ignored, so to ensure that there is no data loss here,
        // we require that all inserted metadata have an ID of 0.
        require(insertedMetadata.all { it.id == 0L }) {
            "Inserted metadata must have an ID of 0"
        }
    }

    internal fun toJsonArray() = JsonArray().apply {
        insertedMetadata.forEach {
            add(
                JsonObject().apply {
                    addProperty(WCMetaData.KEY, it.key)
                    add(WCMetaData.VALUE, it.value.jsonValue)
                }
            )
        }
        updatedMetadata.forEach {
            add(it.toJson())
        }
        deletedMetadataIds.forEach {
            add(
                JsonObject().apply {
                    addProperty(WCMetaData.ID, it)
                    add(WCMetaData.VALUE, JsonNull.INSTANCE)
                }
            )
        }
    }
}
