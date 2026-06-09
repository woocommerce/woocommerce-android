package com.woocommerce.android.ui.woopos.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartCheckoutButtonSlot
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreenProductsPreview
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsScreen
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.products.WooPosItemsScreenPreview
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreenPreview
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel

@Composable
fun WooPosHomeProductsPane(
    modifier: Modifier = Modifier,
    viewModel: WooPosItemsViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosItemsScreenPreview(modifier)
    } else {
        WooPosItemsScreen(modifier = modifier, viewModel = viewModel)
    }
}

@Composable
fun WooPosHomeCartPane(
    modifier: Modifier = Modifier,
    viewModel: WooPosCartViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosCartScreenProductsPreview(modifier)
    } else {
        WooPosCartScreen(
            modifier = modifier,
            viewModel = viewModel,
            checkoutSlot = WooPosCartCheckoutButtonSlot.Inline,
        )
    }
}

@Composable
fun WooPosHomeTotalsPane(
    modifier: Modifier = Modifier,
    viewModel: WooPosTotalsViewModel = hiltViewModel(),
) {
    if (isPreviewMode()) {
        WooPosTotalsScreenPreview(modifier)
    } else {
        WooPosTotalsScreen(
            modifier = modifier,
            viewModel = viewModel,
        )
    }
}
