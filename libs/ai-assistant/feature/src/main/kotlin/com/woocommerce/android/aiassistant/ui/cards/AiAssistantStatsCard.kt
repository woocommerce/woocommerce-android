package com.woocommerce.android.aiassistant.ui.cards

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R

@Composable
fun AiAssistantStatsCard(
    state: AiAssistantStatsCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.assistant_stats_card_open_content_description, state.period)
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.period,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
        StatsMetricGrid(metrics = state.metrics)
    }
}

@Composable
private fun StatsMetricGrid(metrics: List<AiAssistantStatsCardState.Metric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(STATS_CARD_GRID_COLUMNS).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                rowMetrics.forEach { metric ->
                    StatsMetric(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(STATS_CARD_GRID_COLUMNS - rowMetrics.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatsMetric(
    metric: AiAssistantStatsCardState.Metric,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(metric.type.labelRes()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = metric.value,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (shouldShowStatsTrendChart(metric.chartValues)) {
            AssistantStatsTrendChart(
                points = metric.chartValues,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun AssistantCard.Stats.MetricType.labelRes(): Int =
    when (this) {
        AssistantCard.Stats.MetricType.TotalSales -> R.string.assistant_stats_card_total_sales_label
        AssistantCard.Stats.MetricType.NetSales -> R.string.assistant_stats_card_net_sales_label
        AssistantCard.Stats.MetricType.TotalOrders -> R.string.assistant_stats_card_total_orders_label
        AssistantCard.Stats.MetricType.AverageOrderValue -> R.string.assistant_stats_card_average_order_value_label
    }

@VisibleForTesting
internal fun shouldShowStatsTrendChart(values: List<Double>): Boolean = values.size > 1

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 360, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedMultiPoint() {
    AiAssistantStatsCard(state = sampleUnifiedStatsCardState(), onClick = {})
}

@Preview(name = "Large Font", showBackground = true, widthDp = 360, heightDp = 420, fontScale = 1.5f)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedLargeFont() {
    AiAssistantStatsCard(state = sampleUnifiedStatsCardState(), onClick = {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedPartialData() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            netSalesChartValues = emptyList(),
            averageOrderValueChartValues = listOf(80.10),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedLaterTrendOnly() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesChartValues = emptyList(),
            netSalesChartValues = emptyList(),
            totalOrdersChartValues = emptyList(),
            averageOrderValueChartValues = listOf(80.10, 82.25, 91.20),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 260)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedNoTrend() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesChartValues = listOf(12.0),
            netSalesChartValues = listOf(9.0),
            totalOrdersChartValues = emptyList(),
            averageOrderValueChartValues = emptyList(),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedLongCurrencyValues() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesValue = "$123,456,789.01",
            netSalesValue = "$98,765,432.10",
            averageOrderValue = "$12,345.67",
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun AiAssistantStatsCardPreviewLegacyTwoMetricReplay() {
    AiAssistantStatsCard(state = sampleLegacyTwoMetricStatsCardState(), onClick = {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedChangedShape() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesChartValues = listOf(26.0, 9.0, 22.0, 7.0, 18.0),
            netSalesChartValues = listOf(15.0, 4.0, 18.0, 3.0, 12.0),
            totalOrdersChartValues = listOf(3.0, 8.0, 6.0, 12.0, 10.0),
            averageOrderValueChartValues = listOf(90.0, 75.0, 110.0, 68.0, 92.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedAllZero() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesChartValues = listOf(0.0, 0.0, 0.0),
            netSalesChartValues = listOf(0.0, 0.0, 0.0),
            totalOrdersChartValues = listOf(0.0, 0.0, 0.0),
            averageOrderValueChartValues = listOf(0.0, 0.0, 0.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun AiAssistantStatsCardPreviewUnifiedNegativeRefunds() {
    AiAssistantStatsCard(
        state = sampleUnifiedStatsCardState(
            totalSalesChartValues = listOf(10.0, -5.0, 3.0),
            netSalesChartValues = listOf(8.0, -7.0, 1.0),
            totalOrdersChartValues = listOf(4.0, 1.0, 5.0),
            averageOrderValueChartValues = listOf(80.0, -20.0, 70.0),
        ),
        onClick = {},
    )
}

private fun sampleUnifiedStatsCardState(
    totalSalesValue: String = "$170.35",
    netSalesValue: String = "$120.15",
    totalOrdersValue: String = "42",
    averageOrderValue: String = "$85.30",
    totalSalesChartValues: List<Double> = SAMPLE_TOTAL_SALES_CHART_VALUES,
    netSalesChartValues: List<Double> = SAMPLE_NET_SALES_CHART_VALUES,
    totalOrdersChartValues: List<Double> = SAMPLE_TOTAL_ORDERS_CHART_VALUES,
    averageOrderValueChartValues: List<Double> = SAMPLE_AVERAGE_ORDER_VALUE_CHART_VALUES,
) = AiAssistantStatsCardState(
    period = "May 1 - May 7, 2026",
    metrics = listOf(
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.TotalSales,
            value = totalSalesValue,
            chartValues = totalSalesChartValues,
        ),
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.NetSales,
            value = netSalesValue,
            chartValues = netSalesChartValues,
        ),
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.TotalOrders,
            value = totalOrdersValue,
            chartValues = totalOrdersChartValues,
        ),
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.AverageOrderValue,
            value = averageOrderValue,
            chartValues = averageOrderValueChartValues,
        ),
    ),
)

private fun sampleLegacyTwoMetricStatsCardState() = AiAssistantStatsCardState(
    period = "May 1 - May 7, 2026",
    metrics = sampleUnifiedStatsCardState().metrics.take(2),
)

private val SAMPLE_TOTAL_SALES_CHART_VALUES = listOf(12.0, 18.0, 9.0, 26.0, 21.0)
private val SAMPLE_NET_SALES_CHART_VALUES = listOf(8.0, 12.0, 6.0, 20.0, 14.0)
private val SAMPLE_TOTAL_ORDERS_CHART_VALUES = listOf(12.0, 16.0, 14.0, 18.0, 20.0)
private val SAMPLE_AVERAGE_ORDER_VALUE_CHART_VALUES = listOf(80.10, 82.25, 93.55, 89.75, 91.20)
private const val STATS_CARD_GRID_COLUMNS = 2
