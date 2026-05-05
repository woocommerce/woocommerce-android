package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCard
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class AiAssistantStatsCardRenderer(
    private val currencyFormatter: CurrencyFormatter,
) {
    @Composable
    fun Card(
        card: AssistantCard.Stats,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        val context = LocalContext.current
        AiAssistantStatsCard(
            state = card.toStatsCardState(
                currencyFormatter = currencyFormatter,
                unavailableValue = context.getString(R.string.assistant_stats_card_metric_unavailable),
            ),
            onClick = card.toStatsCardClickHandler(onAction),
            modifier = modifier,
        )
    }
}

internal fun AssistantCard.Stats.toStatsCardClickHandler(
    onAction: (AssistantCardAction) -> Unit,
): () -> Unit = {
    onAction(AssistantCardAction.OpenAnalytics(after, before))
}

private const val ASSISTANT_STATS_PERIOD_SEPARATOR = " - "

private fun formatStatsPeriod(
    after: String,
    before: String,
    unavailableValue: String,
    locale: Locale = Locale.getDefault(),
): String {
    val start = after.toStatsLocalDate()
    val end = before.toStatsLocalDate()
    return when {
        start == null || end == null -> listOf(after, before)
            .filter { it.isNotBlank() }
            .joinToString(ASSISTANT_STATS_PERIOD_SEPARATOR)
            .ifBlank { unavailableValue }
        start == end -> start.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))
        start.year == end.year -> listOf(
            start.format(DateTimeFormatter.ofPattern("MMM d", locale)),
            end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale)),
        ).joinToString(ASSISTANT_STATS_PERIOD_SEPARATOR)
        else -> listOf(
            start.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale)),
            end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale)),
        ).joinToString(ASSISTANT_STATS_PERIOD_SEPARATOR)
    }
}

private fun String.toStatsLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()

private fun AssistantCard.Stats.formatStatsRevenue(
    currencyFormatter: CurrencyFormatter,
    unavailableValue: String,
): String =
    when {
        revenueTotal.isBlank() -> unavailableValue
        revenueCurrency.isBlank() -> revenueTotal
        else -> currencyFormatter.formatCurrency(revenueTotal, revenueCurrency)
    }

internal fun AssistantCard.Stats.toStatsCardState(
    currencyFormatter: CurrencyFormatter,
    unavailableValue: String,
    locale: Locale = Locale.getDefault(),
): AiAssistantStatsCardState = AiAssistantStatsCardState(
    period = formatStatsPeriod(after, before, unavailableValue, locale),
    revenueTotal = formatStatsRevenue(currencyFormatter, unavailableValue),
    orderCount = orderCount.ifBlank { unavailableValue },
    revenueChartValues = revenueChartPoints.map { it.value },
    orderChartValues = orderChartPoints.map { it.value },
    isRevenueTrendAvailable = revenueChartPoints.isNotEmpty(),
    isOrdersTrendAvailable = orderChartPoints.isNotEmpty(),
)
