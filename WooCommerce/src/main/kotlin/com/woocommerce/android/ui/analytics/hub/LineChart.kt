package com.woocommerce.android.ui.analytics.hub

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
        // We only draw the chart when info size is greater than 0
        0 -> {}
        1 -> {
            SingleValueLineChart(
                value = info[0],
                color = color,
                strokeWidth = strokeWidth,
                modifier = modifier
            )
        }
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
internal fun SingleValueLineChart(
    value: Float,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val transparentGraphColor = remember(color) {
        color.copy(alpha = 0.5f)
    }

    val higherValue = remember(value) { value + 1f }
    val lowerValue = remember(value) { value - .15f }

    val ratio = remember(value) { higherValue - lowerValue }
    val animation = remember(value) { Animatable(0f) }

    LaunchedEffect(value) {
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
        // Default padding percent 10%
        val defaultPaddingPercent = 0.1f

        val padding = size.width * defaultPaddingPercent
        val yRatio = (value - lowerValue) / ratio
        val lastX = size.width - padding
        val chartTopDrawableArea = size.height - padding
        val y = chartTopDrawableArea - (yRatio * chartTopDrawableArea) * animation.value

        val strokePath = Path().apply {
            moveTo(padding, y)
            lineTo(lastX, y)
        }

        drawLineChart(
            linePath = strokePath,
            color = color,
            transparentColor = transparentGraphColor,
            strokeWidth = strokeWidth,
            size = size,
            padding = padding,
            lastX = lastX
        )
    }
}

@Composable
internal fun MultipleValuesLineChart(
    info: List<Float>,
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val transparentGraphColor = remember(color) {
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
        // Default padding percent 10%
        val defaultPaddingPercent = 0.1f

        val padding = size.width * defaultPaddingPercent
        val spaceBetweenValues = (size.width - padding) / info.size

        var lastX = 0f

        val strokePath = Path().apply {
            val height = size.height
            for (i in info.indices) {
                val currentValue = info[i]
                val yRatio = (currentValue - lowerValue) / ratio

                val x = padding + i * spaceBetweenValues
                val chartTopDrawableArea = height - padding
                val y = chartTopDrawableArea - (yRatio * chartTopDrawableArea) * animation.value

                if (i == 0) moveTo(x, y) else lineTo(x, y)
                lastX = x
            }
        }

        drawLineChart(
            linePath = strokePath,
            color = color,
            transparentColor = transparentGraphColor,
            strokeWidth = strokeWidth,
            size = size,
            padding = padding,
            lastX = lastX
        )
    }
}

@Suppress("LongParameterList")
private fun DrawScope.drawLineChart(
    linePath: Path,
    color: Color,
    transparentColor: Color,
    strokeWidth: Dp,
    size: Size,
    padding: Float,
    lastX: Float
) {
    val fillPath = Path()
        .apply {
            addPath(linePath)
            lineTo(lastX, size.height - padding)
            lineTo(padding, size.height - padding)
            close()
        }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                transparentColor,
                Color.Transparent
            ),
            endY = size.height - padding
        )
    )

    drawPath(
        path = linePath,
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )
    )
}

@Suppress("MagicNumber")
private val infoMultipleValues = listOf(10f, 5.5f, 12f, -12f, 8f, 18f)

@Suppress("MagicNumber")
private val infoSingleValue = listOf(-10f)

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewSquare() {
    LineChart(info = infoMultipleValues)
}

@Preview(widthDp = 500, heightDp = 300)
@Composable
internal fun LineChartPreviewRectangle() {
    LineChart(info = infoMultipleValues)
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewEmpty() {
    LineChart(info = emptyList())
}

@Preview(widthDp = 300, heightDp = 300)
@Composable
internal fun LineChartPreviewOneValue() {
    LineChart(info = infoSingleValue)
}
