package com.woocommerce.android.ui.compose.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val NAV_TRANSITION_DURATION = 250

fun slideInNavTransition(forward: Boolean): EnterTransition {
    return slideInHorizontally(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION)) { fullWidth ->
        if (forward) fullWidth else -fullWidth
    } + fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION))
}

fun slideOutNavTransition(forward: Boolean): ExitTransition {
    return slideOutHorizontally(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION)) { fullWidth ->
        if (forward) -fullWidth else fullWidth
    } + fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_DURATION))
}
