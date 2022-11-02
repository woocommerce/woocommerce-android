package com.woocommerce.android.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    info: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    strokeWidth: Dp = 2.dp
) {
    when (info.size) {
        // We only draw the chart when info size is greater than 1
        0, 1 -> {}
        else -> {
            MultipleValuesLineChart(
                info = info,
                color = color,
                strokeWidth = strokeWidth,
                modifier = modifier
            )
        }
    }
}

@Composable
internal fun MultipleValuesLineChart(
    info: List<Float>,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val transparentGraphColor = remember {
        color.copy(alpha = 0.5f)
    }
    val higherValue = remember(info) {
        (info.maxOfOrNull { it }?.plus(1)) ?: 0f
    }
    val lowerValue = remember(info) {
        info.minOfOrNull { it } ?: 0f
    }
    val ratio = remember(info) { higherValue - lowerValue }

    val animation = remember(info) { Animatable(0f) }

    LaunchedEffect(info) {
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 100,
                easing = LinearOutSlowInEasing
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val padding = size.width * 0.1f
        val spaceBetweenValues = (size.width - padding) / info.size

        val strokePath = Path().apply {
            val height = size.height
            for (i in info.indices) {
                val currentValue = info[i]
                val yRatio = (currentValue - lowerValue) / ratio

                val x = padding + i * spaceBetweenValues
                val chartTopDrawableArea = height - padding
                val y = chartTopDrawableArea - (yRatio * chartTopDrawableArea) * animation.value

                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        val lastX = padding + info.size * spaceBetweenValues

        val fillPath = Path()
            .apply {
                addPath(strokePath)
                lineTo(lastX, size.height - padding)
                lineTo(padding, size.height - padding)
                close()
            }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    transparentGraphColor,
                    Color.Transparent
                ),
                endY = size.height - padding
            )
        )

        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewSquare() {
    LineChart(info = listOf(10f, 5.5f, 12f, 12f, 8f, 18f))
}

@Preview(widthDp = 500, heightDp = 300)
@Composable
internal fun LineChartPreviewRectangle() {
    LineChart(info = listOf(10f, 5.5f, 12f, 12f, 8f, 18f))
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewEmpty() {
    LineChart(info = emptyList())
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewOneValue() {
    LineChart(info = listOf(10f))
}
