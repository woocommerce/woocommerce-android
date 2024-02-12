package com.woocommerce.android.ui.shipping

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState
import kotlinx.coroutines.delay

@Composable
fun InstallWCShippingScreen(viewModel: InstallWCShippingViewModel) {
    val installWcShippingFlowState by viewModel.viewState.observeAsState()
    installWcShippingFlowState?.let {
        InstallWCShippingScreen(it)
    }
}

@OptIn(ExperimentalTransitionApi::class, ExperimentalAnimationApi::class)
@Composable
fun InstallWCShippingScreen(viewState: ViewState) {
    val transition = updateTransition(viewState, label = "MainTransition")
    Box(modifier = Modifier.background(color = MaterialTheme.colors.surface)) {
        transition.AnimatedContent(
            transitionSpec = {
                if (initialState is ViewState.Onboarding && targetState is InstallationState) {
                    // Apply a fade-in/fade-out globally,
                    // then each child will animate the individual components separately
                    fadeIn(tween(500, delayMillis = 500))
                        .togetherWith(fadeOut(tween(500, easing = LinearOutSlowInEasing)))
                } else {
                    // No-op animation, each screen will define animations for specific components separately
                    EnterTransition.None.togetherWith(ExitTransition.None)
                }
            }
        ) { targetState ->
            when (targetState) {
                is ViewState.Onboarding -> InstallWcShippingOnboarding(viewState = targetState)
                is InstallationState -> InstallWCShippingFlow(viewState = targetState)
            }
        }
    }
}

@SuppressLint("RememberReturnType")
@Preview
@Composable
private fun PreviewInstallWCShippingScreen() {
    val states = remember {
        mutableListOf<ViewState>()
    }

    var state by remember {
        mutableStateOf(states.getOrNull(0))
    }

    remember {
        states.add(
            ViewState.Onboarding(
                title = R.string.install_wc_shipping_flow_onboarding_screen_title,
                subtitle = R.string.install_wc_shipping_flow_onboarding_screen_subtitle,
                bullets = emptyList(),
                onInstallClicked = { state = states[1] }
            )
        )

        states.add(
            InstallationState.PreInstallation(
                extensionsName = R.string.install_wc_shipping_extension_name,
                siteName = "Site",
                siteUrl = "URL",
                onCancelClick = {},
                onProceedClick = { state = states[2] },
                onInfoClick = {}
            )
        )

        states.add(
            InstallationState.InstallationOngoing(
                extensionsName = R.string.install_wc_shipping_extension_name,
                siteName = "Site",
                siteUrl = "URL"
            )
        )

        state = states[0]
    }

    LaunchedEffect(state) {
        if (state is InstallationState.InstallationOngoing) {
            delay(5000)
            state = states[0]
        }
    }

    WooThemeWithBackground {
        state?.let {
            InstallWCShippingScreen(viewState = it)
        }
    }
}
