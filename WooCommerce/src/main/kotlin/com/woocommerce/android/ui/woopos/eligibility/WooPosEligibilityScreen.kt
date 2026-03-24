package com.woocommerce.android.ui.woopos.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.util.ChromeCustomTabUtils

private const val CONTENT_WIDTH_FRACTION = 0.6f
private const val BUTTON_WIDTH_FRACTION = 0.5f

@Composable
fun WooPosEligibilityScreen(
    initialReason: WooPosLaunchability.NonLaunchabilityReason,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosEligibilityViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.initialize(initialReason)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val retryState = viewModel.retryState.collectAsState().value
    WooPosEligibilityScreen(
        onNavigationEvent = onNavigationEvent,
        retryState = retryState,
        onRetry = { viewModel.retryEligibilityCheckTapped() },
        onLearnMoreTapped = { viewModel.learnMoreTapped() },
    )
}

@Composable
fun WooPosEligibilityScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    retryState: WooPosEligibilityRetryState,
    onRetry: () -> Unit,
    onLearnMoreTapped: () -> Unit,
) {
    LaunchedEffect(retryState) {
        if (retryState is WooPosEligibilityRetryState.Eligible) {
            onNavigationEvent(WooPosNavigationEvent.OpenSplashScreen)
        }
    }

    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
    }

    val title = when (retryState) {
        is WooPosEligibilityRetryState.Ineligible -> retryState.title
        is WooPosEligibilityRetryState.Loading,
        is WooPosEligibilityRetryState.Eligible -> null
    }

    val suggestionText = when (retryState) {
        is WooPosEligibilityRetryState.Ineligible -> retryState.suggestionText
        is WooPosEligibilityRetryState.Loading -> retryState.suggestionText
        is WooPosEligibilityRetryState.Eligible -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            imageVector = WooPosIcons.ErrorX,
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        title?.let {
            WooPosText(
                text = it,
                style = WooPosTypography.Heading,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }

        suggestionText?.let { text ->
            WooPosText(
                text = text,
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(CONTENT_WIDTH_FRACTION)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        }

        val buttonModifier = Modifier
            .fillMaxWidth(BUTTON_WIDTH_FRACTION)

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

            is WooPosEligibilityRetryState.CiabPlanUpgradeRequired -> {
                val context = LocalContext.current
                WooPosButton(
                    text = stringResource(id = R.string.woopos_eligibility_learn_more_label),
                    onClick = {
                        onLearnMoreTapped()
                        ChromeCustomTabUtils.launchUrl(context, retryState.learnMoreUrl.toString())
                    },
                    state = WooPosButtonState.ENABLED,
                    modifier = buttonModifier,
                )
            }

            is WooPosEligibilityRetryState.Eligible -> { /* navigating away */ }
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
            onLearnMoreTapped = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosEligibilityCiabPreview() {
    WooPosTheme {
        WooPosEligibilityScreen(
            onNavigationEvent = {},
            retryState = WooPosEligibilityRetryState.CiabPlanUpgradeRequired(
                title = "Pro plan required",
                suggestionText = "Accept payments in person for just 2.70% + \$0.10 per transaction. " +
                    "Upgrade to Pro to access tap-to-pay on your phone and our full " +
                    "point of sale system with real-time inventory and order syncing.",
                learnMoreUrl = "https://wordpress.com/setup/woo-hosted-plans/".toUri(),
            ),
            onRetry = {},
            onLearnMoreTapped = {},
        )
    }
}
