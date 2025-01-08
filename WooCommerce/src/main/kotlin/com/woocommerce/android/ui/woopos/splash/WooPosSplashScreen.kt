package com.woocommerce.android.ui.woopos.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosSplashViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()

    BackHandler {
        onNavigationEvent(WooPosNavigationEvent.BackFromSplashClicked)
    }

    Loading()

    when (state.value) {
        is WooPosSplashState.Loading -> {}
        is WooPosSplashState.Loaded -> {
            onNavigationEvent(WooPosNavigationEvent.OpenHomeFromSplash)
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WooPosCircularLoadingIndicator(modifier = Modifier.size(156.dp))
    }
}

@Composable
@WooPosPreview
fun WooPosSplashScreenLoadingPreview() {
    WooPosTheme {
        Loading()
    }
}
