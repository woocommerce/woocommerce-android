package com.woocommerce.android.ui.aiassistant

import androidx.navigation.NavDirections
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.products.details.ProductDetailFragment

internal fun AssistantCardAction.toNavDirections(): NavDirections =
    when (this) {
        is AssistantCardAction.OpenOrder -> NavGraphMainDirections.actionGlobalOrderDetailFragment(
            orderId = remoteOrderId,
            ignoreTwoPaneLayoutLogic = true,
        )
        is AssistantCardAction.OpenProduct -> NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
        )
    }
