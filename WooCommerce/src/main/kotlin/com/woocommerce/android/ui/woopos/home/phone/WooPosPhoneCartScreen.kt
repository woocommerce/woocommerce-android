package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartCheckoutButtonSlot
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreenProductsPreview
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel

@Composable
fun WooPosPhoneCartScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosCartViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosCartScreenProductsPreview(modifier.fillMaxSize())
        return
    }
    val state by viewModel.state.observeAsState()
    state?.let { rawState ->
        WooPosCartScreen(
            modifier = modifier.fillMaxSize(),
            state = rawState.copy(toolbar = rawState.toolbar.copy(backIconVisible = true)),
            onUIEvent = viewModel::onUIEvent,
            onPhoneBack = onBack,
            checkoutSlot = WooPosCartCheckoutButtonSlot.External,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosPhoneCartScreenPreview() {
    WooPosTheme {
        WooPosPhoneCartScreen(onBack = {})
    }
}
