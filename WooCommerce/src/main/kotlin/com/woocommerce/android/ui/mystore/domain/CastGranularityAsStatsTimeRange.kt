package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.util.locale.LocaleProvider
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import org.wordpress.android.fluxc.store.WCStatsStore

class CastGranularityAsStatsTimeRange @Inject constructor(
    private val localeProvider: LocaleProvider
) {
    operator fun invoke(granularity: WCStatsStore.StatsGranularity) =
        StatsTimeRangeSelection.SelectionType.from(granularity)
            .generateSelectionData(
                calendar = Calendar.getInstance(),
                locale = localeProvider.provideLocale() ?: Locale.getDefault()
            )
}
