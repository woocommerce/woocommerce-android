package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Stable
internal class WooPosPINDotShake {
    private val progress = Animatable(0f)

    val offset: Dp get() = SHAKE_TRAVEL * sin(progress.value * PI.toFloat() * SHAKE_COUNT)

    suspend fun play() {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SHAKE_MILLIS, easing = LinearEasing))
    }
}

private const val SHAKE_MILLIS = 400
private const val SHAKE_COUNT = 3
private val SHAKE_TRAVEL = 8.dp
