package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.metadata.WCMetaData

internal class WCMetaDataConverter {
    @TypeConverter
    @Suppress("ReturnCount")
    fun metaDataListToString(value: List<WCMetaData>?): String? {
        if (value == null) {
            return null
        }
        if (value.isEmpty()) {
            return ""
        }

        return JsonArray().apply {
            value.forEach { metaData ->
                add(metaData.toJson())
            }
        }.toString()
    }

    @TypeConverter
    @Suppress("ReturnCount")
    fun stringToMetaDataList(value: String?): List<WCMetaData>? {
        if (value == null) {
            return null
        }
        if (value.isEmpty()) {
            return emptyList()
        }

        return JsonParser().parse(value).asJsonArray.map { jsonElement ->
            val jsonObject = jsonElement as? JsonObject ?: return@map null
            return@map WCMetaData.fromJson(jsonObject)
        }.filterNotNull()
    }
}
