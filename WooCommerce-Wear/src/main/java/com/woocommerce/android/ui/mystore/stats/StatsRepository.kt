package com.woocommerce.android.ui.mystore.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.commons.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.commons.wear.DataParameters
import com.woocommerce.commons.wear.DataParameters.CONVERSION_RATE
import com.woocommerce.commons.wear.DataParameters.ORDERS_COUNT
import com.woocommerce.commons.wear.DataParameters.TOTAL_REVENUE
import com.woocommerce.commons.wear.DataParameters.VISITORS_TOTAL
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class StatsRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.STATS) private val statsDataStore: DataStore<Preferences>,
    private val wcStatsStore: WCStatsStore,
) {
    suspend fun fetchRevenueStats(
        selectedSite: SiteModel
    ): Result<WCRevenueStatsModel?> {
        val todayRange = TodayRangeData(
            selectedSite = selectedSite,
            locale = Locale.getDefault(),
            referenceCalendar = Calendar.getInstance()
        ).currentRange

        val result = wcStatsStore.fetchRevenueStats(
            FetchRevenueStatsPayload(
                site = selectedSite,
                granularity = StatsGranularity.DAYS,
                startDate = todayRange.start.formatToYYYYmmDDhhmmss(),
                endDate = todayRange.end.formatToYYYYmmDDhhmmss()
            )
        )

        return when {
            result.isError -> Result.failure(Exception(REVENUE_DATA_ERROR))
            else -> wcStatsStore.getRawRevenueStats(
                selectedSite,
                result.granularity,
                result.startDate.orEmpty(),
                result.endDate.orEmpty()
            ).let { Result.success(it) }
        }
    }

    suspend fun fetchVisitorStats(
        selectedSite: SiteModel
    ): Result<Int?> {
        val todayRange = TodayRangeData(
            selectedSite = selectedSite,
            locale = Locale.getDefault(),
            referenceCalendar = Calendar.getInstance()
        ).currentRange

        val result = wcStatsStore.fetchNewVisitorStats(
            WCStatsStore.FetchNewVisitorStatsPayload(
                site = selectedSite,
                granularity = StatsGranularity.DAYS,
                startDate = todayRange.start.formatToYYYYmmDDhhmmss(),
                endDate = todayRange.end.formatToYYYYmmDDhhmmss()
            )
        )

        return when {
            result.isError -> Result.failure(Exception(VISITOR_DATA_ERROR))
            else -> wcStatsStore.getNewVisitorStats(
                selectedSite,
                result.granularity,
                result.quantity,
                result.date,
                result.isCustomField
            ).let {
                Result.success(it.values.sum())
            }
        }
    }

    suspend fun receiveStatsDataFromPhone(data: DataMap) {
        val totalSales = data.getDouble(TOTAL_REVENUE.value, 0.0)
        val ordersCount = data.getInt(ORDERS_COUNT.value, 0)
        val visitorsCount = data.getInt(VISITORS_TOTAL.value, 0 )
        val conversionRate = data.getString(CONVERSION_RATE.value, "")

        statsDataStore.edit { prefs ->
            prefs[doublePreferencesKey(TOTAL_REVENUE.value)] = totalSales
            prefs[intPreferencesKey(ORDERS_COUNT.value)] = ordersCount
            prefs[intPreferencesKey(VISITORS_TOTAL.value)] = visitorsCount
            prefs[stringPreferencesKey(CONVERSION_RATE.value)] = conversionRate
        }
    }

    companion object {
        private const val REVENUE_DATA_ERROR = "Error fetching revenue data"
        private const val VISITOR_DATA_ERROR = "Error fetching visitor data"
    }
}
