package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.utils.NonNegativeIntJsonDeserializer

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCRevenueStatsModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var interval = "" // The unit ("hour", "day", "week", "month", "year")
    @Column var startDate = "" // The start date of the data
    @Column var endDate = "" // The end date of the data
    @Column var data = "" // JSON - A list of lists; each nested list contains the data for a time period
    @Column var total = "" // JSON - A map of total stats for a given time period
    @Column var rangeId = "" // A ID for easily fetch the Revenue Stats for a given start and end date

    companion object {
        private val gson by lazy { Gson() }
    }

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    @Suppress("MemberNameEqualsClassName")
    class Interval {
        val interval: String? = null
        val subtotals: SubTotal? = null
    }

    class SubTotal {
        @SerializedName("orders_count")
        val ordersCount: Long? = null
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
