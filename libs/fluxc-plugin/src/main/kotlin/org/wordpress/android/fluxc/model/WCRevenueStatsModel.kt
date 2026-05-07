package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.utils.NonNegativeIntJsonDeserializer

@Entity(
    tableName = "RevenueStatsEntity",
    primaryKeys = ["localSiteId", "rangeId"],
)
data class WCRevenueStatsModel(
    val localSiteId: LocalId,
    val interval: String, // The unit ("hour", "day", "week", "month", "year")
    val startDate: String, // The start date of the data
    val endDate: String, // The end date of the data
    val data: String, // JSON - A list of lists; each nested list contains the data for a time period
    val total: String, // JSON - A map of total stats for a given time period
    val rangeId: String, // A ID for easily fetch the Revenue Stats for a given start and end date
) {
    companion object {
        private val gson by lazy { Gson() }
    }

    @Suppress("MemberNameEqualsClassName")
    class Interval {
        val interval: String? = null
        val subtotals: SubTotal? = null
    }

    class SubTotal {
        @SerializedName("orders_count")
        val ordersCount: Long? = null
        @SerializedName("gross_sales")
        val grossSales: Double? = null
        @SerializedName("total_sales")
        val totalSales: Double? = null
        @SerializedName("net_revenue")
        val netRevenue: Double? = null
        @SerializedName("avg_order_value")
        val avgOrderValue: Double? = null
    }

    /**
     * Deserializes the JSON contained in [data] into a list of [Interval] objects.
     */
    fun getIntervalList(): List<Interval> {
        val responseType = object : TypeToken<List<Interval>>() {}.type
        return gson.fromJson(data, responseType) as? List<Interval> ?: emptyList()
    }

    class Total {
        @SerializedName("orders_count")
        val ordersCount: Int? = null
        @SerializedName("gross_sales")
        val grossSales: Double? = null
        @SerializedName("total_sales")
        val totalSales: Double? = null
        @SerializedName("net_revenue")
        val netRevenue: Double? = null
        @SerializedName("avg_order_value")
        val avgOrderValue: Double? = null
        @JsonAdapter(NonNegativeIntJsonDeserializer::class)
        @SerializedName("num_items_sold")
        val itemsSold: Int? = null
    }

    /**
     * Deserializes the JSON contained in [data] into a Total object.
     * The [total] param by default is a map which is parsed into [Total].
     *
     * There are some instances where the [total] param can be an empty list
     * https://github.com/woocommerce/woocommerce-android/issues/2154
     *
     * To address this issue, we check if the [total] param is a JsonArray
     * and return a null response, if that's the case.
     */
    fun parseTotal(): Total? {
        val jsonElement = JsonParser().parse(total)
        return if (jsonElement.isJsonArray) {
            if (jsonElement.asJsonArray.size() > 0) {
                val responseType = object : TypeToken<Total>() {}.type
                gson.fromJson(jsonElement.asJsonArray[0], responseType) as? Total
            } else null
        } else {
            val responseType = object : TypeToken<Total>() {}.type
            gson.fromJson(total, responseType) as? Total
        }
    }
}
