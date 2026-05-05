package com.woocommerce.android.ui.aiassistant

import androidx.navigation.NavDirections
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils.getNormalizedTimezone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun AssistantCardAction.toNavDirections(
    site: SiteModel,
    locale: Locale = Locale.getDefault(),
): NavDirections? =
    when (this) {
        is AssistantCardAction.OpenOrder -> NavGraphMainDirections.actionGlobalOrderDetailFragment(
            orderId = remoteOrderId,
            ignoreTwoPaneLayoutLogic = true,
        )
        is AssistantCardAction.OpenProduct -> NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
        )
        is AssistantCardAction.OpenAnalytics -> analyticsDatesToStatsTimeRangeSelection(
            after = after,
            before = before,
            site = site,
            locale = locale,
        )?.let { rangeSelection ->
            NavGraphMainDirections.actionGlobalAnalytics(rangeSelection)
        }
    }

internal fun analyticsDatesToStatsTimeRangeSelection(
    after: String,
    before: String,
    site: SiteModel,
    locale: Locale = Locale.getDefault(),
): StatsTimeRangeSelection? {
    val calendar = Calendar.getInstance(getNormalizedTimezone(site.timezone), locale)
    val start = after.toSiteDate(calendar) ?: return null
    val end = before.toSiteDate(calendar) ?: return null
    return StatsTimeRangeSelection.build(
        rangeStart = start,
        rangeEnd = end,
        calendar = calendar,
        locale = locale,
    )
}

private fun String.toSiteDate(calendar: Calendar): Date? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }
        .map { localDate ->
            calendar.clear()
            calendar.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth, 0, 0, 0)
            calendar.time
        }
        .getOrNull()
