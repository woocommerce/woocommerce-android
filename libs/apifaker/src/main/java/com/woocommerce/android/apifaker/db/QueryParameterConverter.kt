package com.woocommerce.android.apifaker.db

import androidx.room.TypeConverter
import com.woocommerce.android.apifaker.models.QueryParameter

internal class QueryParameterConverter {
    @TypeConverter
    fun fromQueryParameters(queryParameters: List<QueryParameter>): String {
        return queryParameters.joinToString("&") { "${it.name}:${it.value}" }
    }

    @TypeConverter
    fun toQueryParameters(query: String): List<QueryParameter> {
        return query.takeIf { it.isNotBlank() }?.split("&")?.map { parts ->
            val (name, value) = parts.split(":")
            QueryParameter(name, value)
        } ?: emptyList()
    }
}
