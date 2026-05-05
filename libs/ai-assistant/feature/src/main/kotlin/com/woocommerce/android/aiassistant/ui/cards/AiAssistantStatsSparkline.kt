package com.woocommerce.android.aiassistant.ui.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
import androidx.compose.material3.Text

@Composable
internal fun AiAssistantStatsSparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
) {
    val normalizedPoints = normalizeStatsSparklinePoints(points)
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val fallbackColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.height(SPARKLINE_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        if (normalizedPoints.isEmpty()) {
            Text(
                text = stringResource(R.string.assistant_stats_card_trend_unavailable),
                modifier = Modifier.padding(horizontal = 8.dp),
                color = fallbackColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val midY = size.height / 2f
                drawLine(
                    color = guideColor,
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1.dp.toPx(),
                )

                val xStep = if (normalizedPoints.size == 1) {
                    0f
                } else {
                    size.width / (normalizedPoints.lastIndex)
                }
                val path = Path()
                normalizedPoints.forEachIndexed { index, normalized ->
                    val x = if (normalizedPoints.size == 1) size.width / 2f else index * xStep
                    val y = size.height - normalized * size.height
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                if (normalizedPoints.size == 1) {
                    val y = size.height - normalizedPoints.single() * size.height
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width * SINGLE_POINT_START_FRACTION, y),
                        end = Offset(size.width * SINGLE_POINT_END_FRACTION, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

internal fun normalizeStatsSparklinePoints(points: List<Double>): List<Float> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) return listOf(CENTERED_POINT)

    val min = points.minOrNull() ?: return emptyList()
    val max = points.maxOrNull() ?: return emptyList()
    if (min == max) return List(points.size) { CENTERED_POINT }

    val range = max - min
    return points.map { ((it - min) / range).toFloat() }
}

private val SPARKLINE_HEIGHT = 58.dp
private const val CENTERED_POINT = 0.5f
private const val SINGLE_POINT_START_FRACTION = 0.42f
private const val SINGLE_POINT_END_FRACTION = 0.58f
