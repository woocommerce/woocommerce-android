package org.wordpress.android.fluxc.wc.stats

import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WCStatsTestUtils {
    private val dateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss")
    }

    /**
     * Generates a sample [WCRevenueStatsModel]
     */
    @Suppress("MagicNumber")
    fun generateSampleRevenueStatsModel(
        localSiteId: Int = 6,
        interval: String = StatsGranularity.DAYS.toString(),
        startDate: String = dateTimeFormatter.format(LocalDate.now().atStartOfDay()),
        endDate: String = dateTimeFormatter.format(LocalDate.now().atTime(23, 59, 59)),
        data: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/revenue-stats-data.json")
    ): WCRevenueStatsModel {
        return WCRevenueStatsModel().apply {
            this.localSiteId = localSiteId
            this.interval = interval
            this.endDate = endDate
            this.data = data
            this.startDate = startDate
        }
    }

    /**
     * Generates a sample [WCNewVisitorStatsModel]
     */
    @Suppress("LongParameterList")
    fun generateSampleNewVisitorStatsModel(
        localSiteId: Int = 6,
        granularity: String = StatsGranularity.DAYS.toString(),
        quantity: String = "30",
        startDate: String? = null,
        endDate: String = DateTimeFormatter.ofPattern("YYYY-MM-dd").format(LocalDateTime.now()),
        fields: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/visitor-stats-fields.json"),
        data: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/visitor-stats-data.json")
    ): WCNewVisitorStatsModel {
        return WCNewVisitorStatsModel(
            localSiteId = LocalOrRemoteId.LocalId(localSiteId),
            granularity = granularity,
            quantity = quantity,
            endDate = endDate,
            fields = fields,
            data = data,
            date = endDate,
            startDate = startDate ?: "",
            isCustomField = startDate != null,
        )
    }
}
