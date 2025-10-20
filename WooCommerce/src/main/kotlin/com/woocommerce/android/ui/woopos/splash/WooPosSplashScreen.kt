package com.woocommerce.android.ui.woopos.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosSplashViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()

    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
    }

    when (val currentState = state.value) {
        is WooPosSplashState.Loading -> {
            Loading()
        }
        is WooPosSplashState.Syncing -> {
            SyncingCatalog()
        }
        is WooPosSplashState.SyncFailed -> {
            SyncFailed(
                onRetryClicked = { viewModel.onRetrySync() }
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
        WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
    }
}

@Suppress("WooPosDesignSystemSpacingUsageRule", "WooPosDesignSystemTextUsageRule")
@Composable
private fun SyncingCatalog() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.woopos_home_syncing_catalog_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SyncFailed(onRetryClicked: () -> Unit) {
    WooPosErrorScreen(
        message = stringResource(R.string.woopos_home_sync_failed_title),
        reason = stringResource(R.string.woopos_home_sync_failed_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_home_sync_failed_retry_button),
            click = onRetryClicked
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
        SyncingCatalog()
    }
}
