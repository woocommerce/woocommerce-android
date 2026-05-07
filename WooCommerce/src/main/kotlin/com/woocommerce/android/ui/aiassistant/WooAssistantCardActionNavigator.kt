package com.woocommerce.android.ui.aiassistant

import androidx.navigation.NavDirections
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun AssistantCardAction.toNavDirections(
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
        is AssistantCardAction.OpenCustomer -> null
        is AssistantCardAction.OpenAnalytics -> analyticsDatesToStatsTimeRangeSelection(
            after = after,
            before = before,
            locale = locale,
        )?.let { rangeSelection ->
            NavGraphMainDirections.actionGlobalAnalytics(rangeSelection)
        }
    }

internal fun analyticsDatesToStatsTimeRangeSelection(
    after: String,
    before: String,
    locale: Locale = Locale.getDefault(),
): StatsTimeRangeSelection? {
    // Analytics Hub converts the selected range to the site timezone when fetching data; keep these as local dates.
    val start = after.toAppDate(locale) ?: return null
    val end = before.toAppDate(locale) ?: return null
    return StatsTimeRangeSelection.build(
        rangeStart = start,
        rangeEnd = end,
        calendar = Calendar.getInstance(locale),
        locale = locale,
    )
}

private fun String.toAppDate(locale: Locale): Date? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }
        .map { localDate ->
            val calendar = Calendar.getInstance(locale)
            calendar.clear()
            calendar.set(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth, 0, 0, 0)
            calendar.time
        }
        .getOrNull()
