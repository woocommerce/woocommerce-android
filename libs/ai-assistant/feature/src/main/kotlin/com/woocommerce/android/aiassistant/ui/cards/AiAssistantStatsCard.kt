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
            StatsMetric(
                label = stringResource(R.string.assistant_stats_card_revenue_label),
                value = state.revenueTotal,
                modifier = Modifier.weight(1f),
            )
            StatsMetric(
                label = stringResource(R.string.assistant_stats_card_orders_label),
                value = state.orderCount,
                modifier = Modifier.weight(1f),
            )
        }
        StatsTrendRow(
            label = stringResource(R.string.assistant_stats_card_revenue_label),
            values = state.revenueChartValues,
            isTrendAvailable = state.isRevenueTrendAvailable,
            modifier = Modifier.fillMaxWidth(),
        )
        StatsTrendRow(
            label = stringResource(R.string.assistant_stats_card_orders_label),
            values = state.orderChartValues,
            isTrendAvailable = state.isOrdersTrendAvailable,
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
private fun StatsTrendRow(
    label: String,
    values: List<Double>,
    isTrendAvailable: Boolean,
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
            points = if (isTrendAvailable) values else emptyList(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 320, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AiAssistantStatsCardPreviewMultiPoint() {
    AiAssistantStatsCard(state = sampleStatsCardState(), onClick = {})
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewChangedShape() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = listOf(26.0, 9.0, 22.0, 7.0, 18.0),
            orderChartValues = listOf(5.0, 1.0, 4.0, 2.0, 3.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewSinglePoint() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = listOf(12.0),
            orderChartValues = listOf(1.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewAllZero() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = listOf(0.0, 0.0, 0.0),
            orderChartValues = listOf(0.0, 0.0, 0.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewNegativeRefunds() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = listOf(10.0, -5.0, 3.0),
            orderChartValues = listOf(2.0, 1.0, 2.0),
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewNoTrend() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = emptyList(),
            orderChartValues = emptyList(),
            isRevenueTrendAvailable = false,
            isOrdersTrendAvailable = false,
        ),
        onClick = {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun AiAssistantStatsCardPreviewPartialData() {
    AiAssistantStatsCard(
        state = sampleStatsCardState(
            revenueChartValues = listOf(12.0, 18.0, 9.0, 26.0, 21.0),
            orderChartValues = emptyList(),
            isOrdersTrendAvailable = false,
        ),
        onClick = {},
    )
}

private fun sampleStatsCardState(
    revenueChartValues: List<Double> = SAMPLE_REVENUE_CHART_VALUES,
    orderChartValues: List<Double> = SAMPLE_ORDER_CHART_VALUES,
    isRevenueTrendAvailable: Boolean = revenueChartValues.isNotEmpty(),
    isOrdersTrendAvailable: Boolean = orderChartValues.isNotEmpty(),
) = AiAssistantStatsCardState(
    period = "May 1 - May 7, 2026",
    revenueTotal = "$123.45",
    orderCount = "8",
    revenueChartValues = revenueChartValues,
    orderChartValues = orderChartValues,
    isRevenueTrendAvailable = isRevenueTrendAvailable,
    isOrdersTrendAvailable = isOrdersTrendAvailable,
)

private val SAMPLE_REVENUE_CHART_VALUES = listOf(12.0, 18.0, 9.0, 26.0, 21.0)
private val SAMPLE_ORDER_CHART_VALUES = listOf(1.0, 3.0, 2.0, 5.0, 4.0)
