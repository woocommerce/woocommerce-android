package com.woocommerce.android.ui.woopos.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosSplashViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()

    BackHandler {
        viewModel.onExitPosClicked()
        onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
    }

    when (val currentState = state.value) {
        is WooPosSplashState.Loading -> {
            Loading()
        }
        is WooPosSplashState.Syncing -> {
            SyncingCatalog(
                onExitPosClicked = {
                    viewModel.onExitPosClicked()
                    onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
                }
            )
        }
        is WooPosSplashState.SyncFailed -> {
            SyncFailed(
                onRetryClicked = { viewModel.onRetrySync() },
                onExitPosClicked = {
                    onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
                }
            )
        }
        is WooPosSplashState.Loaded -> {
            onNavigationEvent(WooPosNavigationEvent.OpenHomeFromSplash)
        }
        is WooPosSplashState.NotEligible -> {
            val reason = currentState.reason
            onNavigationEvent(WooPosNavigationEvent.OpenEligibilityScreenFromSplash(reason))
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WooPosCircularLoadingIndicator(modifier = Modifier.size(WooPosComponentSize.XLarge.value))
    }
}

@Suppress("WooPosDesignSystemTextUsageRule")
@Composable
private fun SyncingCatalog(
    onExitPosClicked: () -> Unit
) {
    val loadingIndicatorSize = WooPosComponentSize.XLarge.value
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        WooPosCircularLoadingIndicator(
            modifier = Modifier
                .size(loadingIndicatorSize)
                .align(Alignment.Center)
        )

        Text(
            text = stringResource(R.string.woopos_home_syncing_catalog_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (loadingIndicatorSize / 2 + WooPosSpacing.XLarge.value))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WooPosSpacing.XLarge.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onExitPosClicked
            ) {
                Text(
                    text = stringResource(R.string.woopos_home_syncing_catalog_exit_button),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value),
            ) {
                Text(
                    text = stringResource(R.string.woopos_home_syncing_catalog_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = WooPosTheme.colors.onSurfaceVariantLowest,
                )
                Text(
                    text = stringResource(R.string.woopos_home_syncing_catalog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = WooPosTheme.colors.onSurfaceVariantLowest,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value),
                )
            }
        }
    }
}

@Composable
private fun SyncFailed(
    onRetryClicked: () -> Unit,
    onExitPosClicked: () -> Unit
) {
    WooPosErrorScreen(
        message = stringResource(R.string.woopos_home_sync_failed_title),
        reason = stringResource(R.string.woopos_home_sync_failed_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_home_sync_failed_retry_button),
            click = onRetryClicked
        ),
        secondaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_home_syncing_catalog_exit_button),
            click = onExitPosClicked
        )
    )
}

@Composable
@WooPosPreview
fun WooPosSplashScreenLoadingPreview() {
    WooPosTheme {
        Loading()
    }
}

@Composable
@WooPosPreview
fun WooPosSplashScreenSyncingPreview() {
    WooPosTheme {
        SyncingCatalog(
            onExitPosClicked = {}
        )
    }
}
