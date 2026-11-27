package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
    onBack: () -> Unit,
    isLeaving: Boolean,
    modifier: Modifier = Modifier,
    viewModel: WooPosTotalsViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosTotalsScreenPreview(modifier.fillMaxSize())
    } else {
        WooPosTotalsScreen(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            viewModel = viewModel,
            onPhoneBack = onBack,
            isLeaving = isLeaving,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosPhoneTotalsScreenPreview() {
    WooPosTheme {
        WooPosPhoneTotalsScreen(onBack = {}, isLeaving = false)
    }
}
