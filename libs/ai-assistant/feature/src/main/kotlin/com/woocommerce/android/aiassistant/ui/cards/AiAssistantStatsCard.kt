package com.woocommerce.android.aiassistant.ui.cards

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val metricsWithTrends = state.metrics.filter { shouldShowStatsTrendChart(it.chartValues) }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.metrics.forEach { metric ->
                StatsMetric(
                    label = stringResource(metric.type.labelRes()),
                    value = metric.value,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (metricsWithTrends.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                metricsWithTrends.forEach { metric ->
                    StatsTrendColumn(
                        label = stringResource(metric.type.labelRes()),
                        values = metric.chartValues,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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

@Composable
private fun StatsMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatsTrendColumn(
    label: String,
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
        AssistantStatsTrendChart(
            points = values,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun shouldShowStatsTrendChart(values: List<Double>): Boolean = values.size > 1

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AiAssistantStatsCardPreviewMultiPoint() {
    AiAssistantStatsCard(state = sampleStatsCardState(), onClick = {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewChangedShape() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = listOf(26.0, 9.0, 22.0, 7.0, 18.0),
            netSalesChartValues = listOf(15.0, 4.0, 18.0, 3.0, 12.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewSinglePoint() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = listOf(12.0),
            netSalesChartValues = listOf(9.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewAllZero() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = listOf(0.0, 0.0, 0.0),
            netSalesChartValues = listOf(0.0, 0.0, 0.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewNegativeRefunds() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = listOf(10.0, -5.0, 3.0),
            netSalesChartValues = listOf(8.0, -7.0, 1.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewNoTrend() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = emptyList(),
            netSalesChartValues = emptyList(),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewOrders() {
    AiAssistantStatsCard(state = sampleOrdersStatsCardState(), onClick = {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun AiAssistantStatsCardPreviewPartialData() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            totalSalesChartValues = listOf(12.0, 18.0, 9.0, 26.0, 21.0),
            netSalesChartValues = emptyList(),
        ),
        onClick = {},
    )
}

private fun sampleStatsCardState(
    totalSalesChartValues: List<Double> = SAMPLE_TOTAL_SALES_CHART_VALUES,
    netSalesChartValues: List<Double> = SAMPLE_NET_SALES_CHART_VALUES,
) = AiAssistantStatsCardState(
    period = "May 1 - May 7, 2026",
    metrics = listOf(
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.TotalSales,
            value = "$170.35",
            chartValues = totalSalesChartValues,
        ),
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.NetSales,
            value = "$120.15",
            chartValues = netSalesChartValues,
        ),
    ),
)

private fun sampleOrdersStatsCardState() = AiAssistantStatsCardState(
    period = "May 1 - May 7, 2026",
    metrics = listOf(
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.TotalOrders,
            value = "42",
            chartValues = listOf(12.0, 16.0, 14.0),
        ),
        AiAssistantStatsCardState.Metric(
            type = AssistantCard.Stats.MetricType.AverageOrderValue,
            value = "$85.30",
            chartValues = listOf(80.10, 82.25, 93.55),
        ),
    ),
)

private val SAMPLE_TOTAL_SALES_CHART_VALUES = listOf(12.0, 18.0, 9.0, 26.0, 21.0)
private val SAMPLE_NET_SALES_CHART_VALUES = listOf(8.0, 12.0, 6.0, 20.0, 14.0)
