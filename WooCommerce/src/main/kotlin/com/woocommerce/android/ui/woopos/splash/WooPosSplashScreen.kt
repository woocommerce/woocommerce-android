package com.woocommerce.android.ui.woopos.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosHomeViewModel = hiltViewModel()
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
    Text(
        text = "Loading...",
        style = MaterialTheme.typography.h1,
        modifier = Modifier.fillMaxSize(),
        textAlign = TextAlign.Center
    )
}
