package com.woocommerce.android.ui.woopos.splash

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSplashScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel: WooPosHomeViewModel = hiltViewModel()
}
