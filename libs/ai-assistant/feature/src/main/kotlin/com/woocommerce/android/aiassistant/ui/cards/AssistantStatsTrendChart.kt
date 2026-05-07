package com.woocommerce.android.aiassistant.ui.cards

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun AssistantStatsTrendChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
) {
    val normalizedPoints = normalizeStatsTrendChartPoints(points)
    if (normalizedPoints.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.height(TREND_CHART_HEIGHT)) {
        val strokeWidthPx = TREND_LINE_STROKE.toPx()
        val verticalInset = strokeWidthPx + END_POINT_RADIUS.toPx()
        val drawableHeight = (size.height - verticalInset * 2f).coerceAtLeast(0f)

        val coords = if (normalizedPoints.size == 1) {
            val y = verticalInset + (1f - normalizedPoints.single()) * drawableHeight
            listOf(Offset(size.width / 2f, y))
        } else {
            val xStep = size.width / normalizedPoints.lastIndex
            normalizedPoints.mapIndexed { index, normalized ->
                val y = verticalInset + (1f - normalized) * drawableHeight
                Offset(index * xStep, y)
            }
        }

        if (coords.size >= 2) {
            val linePath = buildSmoothTrendPath(coords)
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = TREND_FILL_TOP_ALPHA),
                        lineColor.copy(alpha = 0f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        drawCircle(
            color = lineColor,
            radius = END_POINT_RADIUS.toPx(),
            center = coords.last(),
        )
    }
}

private fun buildSmoothTrendPath(coords: List<Offset>): Path = Path().apply {
    moveTo(coords.first().x, coords.first().y)
    for (index in 1 until coords.size) {
        val previous = coords[index - 1]
        val current = coords[index]
        val controlX = (previous.x + current.x) / 2f
        cubicTo(controlX, previous.y, controlX, current.y, current.x, current.y)
    }
}

internal fun normalizeStatsTrendChartPoints(points: List<Double>): List<Float> = when {
    points.isEmpty() -> emptyList()
    points.size == 1 -> listOf(CENTERED_POINT)
    else -> {
        val min = points.minOrNull()
        val max = points.maxOrNull()
        if (min == null || max == null || min == max) {
            List(points.size) { CENTERED_POINT }
        } else {
            val range = max - min
            points.map { ((it - min) / range).toFloat() }
        }
    }
}

@Preview(showBackground = true, widthDp = 240, heightDp = 440)
@Preview(name = "Dark", showBackground = true, widthDp = 240, heightDp = 440, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantStatsTrendChartPreview() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistantStatsTrendChartPreviewItem(points = listOf(12.0, 18.0, 9.0, 26.0, 21.0))
            AssistantStatsTrendChartPreviewItem(points = listOf(26.0, 21.0, 12.0, 4.0, 2.0))
            AssistantStatsTrendChartPreviewItem(points = listOf(4.0, 4.0, 4.0, 4.0))
            AssistantStatsTrendChartPreviewItem(points = listOf(10.0, -5.0, 3.0, -1.0))
            AssistantStatsTrendChartPreviewItem(points = listOf(12.0))
            AssistantStatsTrendChartPreviewItem(points = emptyList())
        }
    }
}

@Preview(showBackground = true, widthDp = 96, heightDp = 96)
@Composable
private fun AssistantStatsTrendChartCompactPreview() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.padding(16.dp)) {
            AssistantStatsTrendChart(
                points = listOf(2.0, 8.0, 3.0, 14.0),
                modifier = Modifier.width(64.dp),
            )
        }
    }
}

@Composable
private fun AssistantStatsTrendChartPreviewItem(points: List<Double>) {
    AssistantStatsTrendChart(
        points = points,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

private val TREND_CHART_HEIGHT = 58.dp
private val TREND_LINE_STROKE = 2.dp
private val END_POINT_RADIUS = 2.5.dp
private const val TREND_FILL_TOP_ALPHA = 0.18f
private const val CENTERED_POINT = 0.5f
