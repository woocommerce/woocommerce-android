package com.woocommerce.android.ui.woopos.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosSplashViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()

    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
    }

    when (state.value) {
        is WooPosSplashState.Loading -> Loading()
        is WooPosSplashState.Loaded -> onNavigationEvent(WooPosNavigationEvent.OpenHomeFromSplash)
    }
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosCircularLoadingIndicator(modifier = Modifier.size(156.dp))

        Spacer(modifier = Modifier.height(56.dp.toAdaptivePadding()))

        Text(
            text = stringResource(id = R.string.woopos_splash_title),
            style = MaterialTheme.typography.h5,
        )

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Text(
            text = stringResource(id = R.string.woopos_splash_message),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
@WooPosPreview
fun WooPosSplashScreenLoadingPreview() {
    WooPosTheme {
        Loading()
    }
}
