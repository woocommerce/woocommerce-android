package com.woocommerce.android.ui.woopos.eligibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
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

    val retryState = viewModel.retryState.collectAsState().value
    WooPosEligibilityScreen(
        onNavigationEvent = onNavigationEvent,
        retryState = retryState,
        onRetry = { viewModel.retryEligibilityCheckTapped() }
    )
}

@Composable
fun WooPosEligibilityScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    retryState: WooPosEligibilityRetryState,
    onRetry: () -> Unit,
) {
    LaunchedEffect(retryState) {
        if (retryState is WooPosEligibilityRetryState.Eligible) {
            onNavigationEvent(WooPosNavigationEvent.OpenHomeFromSplash)
        }
    }

    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_woo_pos_error_x),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(R.string.woopos_eligibility_screen_unable_to_load),
            style = WooPosTypography.Heading,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        val suggestionText = when (retryState) {
            is WooPosEligibilityRetryState.Ineligible -> retryState.suggestionText
            is WooPosEligibilityRetryState.Loading -> retryState.suggestionText
            is WooPosEligibilityRetryState.Eligible -> null
        }

        suggestionText?.let { text ->
            WooPosText(
                text = text,
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(547.dp)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        }

        WooPosButton(
            text = stringResource(id = R.string.woopos_eligibility_retry_check_label),
            onClick = onRetry,
            state = if (retryState is WooPosEligibilityRetryState.Loading) {
                WooPosButtonState.LOADING
            } else {
                WooPosButtonState.ENABLED
            },
            modifier = Modifier.size(width = 366.dp, height = 80.dp)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        WooPosOutlinedButton(
            text = stringResource(id = R.string.woopos_eligibility_exit_pos_label),
            modifier = Modifier.size(width = 366.dp, height = 80.dp)
        ) {
            onNavigationEvent(WooPosNavigationEvent.ExitPosClicked)
        }
    }
}

@WooPosPreview
@Composable
fun WooPosEligibilityScreenPreview() {
    WooPosTheme {
        WooPosEligibilityScreen(
            onNavigationEvent = {},
            retryState = WooPosEligibilityRetryState.Ineligible(
                "The POS system is not available for your store's currency. " +
                    "In United States, it currently supports only USD. " +
                    "Please check your store currency settings or contact support for assistance."
            ),
            onRetry = {}
        )
    }
}
