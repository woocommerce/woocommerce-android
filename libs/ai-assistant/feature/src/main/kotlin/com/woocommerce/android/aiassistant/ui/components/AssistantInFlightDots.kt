package com.woocommerce.android.aiassistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Three small dots that pulse with a staggered fade. Used as the leading affordance in the typing
 * indicator and the tool activity pill so both in-flight surfaces share the same heartbeat.
 *
 * Mirrors the iOS reference: opacity 0.4 ↔ 1.0, 500ms easeInOut autoreverse, 150ms phase offset
 * between dots.
 */
@Composable
internal fun AssistantInFlightDots(
    color: Color,
    dotSize: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "assistant-in-flight-dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(IN_FLIGHT_DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = MIN_ALPHA,
                targetValue = MAX_ALPHA,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = HALF_CYCLE_MILLIS,
                        delayMillis = index * STAGGER_MILLIS,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "assistant-in-flight-dot-$index",
            )
            Box(
                modifier = Modifier
                    .alpha(alpha)
                    .size(dotSize)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

internal const val IN_FLIGHT_DOT_COUNT = 3
private const val MIN_ALPHA = 0.4f
private const val MAX_ALPHA = 1.0f
private const val HALF_CYCLE_MILLIS = 500
private const val STAGGER_MILLIS = 150

internal val InFlightDotsTypingSize: Dp = 6.dp
internal val InFlightDotsTypingSpacing: Dp = 4.dp
internal val InFlightDotsToolPillSize: Dp = 3.dp
internal val InFlightDotsToolPillSpacing: Dp = 2.dp
