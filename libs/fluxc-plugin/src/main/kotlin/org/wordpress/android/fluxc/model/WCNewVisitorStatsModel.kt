package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import java.util.Locale

@Entity(
    tableName = "NewVisitorStatsEntity",
    primaryKeys = ["localSiteId", "granularity", "date", "quantity", "isCustomField"],
)
data class WCNewVisitorStatsModel(
    val localSiteId: LocalId,
    val granularity: String, // The granularity ("days", "weeks", "months", "years")
    val date: String, // The formatted end date of the data
    val startDate: String, // The start date of the data
    val endDate: String, // The end date of the data
    val quantity: String, // The quantity based on unit. i.e. 30 days, 17 weeks, 12 months, 2 years
    val isCustomField: Boolean, // to check if the data is for custom stats or default stats
    val fields: String, // JSON - A map of numerical index to stat name, used to lookup the stat in the data object
    val data: String, // JSON - A list of lists; each nested list contains the data for a time period
) {
    // The fields JSON deserialized into a list of strings
    val fieldsList by lazy {
        val responseType = object : TypeToken<List<String>>() {}.type
        gson.fromJson(fields, responseType) as? List<String> ?: emptyList()
    }

    // The data JSON deserialized into a list of lists of arbitrary type
    val dataList by lazy {
        val responseType = object : TypeToken<List<List<Any>>>() {}.type
        gson.fromJson(data, responseType) as? List<List<Any>> ?: emptyList()
    }

    enum class VisitorStatsField {
        PERIOD,
        VISITORS;

        override fun toString() = name.lowercase(Locale.getDefault())
    }

    companion object {
        private val gson by lazy { Gson() }
    }

    fun getIndexForField(field: VisitorStatsField) = fieldsList.indexOf(field.toString())
}
