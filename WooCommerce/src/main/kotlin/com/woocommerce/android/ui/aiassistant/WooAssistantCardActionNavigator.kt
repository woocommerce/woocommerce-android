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
import java.util.TimeZone

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
    val timeZone = getNormalizedTimezone(site.timezone)
    val start = after.toSiteDate(timeZone, locale) ?: return null
    val end = before.toSiteDate(timeZone, locale) ?: return null
    return StatsTimeRangeSelection.build(
        rangeStart = start,
        rangeEnd = end,
        calendar = Calendar.getInstance(timeZone, locale),
        locale = locale,
    )
}

private fun String.toSiteDate(timeZone: TimeZone, locale: Locale): Date? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }
        .map { localDate ->
            val calendar = Calendar.getInstance(timeZone, locale)
            calendar.clear()
            calendar.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth, 0, 0, 0)
            calendar.time
        }
        .getOrNull()
