package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosShimmerBox(
    modifier: Modifier = Modifier,
    color: Color = WooPosTheme.colors.loadingSkeleton,
) {
    val shimmerColors = listOf(
        color.copy(),
        color.copy(alpha = 0.7f),
        color.copy(alpha = 0.6f),
        color.copy(alpha = 0.7f),
        color.copy()
    )

    val transition = rememberInfiniteTransition(
        label = "shimmer_transition"
    )
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(0f, 0f),
        end = Offset(translateAnim.value, 0f)
    )

    Box(
        modifier = modifier
            .background(brush = brush)
    )
}
