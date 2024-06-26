package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.core.FastOutSlowInEasing
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
        color.copy(alpha = 0.8f),
        color.copy(alpha = 0.3f),
        color.copy(alpha = 0.8f)
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
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(0f, 0f),
        end = Offset(10f, translateAnim.value)
    )

    Box(
        modifier = modifier
            .background(brush = brush)
    )
}
