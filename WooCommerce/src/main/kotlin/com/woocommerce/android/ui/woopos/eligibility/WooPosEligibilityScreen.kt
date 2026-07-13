package com.woocommerce.android.ui.woopos.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.wooPosFullScreenActionButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

@Composable
fun WooPosEligibilityScreen(
    initialReason: WooPosLaunchability.NonLaunchabilityReason,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosEligibilityViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.initialize(initialReason)
    }

    var isNavigatingAway by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToPos.collect {
            isNavigatingAway = true
            onNavigationEvent(WooPosNavigationEvent.OpenSplashScreen)
        }
    }

    if (isNavigatingAway) return

    val retryState = viewModel.retryState.collectAsState().value ?: return
    WooPosEligibilityScreen(
        onNavigationEvent = onNavigationEvent,
        retryState = retryState,
        onRetry = { viewModel.retryEligibilityCheckTapped() },
    )
}

@Composable
fun WooPosEligibilityScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    retryState: WooPosEligibilityRetryState,
    onRetry: () -> Unit,
) {
    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
    }

    val title = when (retryState) {
        is WooPosEligibilityRetryState.Ineligible -> retryState.title
        is WooPosEligibilityRetryState.Loading -> retryState.title
    }

    val suggestionText = when (retryState) {
        is WooPosEligibilityRetryState.Ineligible -> retryState.suggestionText
        is WooPosEligibilityRetryState.Loading -> retryState.suggestionText
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = WooPosIcons.ErrorX,
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosText(
                text = title,
                style = WooPosTypography.Heading,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = suggestionText,
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.adaptiveContentWidth()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WooPosSpacing.Large.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val buttonModifier = Modifier.wooPosFullScreenActionButton()

            when (retryState) {
                is WooPosEligibilityRetryState.RetryableIneligible -> {
                    WooPosButton(
                        text = stringResource(id = R.string.woopos_eligibility_retry_check_label),
                        onClick = onRetry,
                        state = WooPosButtonState.ENABLED,
                        modifier = buttonModifier,
                    )
                }

                is WooPosEligibilityRetryState.Loading -> {
                    WooPosButton(
                        text = stringResource(id = R.string.woopos_eligibility_retry_check_label),
                        onClick = onRetry,
                        state = WooPosButtonState.LOADING,
                        modifier = buttonModifier,
                    )
                }
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                text = stringResource(id = R.string.woopos_eligibility_exit_pos_label),
                modifier = buttonModifier,
            ) {
                onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosEligibilityScreenRetryablePreview() {
    WooPosTheme {
        WooPosEligibilityScreen(
            onNavigationEvent = {},
            retryState = WooPosEligibilityRetryState.RetryableIneligible(
                title = "Unable to load",
                suggestionText = "The POS system is not available for your store's currency. " +
                    "In United States, it currently supports only USD. " +
                    "Please check your store currency settings or contact support for assistance.",
            ),
            onRetry = {},
        )
    }
}
