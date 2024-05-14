package com.woocommerce.android.ui.woopos.root.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenSlideIn(): EnterTransition =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)

fun screenFadeOut(): ExitTransition = fadeOut()

fun screenFadeIn(): EnterTransition = fadeIn()

fun AnimatedContentTransitionScope<NavBackStackEntry>.screenSlideOut(): ExitTransition =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
