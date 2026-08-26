package com.woocommerce.android.ui.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCard
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.extensions.formatAsRangeWith
import com.woocommerce.android.extensions.formatToLocalizedMonthDayYear
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

internal class AiAssistantStatsCardRenderer(
    private val currencyFormatter: AiAssistantCurrencyFormatter,
) {
    @Composable
    fun Card(
        card: AssistantCard.Stats,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        AiAssistantStatsCard(
            state = card.toStatsCardState(
                currencyFormatter = currencyFormatter,
                unavailableValue = stringResource(R.string.ai_assistant_stats_card_metric_unavailable),
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
        start == end -> start.toDate().formatToLocalizedMonthDayYear(locale)
        else -> start.toDate().formatAsRangeWith(end.toDate(), locale)
    }
}

private fun String.toStatsLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()

private fun LocalDate.toDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

private fun formatStatsMoney(
    value: String,
    currency: String,
    currencyFormatter: AiAssistantCurrencyFormatter,
    unavailableValue: String,
): String =
    when {
        value.isBlank() -> unavailableValue
        currency.isBlank() -> value
        else -> currencyFormatter.formatCurrency(value, currency)
    }

internal fun AssistantCard.Stats.toStatsCardState(
    currencyFormatter: AiAssistantCurrencyFormatter,
    unavailableValue: String,
    locale: Locale = Locale.getDefault(),
): AiAssistantStatsCardState = AiAssistantStatsCardState(
    period = formatStatsPeriod(after, before, unavailableValue, locale),
    metrics = metrics.map { metric ->
        AiAssistantStatsCardState.Metric(
            type = metric.type,
            value = formatStatsMetric(metric, currency, currencyFormatter, unavailableValue),
            chartValues = metric.chartPoints.map { it.value },
        )
    },
)

private fun formatStatsMetric(
    metric: AssistantCard.Stats.Metric,
    currency: String,
    currencyFormatter: AiAssistantCurrencyFormatter,
    unavailableValue: String,
): String =
    when (metric.type) {
        AssistantCard.Stats.MetricType.TotalSales,
        AssistantCard.Stats.MetricType.NetSales,
        AssistantCard.Stats.MetricType.AverageOrderValue ->
            formatStatsMoney(metric.value, currency, currencyFormatter, unavailableValue)
        AssistantCard.Stats.MetricType.TotalOrders -> metric.value.ifBlank { unavailableValue }
    }
