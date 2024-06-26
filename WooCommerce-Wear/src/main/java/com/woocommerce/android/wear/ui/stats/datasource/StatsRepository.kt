package com.woocommerce.android.wear.ui.stats.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType
import com.woocommerce.android.wear.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.wear.extensions.getSiteId
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.StoreStatsData.RevenueData
import com.woocommerce.android.wear.ui.stats.range.TodayRangeData
import com.woocommerce.android.wear.util.DateUtils
import com.woocommerce.commons.DataParameters.ORDERS_COUNT
import com.woocommerce.commons.DataParameters.TOTAL_REVENUE
import com.woocommerce.commons.DataParameters.VISITORS_TOTAL
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
    private val loginRepository: LoginRepository,
    private val dateUtils: DateUtils,
    private val locale: Locale
) {
    private val gson by lazy { Gson() }

    suspend fun fetchRevenueStats(
        selectedSite: SiteModel
    ): Result<WCRevenueStatsModel?> {
        val todayRange = TodayRangeData(
            dateUtils = dateUtils,
            locale = locale,
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
            dateUtils = dateUtils,
            locale = locale,
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
        val statsJson = StoreStatsData(
            revenueData = RevenueData(
                totalRevenue = data.getString(TOTAL_REVENUE.value, ""),
                orderCount = data.getInt(ORDERS_COUNT.value, 0)
            ),
            visitorData = data.getInt(VISITORS_TOTAL.value, 0),
        ).let { gson.toJson(it) }
        val siteId = data.getSiteId(loginRepository.selectedSite)

        statsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(generateStatsKey(siteId))] = statsJson
        }
    }

    fun observeStatsDataChanges(
        selectedSite: SiteModel
    ) = statsDataStore.data
        .mapNotNull { it[stringPreferencesKey(generateStatsKey(selectedSite.siteId))] }
        .map { gson.fromJson(it, StoreStatsData::class.java) }

    private fun generateStatsKey(siteId: Long): String {
        return "$STATS_KEY_PREFIX:$siteId"
    }

    companion object {
        private const val REVENUE_DATA_ERROR = "Error fetching revenue data"
        private const val VISITOR_DATA_ERROR = "Error fetching visitor data"
        private const val STATS_KEY_PREFIX = "store-stats"
    }
}
