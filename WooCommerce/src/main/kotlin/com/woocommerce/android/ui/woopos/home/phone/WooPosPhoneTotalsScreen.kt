package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreenPreview
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel

@Composable
fun WooPosPhoneTotalsScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosTotalsViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosTotalsScreenPreview(modifier.fillMaxSize())
    } else {
        WooPosTotalsScreen(
            modifier = modifier.fillMaxSize(),
            viewModel = viewModel,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosPhoneTotalsScreenPreview() {
    WooPosTheme {
        WooPosPhoneTotalsScreen()
    }
}
