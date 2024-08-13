package com.woocommerce.android.ui.woopos.home

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

private const val HOME_ROUTE = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HOME_ROUTE)
}

fun NavGraphBuilder.homeScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = HOME_ROUTE,
        enterTransition = {
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(
                    durationMillis = 800,
                    easing = { time ->
                        val accelerateDecelerate = AccelerateDecelerateInterpolator().getInterpolation(time)
                        @Suppress("MagicNumber")
                        OvershootInterpolator(1.5f).getInterpolation(accelerateDecelerate)
                    }
                )
            )
        }
    ) {
        WooPosHomeScreen(onNavigationEvent)
    }
}
